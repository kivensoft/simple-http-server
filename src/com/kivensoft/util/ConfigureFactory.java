package com.kivensoft.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class ConfigureFactory {
	private static final String UTF8 = "UTF-8";
	private static final String LOG_CONF = "log4j.properties";

	private ConfigureFactory() { }

	/**
	 * @param configFile 配置文件名，系统默认从根目录读取，最好只是不带路径的文件名
	 * @param cls 配置文件所在类
	 * @return 配置内容
	 */
	public static Properties get(String configFile, Class<?> mainClass) {
		String _rootPath = getRootPath(mainClass);
		
		//初始化日志配置
		initLog4j(_rootPath, mainClass);
		
		Properties props = load(configFile, _rootPath, mainClass);
		return props == null ? new Properties() : props;
	}

	/** 从配置文件获取配置内容
	 * @param configFile 配置文件名
	 * @param mainClass 配置文件所在的主类
	 * @param config 配置实例
	 * @return
	 */
	public static <T> T get(String configFile, Class<?> mainClass, T config) {
		Properties props = get(configFile, mainClass);
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
	
	/** 加载配置文件内容到Properties实例中 */
	public static Properties load(String configFile, String rootPath, Class<?> mainClass) {
		//加载配置文件,优先从部署目录中读取，找不到时才从jar包中读取
		File f = new File(rootPath, configFile);
		Reader r = null;
		Properties props = null;
		try {
			r = f.exists() ? new InputStreamReader(new FileInputStream(f), UTF8)
					: new InputStreamReader(mainClass.getResourceAsStream("/" + configFile), UTF8);
			props = new Properties();
			props.load(r);
		} catch (IOException e) {
			MyLogger.warn(e, "load properties error.");
		}
		if (r != null) try {r.close(); } catch (IOException e) {}
		return props;
	}

	/** 获取程序路径，主要是要区分部署与开发之间的路径区别 
	 * @param mainClass 类
	 * @return 根目录
	 * @throws Exception
	 */
	public static String getRootPath(Class<?> mainClass) {
		java.net.URL url = mainClass.getProtectionDomain().getCodeSource().getLocation();
		String path;
		try {
			path = java.net.URLDecoder.decode(url.getPath(), UTF8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("UnsupportedEncodingException", e);
		}
		
		if(path.endsWith(".jar")) { //在部署环境
			path = path.substring(0, path.lastIndexOf('/') + 1);
		}
		return path;
	}

	/** 初始化自定义的日志文件配置，在应用的根目录查找  */
	private static void initLog4j(String rootPath, Class<?> mainClass) {
		//加载日志配置文件
		Properties props = load(LOG_CONF, rootPath, mainClass);
		if (props == null) return;
		try {
			Class.forName("org.apache.log4j.PropertyConfigurator")
				.getMethod("configure", Properties.class)
				.invoke(null, props);
			MyLogger.info("加载日志配置文件: {}",
					new File(rootPath, LOG_CONF).getAbsoluteFile().toString());
		} catch (Exception e) {
			MyLogger.warn(e, "read {} error.",
					new File(rootPath, LOG_CONF).getAbsoluteFile().toString());
		}
		//PropertyConfigurator.configure(path);
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
		Map<String, Class<?>> ret = new HashMap<String, Class<?>>();
		StringBuilder sb = new StringBuilder();
		for (Method m : cls.getMethods()) {
			String name = m.getName();
			//只查找getXXX函数，换成setXXX函数
			if ((name.length() >= 4) && (name.startsWith("get"))
					&& (!name.equals("getClass"))
					&& (m.getParameterTypes().length <= 0)) {
				ret.put(sb.append('s').append(name, 1, name.length())
						.toString(), m.getReturnType());
				sb.setLength(0);
			}
		}
		return ret;
	}

}
