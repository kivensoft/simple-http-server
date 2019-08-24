package cn.kivensoft.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

final public class Resources {
	private static final String UTF8 = "UTF-8";
	private static final String LOGBACK = "logback.xml";
	private static final String LOG4J = "log4j.properties";
	private static final String LIB = "/lib";
	private static final String CONF = "/conf";
	private static final String ROOT = "/";

	/** 从配置文件获取配置内容
	 * @param configFile 配置文件名
	 * @param mainClass 配置文件所在的主类
	 * @param config 配置实例
	 * @return
	 */
	public static <T> T loadToObject(String configFile, Class<?> mainClass, T config) throws Exception {
		Properties props = loadProperties(configFile, mainClass, LIB, CONF, ROOT);
		if (props == null) return config;

		Class<?> cls = config.getClass();
		Map<String, Method> ms = findSetMethods(cls);

		for (Map.Entry<Object, Object> entry : props.entrySet()) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();

			if (value == null || value.isEmpty()) continue;

			// 先尝试setXXX方式设置
			Method m = ms.get(key2Method(key));
			if (m != null) {
				Object arg = Strings.valueOf(value, m.getParameterTypes()[0]);
				m.setAccessible(true);
				try {
					// setXXX方式成功, 直接跳转到下一个循环
					m.invoke(config, arg);
					continue;
				} catch (Exception e) { }
			}

			// setXXX不成功, 尝试直接设置公共字段
			String fieldName = key2Field(key);
			try {
				Field f = cls.getField(fieldName);
				f.setAccessible(true);
				f.set(config, Strings.valueOf(value, f.getType()));
			} catch (Exception e) { }
		}

		return config;
	}

	/**
	 * @param configFile 配置文件名，系统默认从根目录读取，最好只是不带路径的文件名
	 * @param mainClass 配置文件所在jar包的类
	 * @return 配置内容
	 */
	public static Properties loadProperties(String configFile, Class<?> mainClass) throws Exception {
		return loadProperties(configFile, mainClass, LIB, CONF, ROOT);
	}


	/** 加载配置文件, 优先以普通文件方式加载, 找不到则尝试以资源文件方式加载
	 * @param filename 文件名
	 * @param mainClass 应用程序main函数入库所在的类
	 * @param libPath 根目录
	 * @param dirs 尝试加载的子目录数组, 按顺序优先
	 * @return 配置文件内容, 为null表示加载失败
	 */
	public static Properties loadProperties(String filename, Class<?> mainClass,
			String libPath, String... dirs) throws Exception {

		InputStream is = getResourceAsStream(filename, mainClass, libPath, dirs);
		InputStreamReader reader = null;

		try {
			reader = new InputStreamReader(is, UTF8);
			Properties props = new Properties();
			props.load(reader);
			return props;
		} finally {
			Langs.close(reader, is);
		}
	}

	/** 获取App路径, 如果是jar包且在lib目录, 则上级目录是app目录 */ 
	public static String getRootPath(Class<?> mainClass) {
		return getRootPath(mainClass, LIB);
	}

	/** 获取程序路径，主要是要区分部署与开发之间的路径区别 
	 * @param mainClass 应用程序main函数入库所在的类
	 * @param libPath 如果是jar包, 指定jar包所在的相对子路径
	 * @return 根目录
	 */
	public static String getRootPath(Class<?> mainClass, String libPath) {

		// 获取类所在的路径, 返回的可能是直接的路径或者jar包的全路径名
		java.net.URL url = mainClass.getProtectionDomain().getCodeSource().getLocation();
		String path;
		try {
			path = java.net.URLDecoder.decode(url.getPath(), UTF8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("UnsupportedEncodingException", e);
		}

		if(path.endsWith(".jar")) { //在部署环境
			path = path.substring(0, path.lastIndexOf('/'));
			libPath = Strings.joinPath("/", libPath);
			if (libPath.length() > 0 && path.endsWith(libPath))
				path = path.substring(0, path.length() - libPath.length());
		}

		return path;
	}

	public static void initLog4j(Class<?> mainClass) throws Exception {
		initLog4j(LOG4J, mainClass, LIB, CONF, ROOT);
	}

	public static void initLog4j(String filename, Class<?> mainClass) throws Exception {
		initLog4j(LOG4J, mainClass, LIB, CONF, ROOT);
	}

	public static void initLog4j(String filename, Class<?> mainClass,
			String libPath, String... dirs) throws Exception {

		Properties props = loadProperties(filename, mainClass, libPath, dirs);

		Class.forName("org.apache.log4j.PropertyConfigurator")
			.getMethod("configure", Properties.class)
			.invoke(null, props);

		System.out.printf("init log4j configure %s success.\n",
				findResource(filename, mainClass, libPath, dirs));
	}

	public static void initLogback(Class<?> mainClass) throws Exception {
		initLogback(LOGBACK, mainClass, LIB, CONF, ROOT);
	}

	public static void initLogback(String filename, Class<?> mainClass) throws Exception {
		initLogback(filename, mainClass, LIB, CONF, ROOT);
	}

	public static void initLogback(String filename, Class<?> mainClass,
			String libPath, String... dirs) throws Exception {

		InputStream is = getResourceAsStream(filename, mainClass, libPath, dirs);
		if (is == null) {
			System.err.printf("init logback configure file %s error, can't read.\n",
					findResource(filename, mainClass, libPath, dirs));
			return;
		}

		Class<?> fac_cls = Class.forName("org.slf4j.LoggerFactory");
		Class<?> lctx_cls = Class.forName("ch.qos.logback.classic.LoggerContext");
		Object loggerContext = fac_cls.getMethod("getILoggerFactory").invoke(null);
		lctx_cls.getMethod("reset").invoke(loggerContext);
		//LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
		//loggerContext.reset();

		Class<?> ctx_cls = Class.forName("ch.qos.logback.core.Context");
		Class<?> conf_cls = Class.forName("ch.qos.logback.classic.joran.JoranConfigurator");
		Object joranConfigurator = conf_cls.newInstance();
		conf_cls.getMethod("setContext", ctx_cls)
				.invoke(joranConfigurator, loggerContext);
		conf_cls.getMethod("doConfigure", InputStream.class)
				.invoke(joranConfigurator, is);
		//Object logger = fac_cls.getMethod("getLogger", String.class)
		//		.invoke(null, Resource.class.getName());
		//Class<?> log_cls = Class.forName("org.slf4j.Logger");
		//log_cls.getMethod("info", String.class, Object.class).invoke(logger,
		//		"init logback configure {} success.",
		//		findResource(filename, mainClass, libPath, dirs));
		System.out.printf("init logback configure %s success.\n",
				findResource(filename, mainClass, libPath, dirs));
		//JoranConfigurator joranConfigurator = new JoranConfigurator();
		//joranConfigurator.setContext(loggerContext);
		//joranConfigurator.doConfigure(is);
	}

	public static InputStream getResourceAsStream(String filename, Class<?> mainClass) throws Exception {
		return getResourceAsStream(filename, mainClass, LIB, CONF, ROOT);
	}

	/** 加载资源文件, 优先以普通文件方式加载, 找不到则尝试以资源文件方式加载
	 * @param filename 文件名
	 * @param mainClass 应用程序main函数入库所在的类
	 * @param rootPath 根目录
	 * @param dirs 尝试加载的子目录数组, 按顺序优先
	 * @return 文件的输入流, 为null则加载失败
	 */
	public static InputStream getResourceAsStream(String filename,
			Class<?> mainClass, String libPath, String... filePaths) throws IOException {

		File f = null;
		int len = filePaths.length;
		String rootPath = getRootPath(mainClass, libPath);
		// 优先加载磁盘路径下的文件
		for (int i = 0; i < len; ++i) {
			f = new File(Strings.joinPath(rootPath, filePaths[i], filename));
			if (f.exists())
				return new FileInputStream(f);
		}
		// 尝试以加载资源的方式加载文件
		for (int i = 0; i < len; ++i) {
			String p = Strings.joinPath(ROOT, filePaths[i], filename);
			InputStream is = mainClass.getResourceAsStream(p);
			if (is != null) {
				return is;
			}
		}

		return null;
	}

	/** 查找资源文件, 优先查找普通文件方式, 找不到则尝试查找资源文件
	 * @param filename 文件名
	 * @param mainClass 应用程序main函数入库所在的类
	 * @param rootPath 根目录
	 * @param dirs 尝试查找的子目录数组, 按顺序优先
	 * @return 找到的文件全路径名, '/'开头表示是资源文件, 为null则查找失败
	 */
	public static String findResource(String filename, Class<?> mainClass, String libPath, String... dirs) {
		File f = null;
		int len = dirs.length;
		String rootPath = getRootPath(mainClass, libPath);

		// 查找磁盘路径下的文件
		for (int i = 0; i < len; ++i) {
			f = new File(Strings.joinPath(rootPath, dirs[i], filename));
			if (f.exists()) return f.getAbsolutePath();
		}

		// 查找资源文件
		for (int i = 0; i < len; ++i) {
			String path = Strings.joinPath("/", dirs[i], filename);
			InputStream is = mainClass.getResourceAsStream(path);
			if (is != null) {
				Langs.close(is);
				return path;
			}
		}

		return null;
	}

	/** 配置文件中的多个"."分割的配置键转换成设置属性函数 */
	private static String key2Field(String key) {
		return transKey(key, null);
	}

	/** 配置文件中的多个"."分割的配置键转换成设置属性函数 */
	private static String key2Method(String key) {
		return transKey(key, "set");
	}

	private static String transKey(String key, String prefix) {
		StringBuilder sb = new StringBuilder();

		if (prefix != null)
			sb.append(prefix).append(Character.toUpperCase(key.charAt(0)));
		else sb.append(key.charAt(0));

		boolean dot = false;
		for (int i = 1, imax = key.length(); i < imax; i++) {
			char c = key.charAt(i);
			if (c != '.') {
				if (dot) {
					sb.append(Character.toUpperCase(c));
					dot = false;
				}
				else sb.append(c);
			}
			else dot = true;
		}

		return sb.toString();
	}

	/** 只查找getXXX函数，换成setXXX函数 **/
	public static Map<String, Method> findSetMethods(Class<?> cls) {
		Map<String, Method> ret = new HashMap<>();

		// 查找所有的setXXX函数
		for (Method m : cls.getMethods()) {
			String name = m.getName();
			//只查找setXXX函数
			if ((name.length() >= 4) && (name.startsWith("set"))
					&& (m.getParameterCount() == 1)) {
				ret.put(name, m);
			}
		}

		return ret;
	}

}
