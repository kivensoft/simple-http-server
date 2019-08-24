package cn.kivensoft.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import cn.kivensoft.httpserver.Headers;
import cn.kivensoft.httpserver.HttpExchange;
import cn.kivensoft.httpserver.HttpHandler;
import cn.kivensoft.httpserver.HttpServer;
import cn.kivensoft.util.Fmt;
import cn.kivensoft.util.Langs;
import cn.kivensoft.util.MyLogger;
import cn.kivensoft.util.ObjectPool;
import cn.kivensoft.util.PoolItem;
import cn.kivensoft.util.ScanPackage;
import cn.kivensoft.util.Strings;

public class SimpleHttpServer implements HttpHandler {
	private static final String UTF8 = "UTF-8";
	private static final String DEFAULT_SERVER_NAME = "SimpleHttpServer";
	private static final String HTTP_VERSION = "1.0";

	private MyLogger logger = MyLogger.get(getClass());
	private HttpServer httpServer;
	private BiFunction<String, String, Object> preHandle;
	private Map<String, MethodInfo> handles = new HashMap<>();
	private String serverName;

	public void start(int port, ExecutorService executorService) throws Exception {
		start(null, port, executorService);
	}

	public void start(String serverName, int port, ExecutorService executorService) throws Exception {
		if (serverName == null || serverName.isEmpty())
			serverName = DEFAULT_SERVER_NAME;
		this.serverName = serverName + "/" + HTTP_VERSION;

		httpServer = HttpServer.create(new InetSocketAddress(port), 0);
		httpServer.createContext("/", this);
		if (executorService !=  null) httpServer.setExecutor(executorService);
		httpServer.start();
	}

	public void stop() {
		httpServer.stop(0);
	}

	public void setAuthHandle(BiFunction<String, String, Object> preHandle) {
		this.preHandle = preHandle;
	}

	/** 映射类的公共静态函数到api地址
	 * @param prefix 地址前缀
	 * @param cls 要映射的类
	 */
	public void mapController(String prefix, Class<?> cls) {
		StringBuilder sb = new StringBuilder();
		pathAppend(sb, prefix);
		mapClass(cls, sb);
	}

	/** 搜索指定包下面的类并进行映射，只有带注解RequestMapping的类及函数才进行映射
	 * @param prefix 地址前缀
	 * @param packageName 要搜索的包名称
	 * @param recursive 是否递归搜索
	 */
	public void scanPackage(String prefix, String packageName, boolean recursive) {
		// 地址前缀 + 每个映射函数的小写名称 = 实际的api地址
		StringBuilder sb = new StringBuilder();
		pathAppend(sb, prefix);
		int prefix_len = sb.length();

		List<Class<?>> clss = ScanPackage.getClasses(packageName, true, null);
		for (Class<?> cls : clss) {
			mapClass(cls, sb);
			sb.setLength(prefix_len);
		}
	}

	/** HTTP请求处理函数，由系统回调，应用程序不可调用 */
	@Override
	public void handle(HttpExchange he) throws IOException {
		logRequestInfo(he);

		// 解析请求路径及请求参数，找到系统对应的处理函数进行调用处理，处理调用结果
		String path = he.getRequestURI().getPath().toLowerCase();
		if (path.length() > 1 && path.charAt(path.length() - 1) == '/')
			path = path.substring(0, path.length() - 1);

		Object ret = null;
		int httpCode = 200; // http成功代码
		try {
			// 查找路径对应的处理函数并解析参数，然后进行调用
			MethodInfo act = handles.get(path);
			if (act != null && (act.actionType.isEmpty()
					|| act.actionType.equalsIgnoreCase(he.getRequestMethod()))) {
				Map<String, List<String>> query = parseQuery(he.getRequestURI().getRawQuery());
				if (preHandle != null) {
					Object authRet = preHandle.apply(path, parseJwtToken(he, query));
					if (authRet != null && authRet.getClass() == Boolean.class) {
						if (((Boolean)authRet).booleanValue()) {
							ret = "未授权的访问";
							httpCode = 403;
						} else {
							ret = "用户尚未登陆";
							httpCode = 401;
						}
					} else {
						he.setAttribute("auth", authRet);
						ret = invokeMethodInfo(he, query, act);
					}
				}
			} else {
				ret = "请求的地址不存在";
				httpCode = 404; // http找不到页面错误
			}
		} catch (Exception e) {
			logger.error(e, "process {} has error: {}", path, e.getMessage());
			ret = "系统内部错误";
			httpCode = 500; // http 内部错误
		}

		// 写入返回结果
		processResult(he, path, httpCode, ret);
	}

	/** 获取已经生效的映射url */
	final public Map<String, String> getAllMappingPath() {
		Map<String, String> ret = new LinkedHashMap<>();
		for (Map.Entry<String, MethodInfo> item : handles.entrySet())
			ret.put(item.getKey(), item.getValue().desc);
		return ret;
	}

	final public MethodInfo findMethodInfo(String path) {
		return handles.get(path);
	}

	/** 映射类cls中的函数到url */
	final private void mapClass(Class<?> cls, StringBuilder sb) {
		// 如果类没有RequestMapping注解，则不是控制器
		RequestMapping mapping = cls.getAnnotation(RequestMapping.class);
		if (mapping == null) return;
		pathAppend(sb, mapping.value());

		Object ctrl = null;
		try {
			ctrl = cls.newInstance();
		} catch (Exception e) {
			logger.error(e, "create {} class error: {}", cls.getName(), e.getMessage());
			return;
		}

		int prefixLen = sb.length();
		Method[] ms = cls.getMethods();
		for (int i = 0, n = ms.length; i < n; ++i) {
			Method method = ms[i];
			// 函数必须具备RequestMapping注解, 具备0个或1个或2个参数
			RequestMapping reqMapping = method.getAnnotation(RequestMapping.class);
			if (reqMapping == null) continue;
			Class<?>[] paramTypes = method.getParameterTypes();
			int ptLen = paramTypes.length;
			if (ptLen > 2) continue; // 大于2个参数的不支持
			Class<?> paramType1 = null, paramType2 = null;
			int authParamIndex = -1;
			if (ptLen > 0) {
				// 2个参数的函数, 其中一个参数必须是AuthInfo注解
				authParamIndex = findAuthInfoAnnotation(method.getParameterAnnotations());
				if (authParamIndex == -1 && ptLen == 2 &&
						paramTypes[0] != HttpRaw.class && paramTypes[1] != HttpRaw.class)
					continue;
				paramType1 = paramTypes[0];
				if (ptLen > 1) paramType2 = paramTypes[1];
			}
			pathAppend(sb, reqMapping.value());
			String uri = sb.toString().toLowerCase();
			method.setAccessible(true);
			handles.put(uri, new MethodInfo(ctrl, method, reqMapping.method(),
					reqMapping.desc(), authParamIndex, paramType1, paramType2));
			logMappingInfo(uri, cls, method, paramType1, paramType2);
			sb.setLength(prefixLen);
		}
	}

	/** url路径增加子路径，必须开头有斜杠和结尾不能有斜杠 */
	final static public void pathAppend(StringBuilder sb, String path) {
		if (path == null || path.isEmpty()) return;
		if (path.charAt(0) != '/') sb.append('/');
		sb.append(path);
		if (sb.charAt(sb.length() - 1) == '/') sb.setLength(sb.length() - 1);
	}

	/** 根据MethodInfo的参数个数进行相应的函数处理 */
	final private Object invokeMethodInfo(HttpExchange he,
			Map<String, List<String>> query, MethodInfo act) throws Exception {
		// 0个参数
		if (act.argType1 == null) return act.method.invoke(act.obj);

		Object attr = he.getAttribute("auth");
		Object arg1 = null, arg2 = null;
		if (act.argType1 == HttpRaw.class) arg1 = new HttpRawImpl(he);
		else if (act.authParamIndex == 0) arg1 = attr;
		if (act.argType2 == HttpRaw.class) arg2 = new HttpRawImpl(he);
		else if (act.authParamIndex == 1) arg2 = attr;

		// 1个参数需要解析或2个参数有1个需要解析时, 解析参数
		if (arg1 == null && act.argType2 == null
				|| ((arg1 == null || arg2 == null) && act.argType2 != null)) {
			Object arg;
			Class<?> argType = arg1 == null ? act.argType1 : act.argType2;;
			// 读取并设置post参数
			if ("post".equalsIgnoreCase(he.getRequestMethod())) {
				arg = parseBody(he, argType);
			} else {
				arg = argType.newInstance();
			}

			// 读取并设置url中的参数
			if (query != null && query.size() > 0) {
				Object def_arg = argType.newInstance();
				for (Map.Entry<String, List<String>> item : query.entrySet())
					setObjectProperty(argType, arg, def_arg, item);
			}

			if (arg1 == null) arg1 = arg;
			else arg2 = arg;
			logger.debugJson("params: {}", arg);
		}

		// 1个参数, 且参数是预定义的, 无需解析
		if (act.argType2 == null) return act.method.invoke(act.obj, arg1);
		// 2个参数
		else return act.method.invoke(act.obj, arg1, arg2);
	}

	final private int findAuthInfoAnnotation(Annotation[][] ass) {
		for (int i = 0, imax = ass.length; i < imax; ++i) {
			Annotation[] as = ass[i];
			for (Annotation a : as) {
				if (a.annotationType() == AuthInfo.class) return i;
			}
		}
		return -1;
	}

	/** 解析POST请求的body内容的json格式参数,返回类型为参数cls类型 */
	final private Object parseBody(HttpExchange he, Class<?> cls) {
		try {
			String body = readStringFromInputStream(he.getRequestBody());
			logger.debug("request body: {}", body);
			Object ret = body == null || body.isEmpty()
					? cls.newInstance() : JSON.parseObject(body, cls);
			if (logger.isDebugEnabled())
				logger.debug("parseBody result: {}", Json.toJson(ret));
			return ret;
		} catch (Exception e) {
			logger.error(e, "parseBody error: {}", e.getMessage());
			return null;
		}
	}

	/** 解析url地址带的参数成hashmap类型返回值 */
	final private HashMap<String, List<String>> parseQuery(String query) {
		HashMap<String, List<String>> ret = new HashMap<>();
		if (query == null || query.isEmpty()) return ret;

		int start, idx = -1;
		do {
			// 获取key和value
			start = idx + 1;
			idx = query.indexOf('&', start);
			int pos = query.indexOf('=', start);
			if (pos <= start || pos >= idx && idx > 0) {
				logger.debug("parseQuery warning, query string can't parse");
				continue;
			}
			String key = null;
			String value = null;
			try {
				key = URLDecoder.decode(query.substring(start, pos), UTF8);
				value = URLDecoder.decode(query.substring(
						pos + 1, idx > 0 ? idx : query.length()), UTF8);
			} catch (UnsupportedEncodingException e) {
				continue;
			}

			// 写入键值到字典表中
			List<String> vals = ret.get(key);
			if (vals == null) {
				vals = new ArrayList<>();
				ret.put(key, vals);
			}
			vals.add(value);
		} while (idx != -1);

		return ret;
	}

	final private String parseJwtToken(HttpExchange he, Map<String, List<String>> query) {
		List<String> auths = he.getRequestHeaders().get("Authorization");
		if (auths != null && !auths.isEmpty()) {
			String auth = auths.get(0);
			if (auth.startsWith("Bearer ")) return auth.substring(7);
		}

		auths = he.getRequestHeaders().get("Cookie");
		if (auths != null && !auths.isEmpty()) {
			auths = Strings.split(auths.get(0), ';');
			for (String s : auths) {
				List<String> ss = Strings.split(s, '=');
				if (ss != null && ss.size() == 2) {
					if (ss.get(0).trim().equals("jwtToken"))
						return ss.get(1);
				}
			}
		}

		auths = query.get("jwtToken");
		if (auths != null && !auths.isEmpty()) return auths.get(0);

		return null;
	}

	/** 记录映射api的条目 */
	private void logMappingInfo(String uri, Class<?> cls, Method method,
			Class<?> argType1, Class<?> argType2) {
		if (!logger.isDebugEnabled()) return;
		if (argType1 != null && argType2 != null) {
			logger.info("Mapping api url: {}  ->  {}.{}({}, {})",
					uri, cls.getSimpleName(), method.getName(),
					argType1.getSimpleName(), argType2.getSimpleName());

		} else if (argType1 != null) {
			logger.info("Mapping api url: {}  ->  {}.{}({})",
					uri, cls.getSimpleName(), method.getName(),
					argType1.getSimpleName());
		} else {
			logger.info("Mapping api url: {}  ->  {}.{}()",
					uri, cls.getSimpleName(), method.getName());
		}
	}

	/** 记录访问日志 */
	private void logRequestInfo(HttpExchange he) {
		URI uri = he.getRequestURI();
		// 显示请求日志
		if (logger.isDebugEnabled()) {
			String query = uri.getQuery();
			if (query == null || query.isEmpty())
				logger.debug("{} {}", he.getRequestMethod(), uri.getPath());
			else
				logger.debug("{} {}?{}", he.getRequestMethod(), uri.getPath(), query);
		}
	}

	/** 处理返回结果，生成json格式发送给调用方
	 * @param he http上下文
	 * @param path 请求路径
	 * @param status 返回的http状态
	 * @param result 返回的结果
	 * @throws IOException
	 */
	private void processResult(HttpExchange he, String path, int status, Object result) throws IOException {

		Headers headers = he.getResponseHeaders();
		headers.add("Server", serverName);
		OutputStream out = he.getResponseBody();
		Class<?> retCls = result == null ? null : result.getClass();

		if (result == null) {
			he.sendResponseHeaders(status, 0);
		} else if (retCls == String.class) {
			headers.add("Content-Type", "text/html; charset=UTF-8");
			byte[] data = ((String)result).getBytes(UTF8);
			he.sendResponseHeaders(status, data.length);
			out.write(data);
		} else if (retCls == BinResult.class) {
			BinResult br = (BinResult) result;
			headers.add("Content-Type", br.getContentType());
			he.sendResponseHeaders(status, br.getData().length);
			out.write(br.getData());
		} else {
			// 记录返回结果日志
			if (logger.isDebugEnabled())
				logger.debug("{} result: {}", path, Fmt.toJson(result));
			// 返回结果使用json方式传输
			byte[] jsonBytes = JSON.toJSONBytes(result,
					SerializerFeature.WriteDateUseDateFormat,
					SerializerFeature.DisableCircularReferenceDetect);
			// 向调用方客户端返回结果
			headers.add("Content-Type", "application/json; charset=UTF-8");
			he.sendResponseHeaders(status, jsonBytes.length);
			out.write(jsonBytes);
		}

		out.close();
	}

	/** 从流中读取文本内容直到末尾
	 * @param inputStream http请求内容流
	 * @return 读取的内容
	 */
	private final String readStringFromInputStream(InputStream inputStream) {
		String ret = null;
		PoolItem<char[]> bufItem = charsPool.get();
		PoolItem<StringBuilder> sbItem = bufferPool.get();
		char[] buf = bufItem.get();
		StringBuilder sb = sbItem.get();
		try {
			Reader reader = new BufferedReader(new InputStreamReader(inputStream, UTF8));
			int readCount;

			while ((readCount = reader.read(buf)) != -1)
				sb.append(buf, 0, readCount);

			ret = sb.toString();
		}
		catch(IOException e) {
			logger.error(e, Fmt.fmt("readStringFromInputStream error: {}", e.getMessage()));
		}
		finally {
			bufItem.recycle();
			sbItem.recycle();
		}

		return ret;
	}

	private final void setObjectProperty(Class<?> cls, Object arg,
			Object def, Map.Entry<String, List<String>> item) {

		if (item.getValue() == null || item.getValue().size() == 0)
			return;

		String key = item.getKey();
		Fmt fmt = Fmt.get();
		fmt.append(Character.toUpperCase(key.charAt(0)))
			.append(key, 1, key.length());
		String suffix = fmt.release();
		Method m = null;
		Field f = null;
		Object prop_val = null, def_val = null;
		Class<?> prop_cls = null;
		// 通过反射获取字段值
		try {
			m = cls.getMethod(Fmt.concat("get", suffix));
			prop_val = m.invoke(arg);
			prop_cls = m.getReturnType();
			def_val = m.invoke(def);
		} catch (Exception e) {
			try {
				f = cls.getField(key);
				prop_val = f.get(arg);
				prop_cls = f.getType();
				def_val = f.get(def);
			} catch (Exception e2) {
				logger.debug("read object property {} fail.", key);
				return;
			}
		}

		// 如果当前参数的属性与缺省属性不一致, 表明已由post赋值, 忽略url同名参数
		if (!Langs.isEquals(prop_val, def_val)) return;

		// 用反射对参数进行赋值
		Object new_val = Strings.valueOf(item.getValue().get(0), prop_cls);
		try {
			if (f == null) {
				m = cls.getMethod(Fmt.concat("set", suffix), prop_cls);
				m.invoke(arg, new_val);
			} else {
				f.set(arg, new_val);
			}
		} catch (Exception e) {
			logger.warn(e, "write object property {} fail.", key);
		}
	}

	public static class MethodInfo {
		public Object obj;
		public Method method;
		public String actionType;
		public String desc;
		public int authParamIndex;
		public Class<?> argType1;
		public Class<?> argType2;

		public MethodInfo(Object obj, Method method, String actionType, String desc,
				int authParamIndex, Class<?> argType1, Class<?> argType2) {
			this.obj = obj;
			this.method = method;
			this.actionType = actionType;
			this.desc = desc;
			this.authParamIndex = authParamIndex;
			this.argType1 = argType1;
			this.argType2 = argType2;
		}
	}

	/** char数组对象池 */
	private ObjectPool<char[]> charsPool = new ObjectPool<>(() -> new char[512]); 

	/** StringBuilder对象池 */
	private ObjectPool<StringBuilder> bufferPool = new ObjectPool<>(
			() -> new StringBuilder(), v -> v.setLength(0)); 

	private static class HttpRawImpl implements HttpRaw {
		private HttpExchange he;

		public HttpRawImpl(HttpExchange he) {
			this.he = he;
		}

		@Override
		public URI getRequestURI() {
			return he.getRequestURI();
		}

		@Override
		public Map<String, List<String>> getRequestHeaders() {
			return he.getRequestHeaders();
		}

		public String getRequestHeader(String name) {
			List<String> list = he.getRequestHeaders().get(name);
			if (list != null && !list.isEmpty()) return list.get(0);
			return null;
		}

		@Override
		public InputStream getRequestBody() {
			return he.getRequestBody();
		}

	}
}
