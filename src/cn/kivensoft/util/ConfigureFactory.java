package cn.kivensoft.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

public final class ConfigureFactory {
	private static final String UTF8 = "UTF-8";

	private Class<?> mainClass;
	private String rootPath;
	private String[] dirs;
	
	public ConfigureFactory(Class<?> mainClass) {
		this(mainClass, "lib", new String[]{"/conf", "/"});
	}
	
	public ConfigureFactory(Class<?> mainClass, String subPath, String[] dirs) {
		this.mainClass = mainClass;
		rootPath = getRootPath(mainClass, subPath);
		this.dirs = dirs;
	}
	
	public ConfigureFactory initLog4j(String log4jFileName) {
		if (log4jFileName == null) log4jFileName = "log4j.properties";
		initLog4j(log4jFileName, mainClass, rootPath, dirs);
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
		Map<String, Class<?>> ms = getSetMethods(cls);
		for (Map.Entry<Object, Object> entry : props.entrySet()) {
			String value = (String) entry.getValue();
			if ((value != null) && (!value.isEmpty())) {
				String name = key2Method((String) entry.getKey());
				Class<?> argType = (Class<?>) ms.get(name);
				if (argType != null) {
					try {
						Method m = cls.getMethod(name, new Class[] { argType });
						m.invoke(config,
								new Object[] { Langs.valueOf(argType, value) });
					}
					catch (Exception e) {
						MyLogger.warn(e, "invoke method error.");
					}
				}
			}
		}
		return config;
	}
	
	/** 加载配置文件, 优先以普通文件方式加载, 找不到则尝试以资源文件方式加载
	 * @param fileName 文件名
	 * @param mainClass 应用程序main函数入库所在的类
	 * @param rootPath 根目录
	 * @param dirs 尝试加载的子目录数组, 按顺序优先
	 * @return 配置文件内容, 为null表示加载失败
	 */
	public static Properties load(String fileName, Class<?> mainClass,
			String rootPath, String[] dirs) {
		InputStream is = getResourceAsStream(fileName, mainClass, rootPath, dirs);
		if (is == null) return null;
		InputStreamReader reader = null;
		try {
			reader = new InputStreamReader(is, UTF8);
			Properties props = new Properties();
			props.load(reader);
			return props;
		} catch (IOException e) {
			MyLogger.error(e);
		}
		finally {
			Langs.close(reader, is);
		}
		return null;
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
	public static void initLog4j(String fileName, Class<?> mainClass,
			String rootPath, String[] dirs) {
		//加载日志配置文件
		Properties props = load(fileName, mainClass, rootPath, dirs);
		if (props == null) return;
		/*
		Class.forName("org.apache.log4j.PropertyConfigurator")
			.getMethod("configure", Properties.class)
			.invoke(null, props);
		*/
		PropertyConfigurator.configure(props);
		MyLogger.info("加载日志配置文件: {}",
				findResource(fileName, mainClass, rootPath, dirs));
	}
	
	/** 加载资源文件, 优先以普通文件方式加载, 找不到则尝试以资源文件方式加载
	 * @param fileName 文件名
	 * @param mainClass 应用程序main函数入库所在的类
	 * @param rootPath 根目录
	 * @param dirs 尝试加载的子目录数组, 按顺序优先
	 * @return 文件的输入流, 为null则加载失败
	 */
	public static InputStream getResourceAsStream(String fileName,
			Class<?> mainClass, String rootPath, String[] dirs) {
		File f = null;
		int len = dirs.length;
		// 优先加载磁盘路径下的文件
		for (int i = 0; i < len; ++i) {
			f = new File(joinPath(rootPath, dirs[i], fileName));
			if (f.exists()) {
				try {
					FileInputStream fs = new FileInputStream(f);
					MyLogger.debug("load resource: {}", f.getAbsolutePath());
					return fs;
				} catch (FileNotFoundException e) {
					MyLogger.error(e, "can't open resource {}: {}",
							f.getAbsolutePath(), e.getMessage());
				}
			}
		}
		// 尝试以加载资源的方式加载文件
		for (int i = 0; i < len; ++i) {
			String p = joinPath("/", dirs[i], fileName);
			InputStream is = mainClass.getResourceAsStream(p);
			if (is != null) {
				MyLogger.debug("load resource: ", p);
				return is;
			}
		}
		return null;
	}
	
	/** 查找资源文件, 优先查找普通文件方式, 找不到则尝试查找资源文件
	 * @param fileName 文件名
	 * @param mainClass 应用程序main函数入库所在的类
	 * @param rootPath 根目录
	 * @param dirs 尝试查找的子目录数组, 按顺序优先
	 * @return 找到的文件全路径名, '/'开头表示是资源文件, 为null则查找失败
	 */
	public static String findResource(String fileName,
			Class<?> mainClass, String rootPath, String[] dirs) {
		File f = null;
		int len = dirs.length;
		// 查找磁盘路径下的文件
		for (int i = 0; i < len; ++i) {
			f = new File(joinPath(rootPath, dirs[i], fileName));
			if (f.exists()) return f.getAbsolutePath();
		}
		// 查找资源文件
		for (int i = 0; i < len; ++i) {
			String path = joinPath("/", dirs[i], fileName);
			InputStream is = mainClass.getResourceAsStream(path);
			if (is != null) {
				Langs.close(is);
				return path;
			}
		}
		return null;
	}
	
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
	  
	private static Map<String, Class<?>> getSetMethods(Class<?> cls) {
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

	private static String joinPath(String... paths) {
		Fmt f = Fmt.get();
		for (int i = 0, n = paths.length; i < n; ++i) {
			String p = paths[i];
			if (p == null || p.isEmpty()) 
				continue;
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
