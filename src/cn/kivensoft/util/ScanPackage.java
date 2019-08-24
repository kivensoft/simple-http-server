package cn.kivensoft.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** 获取指定包下的所有类，自动循环扫描下级目录
 * @author Kiven Lee
 * @version 1.0.0
 */
final public class ScanPackage {
	private static final String CLASS_NAME = ".class";

	/** 返回指定包下面的所有类
	 * @param packageName 包名称
	 * @return 找到的类对象列表
	 */
	public static List<Class<?>> getClasses(String packageName) {
		return getClasses(packageName, true, null);
	}
	
	/** 返回指定包下面的所有类
	 * @param packageName 包名称
	 * @param recursive 是否递归查找
	 * @return 找到的类对象列表
	 */
	public static List<Class<?>> getClasses(String packageName, boolean recursive) {
		return getClasses(packageName, recursive, null);
	}
	
	/** 返回指定包下面的所有类
	 * @param packageName 包名称
	 * @param recursive 是否递归查找
	 * @param predicate 条件过滤表达式，对类名进行条件过滤
	 * @return 找到的类对象列表
	 */
	public static List<Class<?>> getClasses(String packageName, boolean recursive,
			Predicate<String> predicate) {
		// 第一个class类的集合
		List<Class<?>> classes = new ArrayList<Class<?>>();
		// 获取包的名字 并进行替换
		String packageDirName = packageName.replace('.', '/');
		// 定义一个枚举的集合 并进行循环来处理这个目录下的things
		Enumeration<URL> dirs = null;
		try {
			dirs = Thread.currentThread().getContextClassLoader()
					.getResources(packageDirName);
		}
		catch (IOException e) { }

		// 循环迭代下去
		while (dirs.hasMoreElements()) {
			// 获取下一个元素
			URL url = dirs.nextElement();
			// 得到协议的名称
			String protocol = url.getProtocol();
			// 如果是以文件的形式保存在服务器上
			if ("file".equals(protocol)) {
				try {
					// 获取包的物理路径
					String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
					// 以文件的方式扫描整个包下的文件 并添加到集合中
					findByFile(packageName, filePath, recursive, classes, predicate);
				} catch (UnsupportedEncodingException e) { }
			}
			// 如果是jar包文件
			else if ("jar".equals(protocol))
				findByJar(packageName, url, packageDirName, recursive,
						classes, predicate);
		}

		return classes;
	}

	private static <T> void findByFile(String packageName, String packagePath,
			final boolean recursive, List<Class<?>> classes,
			Predicate<String> predicate){

		// 获取此包的目录 建立一个File
		File dir = new File(packagePath);
		// 如果不存在或者 也不是目录就直接返回
		if (!dir.exists() || !dir.isDirectory()) return;

		// 如果存在 就获取包下的所有文件 包括目录
		// 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
		File[] dirfiles = dir.listFiles();
		StringBuilder sb = new StringBuilder();

		// 循环所有文件
		for (int i = 0, n = dirfiles.length; i < n; ++i) {
			File file = dirfiles[i];
			sb.setLength(0);
			sb.append(packageName).append('.');
			// 如果是目录 则继续扫描
			if (recursive && file.isDirectory()) {
				findByFile(sb.append(file.getName()).toString(),
						file.getAbsolutePath(), recursive, classes, predicate);
			}
			else if (file.getName().endsWith(CLASS_NAME)) {
				// 如果是java类文件 去掉后面的.class 只留下类名
				String className = file.getName().substring(0,
						file.getName().length() - CLASS_NAME.length());
				try {
					// 添加到集合中去
					String clsName = sb.append(className).toString();
					if (predicate == null || predicate.test(clsName))
						classes.add(Class.forName(clsName));
				}
				catch(ClassNotFoundException e) { }
			}
		}
	}

	private static void findByJar(String packageName, URL url, String packageDirName,
			boolean recursive, List<Class<?>> classes, Predicate<String> predicate) {
		
		JarFile jar = null;
		try {
			jar = ((JarURLConnection) url.openConnection()).getJarFile();
		}
		catch (IOException e) { }
		
		Enumeration<JarEntry> entries = jar.entries();
		StringBuilder sb = new StringBuilder();
		// 进行循环迭代
		while (entries.hasMoreElements()) {
			// 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
			JarEntry entry = entries.nextElement();
			String name = entry.getName();
			if (name.charAt(0) == '/') name = name.substring(1);
			
			// 如果前半部分和定义的包名不同则忽略
			if (!name.startsWith(packageDirName)) continue;
			
			int idx = name.lastIndexOf('/');
			// 获取包名 把"/"替换成"."
			if (idx != -1)
				packageName = name.substring(0, idx).replace('/', '.');
			
			// 如果不是一个包且不可迭代则忽略
			if (!recursive && idx == -1) continue;
			
			// 如果不是一个.class文件或者是目录则忽略
			if (entry.isDirectory() || !name.endsWith(".class")) continue;
			
			// 去掉后面的".class" 获取真正的类名
			String className = name.substring(packageName.length() + 1,
					name.length() - CLASS_NAME.length());
			sb.setLength(0);
			String clsName = sb.append(packageName).append('.')
					.append(className).toString();
			try {
				// 添加到classes
				if (predicate == null || predicate.test(clsName))
					classes.add(Class.forName(clsName));
			}
			catch (ClassNotFoundException e) { }
		}
	}

}
