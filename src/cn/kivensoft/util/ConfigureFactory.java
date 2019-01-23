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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigureFactory {
	private static final String UTF8 = "UTF-8";
	private static final String LOG4J = "log4j.properties";
	private static final String LOGBACK = "logback.xml";

	private Class<?> mainClass;
	private String rootPath;
	private String[] dirs;
	
	public static ConfigureFactory of(Class<?> mainClass) {
		return new ConfigureFactory(mainClass, "lib", "/conf", "/");
	}
	
	public static ConfigureFactory of(Class<?> mainClass, String mainClassPath, String... dirs) {
		return new ConfigureFactory(mainClass, mainClassPath, dirs);
	}

	private ConfigureFactory(Class<?> mainClass, String mainClassPath, String... dirs) {
		this.mainClass = mainClass;
		rootPath = getRootPath(mainClass, mainClassPath);
		this.dirs = dirs;
	}
	
	public ConfigureFactory initLog4j() {
		return initLog4j(LOG4J);
	}
	
	public ConfigureFactory initLog4j(String log4jFileName) {
		initLog4j(log4jFileName, mainClass, rootPath, dirs);
		return this;
	}
	
	public ConfigureFactory initLogback() {
		return initLogback(LOGBACK);
	}
	
	public ConfigureFactory initLogback(String logbackFileName) {
		initLogback(logbackFileName, mainClass, rootPath, dirs);
		return this;
	}
	
	/**
	 * @param configFile 配置文件名，系统默认从根目录读取，最好只是不带路径的文件名
	 * @param mainClass 配置文件所在jar包的类
	 * @param subDir jar包所在的相对子路径, 为null时忽略
	 * @param dirs 加载配置文件的相对路径数组, 按数组循序优先查找
	 * @return 配置内容
	 */
	public Properties get(String configFile) {
		Properties props = load(configFile, mainClass, rootPath, dirs);
		return props == null ? new Properties() : props;
	}

	/** 从配置文件获取配置内容
	 * @param configFile 配置文件名
	 * @param mainClass 配置文件所在的主类
	 * @param config 配置实例
	 * @return
	 */
	public <T> T get(String configFile, T config) {
		Properties props = load(configFile, mainClass, rootPath, dirs);
		if (props == null) return config;
		Class<?> cls = config.getClass();
		Map<String, Class<?>> ms = findSetMethods(cls);
		for (Map.Entry<Object, Object> entry : props.entrySet()) {
			String value = (String) entry.getValue();
			
			// 尝试写入到公共字段, 写入成功直接进入下一循环
			try {
				Field field = cls.getField((String)entry.getKey());
				Object obj_arg = Strings.valueOf(field.getType(), value);
				field.set(config, obj_arg);
				continue;
			} catch (Exception e) { }
			
			// 尝试写入到set函数中, 写入成功直接进入下一循环
			String name = key2Method((String)entry.getKey());
			Class<?> argType = (Class<?>) ms.get(name);
			// 找不到set函数, 进入下一次循环
			if (argType == null) continue;
			try {
				Class<?> cls_arg = argType;
				Method m = cls.getMethod(name, cls_arg);
				Object obj_arg = Strings.valueOf(argType, value);
				m.invoke(config, obj_arg);
				continue;
			}
			catch (Exception e) { }
		}
		return config;
	}
	
	/** 加载配置文件, 优先以普通文件方式加载, 找不到则尝试以资源文件方式加载
	 * @param filename 文件名
	 * @param mainClass 应用程序main函数入库所在的类
	 * @param rootPath 根目录
	 * @param dirs 尝试加载的子目录数组, 按顺序优先
	 * @return 配置文件内容, 为null表示加载失败
	 */
	public static Properties load(String filename, Class<?> mainClass,
			String rootPath, String[] dirs) {
		InputStream is = getResourceAsStream(filename, mainClass, rootPath, dirs);
		InputStreamReader reader = null;
		try {
			reader = new InputStreamReader(is, UTF8);
			Properties props = new Properties();
			props.load(reader);
			return props;
		} catch(IOException e) {
			LoggerFactory.getLogger(ConfigureFactory.class).error(
					Fmt.fmt("load property file {} error: {}", filename, e.getMessage()), e);
			return null;
		} finally {
			Langs.close(reader, is);
		}
	}
	
	/** 获取程序路径，主要是要区分部署与开发之间的路径区别 
	 * @param mainClass 应用程序main函数入库所在的类
	 * @param subPath 如果是jar包, 指定jar包所在的相对子路径
	 * @return 根目录
	 */
	public static String getRootPath(Class<?> mainClass, String subPath) {
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
			subPath = joinPath("/", subPath);
			if (subPath.length() > 0 && path.endsWith(subPath))
				path = path.substring(0, path.length() - subPath.length());
		}
		return path;
	}

	/** 初始化自定义的日志文件配置，在应用的根目录查找  */
	public static void initLog4j(String filename, Class<?> mainClass,
			String rootPath, String[] dirs) {
		Logger logger = LoggerFactory.getLogger(ConfigureFactory.class);
		//加载日志配置文件
		Properties props = load(filename, mainClass, rootPath, dirs);
		if (props == null) {
			logger.warn("init log4j configure file {} error, can't read.",
					findResource(filename, mainClass, rootPath, dirs));
			return;
		}

		try {
			Class.forName("org.apache.log4j.PropertyConfigurator")
				.getMethod("configure", Properties.class)
				.invoke(null, props);
			logger.info("init log4j configure {} success",
					findResource(filename, mainClass, rootPath, dirs));
		} catch (Exception e) {
			logger.error(Fmt.fmt("init log4j configure {} error: {}",
					filename, e.getMessage()), e);
		}
	}
	
	public static void initLogback(String filename, Class<?> mainClass,
			String rootPath, String[] dirs) {
		InputStream is = getResourceAsStream(filename, mainClass, rootPath, dirs);
		if (is == null) {
			LoggerFactory.getLogger(ConfigureFactory.class).warn(
					"init logback configure file {} error, can't read.",
					findResource(filename, mainClass, rootPath, dirs));
			return;
		}
		try {
			Class<?> ctx_cls = Class.forName("ch.qos.logback.classic.LoggerContext");
			Object loggerContext = LoggerFactory.getILoggerFactory();
			ctx_cls.getMethod("reset").invoke(loggerContext);
			//LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
			//loggerContext.reset();
			Class<?> conf_cls = Class.forName("ch.qos.logback.classic.joran.JoranConfigurator");
			Object joranConfigurator = conf_cls.newInstance();
			conf_cls.getMethod("setContext", ctx_cls)
					.invoke(joranConfigurator, loggerContext);
			conf_cls.getMethod("doConfigure", InputStream.class)
					.invoke(joranConfigurator, is);
			LoggerFactory.getLogger(ConfigureFactory.class).info(
					"init logback configure {} success",
					findResource(filename, mainClass, rootPath, dirs));
			//JoranConfigurator joranConfigurator = new JoranConfigurator();
			//joranConfigurator.setContext(loggerContext);
			//joranConfigurator.doConfigure(is);
		} catch (Exception e) {
			LoggerFactory.getLogger(ConfigureFactory.class).error(
					Fmt.fmt("init logback configure {} error: {}",
							filename, e.getMessage()), e);
		}
	}
	
	/** 加载资源文件, 优先以普通文件方式加载, 找不到则尝试以资源文件方式加载
	 * @param filename 文件名
	 * @param mainClass 应用程序main函数入库所在的类
	 * @param rootPath 根目录
	 * @param dirs 尝试加载的子目录数组, 按顺序优先
	 * @return 文件的输入流, 为null则加载失败
	 */
	public static InputStream getResourceAsStream(String filename,
			Class<?> mainClass, String rootPath, String[] dirs) {
		Logger logger = LoggerFactory.getLogger(ConfigureFactory.class);
		File f = null;
		int len = dirs.length;
		// 优先加载磁盘路径下的文件
		for (int i = 0; i < len; ++i) {
			f = new File(joinPath(rootPath, dirs[i], filename));
			if (f.exists()) {
				try {
					FileInputStream fs = new FileInputStream(f);
					return fs;
				} catch (IOException e) {
					logger.error(Fmt.fmt("load resource {} error: {}",
							f.getAbsolutePath()), e);
					return null;
				}
			}
		}
		// 尝试以加载资源的方式加载文件
		for (int i = 0; i < len; ++i) {
			String p = joinPath("/", dirs[i], filename);
			InputStream is = mainClass.getResourceAsStream(p);
			if (is != null) return is;
		}
		
		logger.error("load resource error, can't find {} in {}", Fmt.concat(dirs));
		return null;
	}
	
	/** 查找资源文件, 优先查找普通文件方式, 找不到则尝试查找资源文件
	 * @param filename 文件名
	 * @param mainClass 应用程序main函数入库所在的类
	 * @param rootPath 根目录
	 * @param dirs 尝试查找的子目录数组, 按顺序优先
	 * @return 找到的文件全路径名, '/'开头表示是资源文件, 为null则查找失败
	 */
	public static String findResource(String filename,
			Class<?> mainClass, String rootPath, String[] dirs) {
		File f = null;
		int len = dirs.length;
		// 查找磁盘路径下的文件
		for (int i = 0; i < len; ++i) {
			f = new File(joinPath(rootPath, dirs[i], filename));
			if (f.exists()) return f.getAbsolutePath();
		}
		// 查找资源文件
		for (int i = 0; i < len; ++i) {
			String path = joinPath("/", dirs[i], filename);
			InputStream is = mainClass.getResourceAsStream(path);
			if (is != null) {
				Langs.close(is);
				return path;
			}
		}
		return null;
	}
	
	/** 配置文件中的多个"."分割的配置键转换成设置属性函数 */
	private static String key2Method(String key) {
		Fmt f = Fmt.get().append("set").append(Character.toUpperCase(key.charAt(0)));
		boolean dot = false;
		for (int i = 1, n = key.length(); i < n; i++) {
			char c = key.charAt(i);
			if (c != '.') {
				if (dot) {
					f.append(Character.toUpperCase(c));
					dot = false;
				}
				else f.append(c);
			}
			else dot = true;
		}
		return f.release();
	}
	  
	/** 只查找getXXX函数，换成setXXX函数 **/
	public static Map<String, Class<?>> findSetMethods(Class<?> cls) {
		Map<String, Class<?>> ret = new HashMap<>();
		StringBuilder sb = new StringBuilder();
		for (Method m : cls.getMethods()) {
			String name = m.getName();
			//只查找getXXX函数，换成setXXX函数
			if ((name.length() >= 4) && (name.startsWith("get"))
					&& (!name.equals("getClass"))
					&& (m.getParameterCount() <= 0)) {
				ret.put(sb.append('s').append(name, 1, name.length())
						.toString(), m.getReturnType());
				sb.setLength(0);
			}
		}
		return ret;
	}

	/** 连接多个个路径串成一个路径名, 主要是自动处理头尾两端的路径分隔符 */
	public static String joinPath(String... paths) {
		Fmt f = Fmt.get();
		for (int i = 0, n = paths.length; i < n; ++i) {
			String p = paths[i];
			if (p == null || p.length() == 0) continue;
			if (f.length() > 0) {
				char c = f.charAt(f.length() - 1);
				if (c != '/' && c != '\\') f.append('/');
			}
			if (p.charAt(0) == '/') f.append(p, 1, p.length());
			else f.append(p);
		}
		return f.release();
	}

}
