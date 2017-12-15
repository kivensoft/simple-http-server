package com.kivensoft.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.kivensoft.util.Fmt;
import com.kivensoft.util.MyLogger;
import com.kivensoft.util.ObjectPool;
import com.kivensoft.util.ScanPackage;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class SimpleHttpServer implements HttpHandler {
	private static final String UTF_8 = "UTF-8";
	private static final String DEFAULT_SERVER_NAME = "SimpleHttpServer";
	private static final String HTTP_VERSION = "1.0";
	private static final String CTRL_NAME = "Controller";
	
	private static enum EError { 
		系统内部错误, 参数解析失败, 请求的地址不存在;
		public int code() { return 100000 + ordinal(); }
	}

	private HttpServer httpServer;
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

	/** 映射类的公共静态函数到api地址
	 * @param prefix 地址前缀
	 * @param cls 要映射的类
	 */
	public void mapController(String prefix, Class<?> cls) {
		if (!cls.getSimpleName().endsWith(CTRL_NAME)) {
			MyLogger.warn("can't mapping class {} beacause it isn't \"Controller\" suffix");
			return;
		}
		StringBuilder sb = new StringBuilder();
		pathAppend(sb, prefix);
		mapClass(cls, sb, true);
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

		List<Class<?>> clss = ScanPackage.getClasses(packageName, true,
				clsName -> clsName.endsWith(CTRL_NAME));
		for (int i = 0, n = clss.size(); i < n; ++i) {
			mapClass(clss.get(i), sb, true);
			sb.setLength(prefix_len);
		}
	}

	/** HTTP请求处理函数，由系统回调，应用程序不可调用 */
	@Override
	public void handle(HttpExchange he) throws IOException {
		logRequestInfo(he);

		// 解析请求路径及请求参数，找到系统对应的处理函数进行调用处理，处理调用结果
		String path = he.getRequestURI().getPath().toLowerCase();
		ApiResult ret = null;
		int httpCode = 200; // http成功代码
		try {
			// 查找路径对应的处理函数并解析参数，然后进行调用
			MethodInfo act = handles.get(path);
			if (act != null)
				ret = invokeMethodInfo(he, act);
			else {
				ret = ApiResult.error(EError.请求的地址不存在.code(), EError.请求的地址不存在.name());
				httpCode = 404; // http找不到页面错误
			}
		} catch (Exception e) {
			MyLogger.error(e, "process {} has error.", path);
			ret = ApiResult.error(EError.系统内部错误.code(), EError.系统内部错误.name());
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
	
	/** 映射类cls中的函数到url */
	final private void mapClass(Class<?> cls, StringBuilder sb, boolean useAnnotation) {
		if (useAnnotation) {
			// 如果类没有RequestMapping注解，则不是控制器
			RequestMapping mapping = cls.getAnnotation(RequestMapping.class);
			if (mapping == null) return;
			pathAppend(sb, mapping.value());
		}
		else {
			String name = cls.getSimpleName();
			pathAppend(sb, name.substring(0, name.length() - CTRL_NAME.length()));
		}
		
		Object ctrl = null;
		try {
			ctrl = cls.newInstance();
		} catch (Exception e) {
			return;
		}
		int prefix_len = sb.length();
		Method[] ms = cls.getMethods();
		for (int i = 0, n = ms.length; i < n; ++i) {
			Method m = ms[i];
			// 函数类型必须是：1、返回类型ApiResult
			//                2、小于2个请求参数
			//                3、具备RequestMapping注解
			if (m.getReturnType() != ApiResult.class) continue;
			Class<?>[] ps = m.getParameterTypes();
			// 如果是2个参数，第一个必须是Map<String, Object>类型
			if (ps.length > 2 || ps.length == 2 && !Map.class.isAssignableFrom(ps[0]))
				continue;

			String desc = null;
			if (useAnnotation) {
				RequestMapping rm = m.getAnnotation(RequestMapping.class);
				if (rm == null) continue;
				pathAppend(sb, rm.value());
				desc = rm.desc();
			}
			else pathAppend(sb, m.getName());
			
			Class<?> arg1Type = ps.length == 0 ? null : ps[0];
			Class<?> arg2Type = ps.length == 2 ? ps[1] : null;
			String uri = sb.toString();
			handles.put(uri, new MethodInfo(ctrl, m, arg1Type, arg2Type, desc));
			logMappingInfo(uri, cls, m, arg1Type, arg2Type);
			sb.setLength(prefix_len);
		}
	}
	
	/** url路径增加子路径，必须开头有斜杠和结尾不能有斜杠 */
	final private void pathAppend(StringBuilder sb, String path) {
		if (path == null || path.isEmpty()) return;
		if (path.charAt(0) != '/') sb.append('/');
		sb.append(path);
		if (sb.charAt(sb.length() - 1) == '/') sb.setLength(sb.length() - 1);
	}
	
	/** 根据MethodInfo的参数个数进行相应的函数处理 */
	final private ApiResult invokeMethodInfo(HttpExchange he, MethodInfo act) throws Exception {
		// 0个参数
		if (act.arg1Type == null)
			return (ApiResult) act.method.invoke(act.obj);
		// 1个参数
		Object arg1 = Map.class.isAssignableFrom(act.arg1Type)
				? parseQuery(he.getRequestURI().getRawQuery())
				: parseBody(he, act.arg1Type);
		if (act.arg2Type == null)
			return (ApiResult) act.method.invoke(act.obj, arg1);
		// 2个参数
		Object arg2 = parseBody(he, act.arg2Type);
		return (ApiResult) act.method.invoke(act.obj, arg1, arg2);
	}

	/** 解析POST请求的body内容的json格式参数,返回类型为参数cls类型 */
	final private Object parseBody(HttpExchange he, Class<?> cls) {
		try {
			String body = readStringFromInputStream(he.getRequestBody());
			MyLogger.debug("request body: {}", body);
			Object ret = body == null || body.isEmpty()
					? cls.newInstance() : JSON.parseObject(body, cls);
			if (MyLogger.isDebugEnabled())
				MyLogger.debug("parseBody result: {}", JSON.toJSONString(ret,
					SerializerFeature.WriteDateUseDateFormat,
					SerializerFeature.DisableCircularReferenceDetect));
			return ret;

		} catch (Exception e) {
			MyLogger.error(e, "parseBody error.");
			return null;
		}
	}
	
	/** 解析url地址带的参数成hashmap类型返回值 */
	@SuppressWarnings("unchecked")
	final private HashMap<String, Object> parseQuery(String query) throws UnsupportedEncodingException {
		HashMap<String, Object> ret = new HashMap<>();
		if (query == null || query.isEmpty()) return ret;
		int start, idx = -1;
		do {
			// 获取key和value
			start = idx + 1;
			idx = query.indexOf('&', start);
			int pos = query.indexOf('=', start);
			if (pos <= start || pos >= idx && idx > 0) {
				MyLogger.debug("parseQuery warning, query string can't parse");
				continue;
			}
			String key = URLDecoder.decode(query.substring(start, pos), UTF_8);
			String value = URLDecoder.decode(query.substring(
					pos + 1, idx > 0 ? idx : query.length()), UTF_8);
			
			// 写入键值到字典表中
			Object old_value = ret.putIfAbsent(key, value);
			if (old_value != null) {
				List<String> values;
				if (old_value instanceof String) {
					values = new ArrayList<>();
					values.add((String)old_value);
				}
				else if (old_value instanceof List<?>) {
					values = (List<String>)old_value;
				}
				else continue;
				values.add(value);
				ret.put(key, values);
			}
		} while (idx != -1);
		
		return ret;
	}
	
	/** 记录映射api的条目 */
	private void logMappingInfo(String uri, Class<?> cls, Method method,
			Class<?> arg1Type, Class<?> arg2Type) {
		if (MyLogger.isInfoEnabled()) {
			String paramsDefine;
			if (arg2Type != null)
				paramsDefine = Fmt.concat(arg1Type.getSimpleName(),
						", ", arg2Type.getSimpleName());
			else if (arg1Type != null)
				paramsDefine = arg1Type.getSimpleName();
			else paramsDefine = "";
			MyLogger.info("Mapping api url: {}  ->  {}.{}({})",
					uri, cls.getSimpleName(), method.getName(), paramsDefine);
		}
	}

	/** 记录访问日志 */
	private void logRequestInfo(HttpExchange he) {
		URI uri = he.getRequestURI();
		// 显示请求日志
		if (MyLogger.isDebugEnabled()) {
			String query = uri.getQuery();
			if (query == null || query.isEmpty())
				MyLogger.debug("{} {}", he.getRequestMethod(), uri.getPath());
			else
				MyLogger.debug("{} {}?{}", he.getRequestMethod(), uri.getPath(), query);
		}
	}
	
	/** 处理返回结果，生成json格式发送给调用方
	 * @param he http上下文
	 * @param path 请求路径
	 * @param status 返回的http状态
	 * @param result 返回的结果
	 * @throws IOException
	 */
	private void processResult(HttpExchange he, String path, int status,
			ApiResult result) throws IOException {
		// 记录返回结果日志
		if (MyLogger.isDebugEnabled())
			MyLogger.debug("{} result: {}", path, JSON.toJSONString(result,
					SerializerFeature.WriteDateUseDateFormat,
					SerializerFeature.DisableCircularReferenceDetect));

		// 返回结果使用json方式传输
		byte[] jsonBytes = JSON.toJSONBytes(result,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect);
		
		// 向调用方客户端返回结果
		Headers headers = he.getResponseHeaders();
		headers.add("Server", serverName);
		headers.add("Content-Type", "application/json; charset=UTF-8");
		he.sendResponseHeaders(status, jsonBytes.length);
		OutputStream out = he.getResponseBody();
		out.write(jsonBytes);
		out.close();
	}
	
	/** 从流中读取文本内容直到末尾
	 * @param inputStream http请求内容流
	 * @return 读取的内容
	 */
	private final String readStringFromInputStream(InputStream inputStream) {
		String ret = null;
		try {
			Reader reader = new BufferedReader(new InputStreamReader(inputStream, UTF_8));
			CharsItem ciItem = charsPool.get();
			BufferItem bItem = bufferPool.get();
			char[] buf = ciItem.buf;
			int readCount;

			while ((readCount = reader.read(buf)) != -1)
				bItem.sb.append(buf, 0, readCount);
			
			ret = bItem.sb.toString();
			ciItem.recycle();
			bItem.recycle();
		}
		catch(IOException e) {
			MyLogger.error(e, "读取网络输入流时错误.");
		}
		
		return ret;
	}
	
	private class MethodInfo {
		public Object obj;
		public Method method;
		public Class<?> arg1Type;
		public Class<?> arg2Type;
		public String desc;

		public MethodInfo(Object obj, Method method,
				Class<?> arg1Type, Class<?> arg2Type, String desc) {
			this.obj = obj;
			this.method = method;
			this.arg1Type = arg1Type;
			this.arg2Type = arg2Type;
			this.desc = desc;
		}
	}
	
	/** char数组对象池 */
	private ObjectPool<CharsItem> charsPool = new ObjectPool<>(() -> new CharsItem()); 
	private class CharsItem extends ObjectPool.Item {
		char[] buf = new char[512];
	}
	
	/** StringBuilder对象池 */
	private ObjectPool<BufferItem> bufferPool = new ObjectPool<>(() -> new BufferItem()); 
	private class BufferItem extends ObjectPool.Item {
		StringBuilder sb = new StringBuilder(1024);
		@Override
		protected void clear() {
			super.clear();
			sb.setLength(0);
		}
	}
}
