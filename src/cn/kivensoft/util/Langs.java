package cn.kivensoft.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

/** 基于jdk8的实用函数集合
 * @author Kiven Lee
 * @version 1.2.0
 */
public final class Langs {
	// 一天的毫秒数
	public static long MS_OF_DAY = 86400_000;
	// 本地时区偏移值, 毫秒为单位
	public static long LOCAL_ZONE_OFFSET = TimeZone.getDefault().getRawOffset();

	/** 比较两个对象是否相等
	 * @param src 源对象
	 * @param dst 目标对象
	 * @return 相等返回true，不等返回false
	 */
	public static <T> boolean isEquals(T src, T dst) {
		return src == null ? dst == null : src.equals(dst);
	}

	/** 比较整数对象与整数基本值
	 * @param obj 整数对象
	 * @param i 整数基本值
	 * @return
	 */
	public static boolean isEquals(int i, Integer obj) {
		return obj == null ? false : obj.intValue() == i;
	}

	/** 比较整数对象与整数基本值
	 * @param obj 整数对象
	 * @param i 整数基本值
	 * @return
	 */
	public static boolean isEquals(long i, Long obj) {
		return obj == null ? false : obj.longValue() == i;
	}

	/** 获取异常的堆栈详细内容
	 * @param e 异常变量
	 * @return 堆栈详细内容
	 */
	public static String getStackTrace(Exception e) {
		StringWriter sw = new StringWriter();
		try {
			e.printStackTrace(new PrintWriter(sw));
		} catch(Exception ex) {}
		return sw.toString();
	}

	/** 三目运算函数, true返回第一个值, false返回第二个值
	 * @param value true时返回的值
	 * @param def false时返回额值
	 * @param pred lambda, 参数是value, 返回值根据该lambda返回的true/false决定
	 * @return true返回value, false返回def
	 */
	public static int assignIf(int value, int def, IntPredicate pred) {
		return pred.test(value) ? value : def;
	}

	/** 三目运算函数, value不为null返回value, 否则返回def
	 * @param value not null时返回的值
	 * @param def null时返回额值
	 * @return not null返回value, null返回def
	 */
	public static <T> T assignIf(T value, T def) {
		return value != null ? value : def;
	}

	/** 三目运算函数, true返回第一个值, false返回第二个值
	 * @param value true时返回的值
	 * @param def false时返回额值
	 * @param pred lambda, 参数是value, 返回值根据该lambda返回的true/false决定
	 * @return true返回value, false返回def
	 */
	public static <T> T assignIf(T value, T def, Predicate<T> pred) {
		return pred.test(value) ? value : def;
	}

	public static <T> T callIf(T value, Consumer<T> func) {
		if (value != null) func.accept(value);
		return value;
	}

	public static <T> T callIf(T value, Predicate<T> pred, Consumer<T> func) {
		if (pred.test(value)) func.accept(value);
		return value;
	}

	public static <T, R> R callIf(T value, R def, Function<T, R> func) {
		return value != null ? func.apply(value) : def;
	}

	public static <T, R> R callIf(T value, R def, Predicate<T> pred, Function<T, R> func) {
		return pred.test(value) ? func.apply(value) : def;
	}

	/** 生成map字典
	 * @param args key1, value1, key2, value2, ... 形式的参数
	 * @return 字典对象
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> HashMap<K, V> mapOf(Object... args) {
		HashMap<K, V> ret = new HashMap<>(args.length);
		for (int i = 0, imax = args.length - 1; i < imax; i += 2)
			ret.put((K) args[i], (V) args[i + 1]);
		return ret;
	}

	/** 生成列表
	 * @param args 列表项
	 * @return 列表
	 */
	@SafeVarargs
	public static <T> ArrayList<T> listOf(T... args) {
		ArrayList<T> ret = new ArrayList<>(args.length);
		for (int i = 0, imax = args.length; i < imax; ++i)
			ret.add(args[i]);
		return ret;
	}

	/** 生成数组
	 * @param args 数组项
	 * @return 数组
	 */
	@SafeVarargs
	public static <T> T[] arrayOf(T... args) {
		return args;
	}

	/** 生成整数数组
	 * @param args 整数项
	 * @return 整数数组
	 */
	public static int[] arrayOfInt(int... args) {
		return args;
	}

	/** 拷贝字典表中的指定键值
	 * @param dst
	 * @param src
	 * @param args
	 */
	@SafeVarargs
	public static <T1, T2> void transTo(Map<T1, T2> dst, Map<T1, T2> src, T1... args) {
		if (args.length == 0)
			dst.putAll(src);
		else
			for (T1 arg : args)
				dst.put(arg, src.get(arg));
	}

	@FunctionalInterface
	public interface NoExceptionConsumer<T> {
		void accept(T t) throws Exception;
	}

	@FunctionalInterface
	public interface NoExceptionBiConsumer<T, U> {
		void accept(T t, U u) throws Exception;
	}

	/** 双重嵌套的数组循环函数, 减少性能损失
	 * @param args 需要循环的数组
	 * @param value 循环时用到的变量
	 * @param consumer 循环回调函数
	 */
	public static <T> void forEachWithCatch(T[] args, NoExceptionConsumer<T> consumer) {
		// 双重嵌套循环，减少循环中频繁设置try/catch的性能损失
		for (int i = 0, n = args.length; i < n; ++i) {
			try {
				for (; i < n; ++i) consumer.accept(args[i]);
			}
			catch (Exception e) {}
		}
	}

	/** 双重嵌套的数组循环函数, 减少性能损失
	 * @param args 需要循环的数组
	 * @param value 循环时用到的变量
	 * @param consumer 循环回调函数
	 */
	public static <T, R> R forEachWithCatch(T[] args, R value, NoExceptionBiConsumer<T, R> consumer) {
		// 双重嵌套循环，减少循环中频繁设置try/catch的性能损失
		for (int i = 0, n = args.length; i < n; ++i) {
			try {
				for (; i < n; ++i) consumer.accept(args[i], value);
			}
			catch (Exception e) {}
		}
		return value;
	}

	/** 双重嵌套的数组循环函数, 减少性能损失
	 * @param args 需要循环的数组
	 * @param value 循环时用到的变量
	 * @param consumer 循环回调函数
	 */
	public static <T> void forEachWithCatch(Collection<T> args, NoExceptionConsumer<T> consumer) {
		Iterator<T> iter = args.iterator();
		while (iter.hasNext()) {
			try {
				do {
					consumer.accept(iter.next());
				} while (iter.hasNext());
			}
			catch (Exception e) {}
		}
	}

	/** 双重嵌套的数组循环函数, 减少性能损失
	 * @param args 需要循环的数组
	 * @param value 循环时用到的变量
	 * @param consumer 循环回调函数
	 */
	public static <T, R> R forEachWithCatch(Collection<T> args, R value, NoExceptionBiConsumer<T, R> consumer) {
		Iterator<T> iter = args.iterator();
		while (iter.hasNext()) {
			try {
				do {
					consumer.accept(iter.next(), value);
				} while (iter.hasNext());
			}
			catch (Exception e) {}
		}
		return value;
	}

	/** javabean的属性复制，只支持同名同类型属性的复制
	 * @param src 复制源对象
	 * @param dst 复制的目标对象
	 */
	public static <T> T copyProperties(Object src, T dst) {
		if (src == null || dst == null) return dst;

		Set<String> names = new HashSet<>();
		Class<?> dstCls = dst.getClass();
		StringBuilder sb = new StringBuilder();
		Method[] methods = src.getClass().getMethods();
		for (int i = 0, n = methods.length; i < n; ++i) {
			Method method = methods[i];
			String name = method.getName();
			if (name.length() < 4 || name.equals("getClass")
					|| !name.startsWith("get")
					|| method.getParameterCount() > 0)
				continue;
			sb.setLength(0);
			String mname = sb.append('s').append(name, 1, name.length()).toString();
			sb.setLength(0);
			String fname = sb.append(Character.toLowerCase(name.charAt(3)))
					.append(name, 4, name.length()).toString();
			try {
				method.setAccessible(true);
				Method m = dstCls.getMethod(mname, method.getReturnType());
				m.setAccessible(true);
				m.invoke(dst, method.invoke(src));
				names.add(fname);
			} catch (Exception e) {
				try {
					Field f2 = dstCls.getField(fname);
					f2.setAccessible(true);
					f2.set(dst, method.invoke(src));
					names.add(fname);
				} catch (Exception e2) { }
			}
		}

		Field[] fields = src.getClass().getFields();
		for (int i = 0, n = fields.length; i < n; ++i) {
			Field field = fields[i];
			String name = field.getName();
			if (names.contains(name))
				continue;
			try {
				field.setAccessible(true);
				Field f2 = dstCls.getField(name);
				f2.setAccessible(true);
				f2.set(dst, field.get(src));
			} catch (Exception e) {
				sb.setLength(0);
				String mname = sb.append("set")
					.append(Character.toUpperCase(name.charAt(0)))
					.append(name, 1, name.length()).toString();
				try {
					Method m = dstCls.getMethod(mname, field.getType());
					m.setAccessible(true);
					m.invoke(dst, field.get(src));
				} catch (Exception e2) {}
			}
		}

		return dst;
	}

	/** 循环对象的所有字段名/值
	 * @param obj 要循环的对象
	 * @param consumer 回调函数, 每个找到的字段名/值进行回调
	 */
	public static void forEachFields(Object obj, BiConsumer<String, Object> consumer) {
		if (obj == null) return;

		StringBuilder sb = new StringBuilder();

		Field[] fields = obj.getClass().getFields();
		for (int i = 0, n = fields.length; i < n; ++i) {
			Field field = fields[i];
			String name = field.getName();
			try {
				field.setAccessible(true);
				Object value = field.get(obj);
				consumer.accept(name, value);
			} catch (Exception e) { }
		}

		Method[] methods = obj.getClass().getMethods();
		for (int i = 0, n = methods.length; i < n; ++i) {
			Method method = methods[i];
			String name = method.getName();
			if (name.length() < 4 || name.equals("getClass")
					|| !name.startsWith("get")
					|| method.getParameterCount() > 0)
				continue;
			sb.setLength(0);
			String fname = sb.append(Character.toLowerCase(name.charAt(3)))
					.append(name, 4, name.length()).toString();
			try {
				method.setAccessible(true);
				consumer.accept(fname, method.invoke(obj));
			} catch (Exception e) {
			}
		}
	}

	/** 获取对象指定字段名的值
	 * @param obj 对象实例
	 * @param name 字段名
	 * @return
	 */
	public static Object getField(Object obj, String name) {
		Class<?> cls = obj.getClass();
		char first = name.charAt(0);
		if (first >= 'a' && first <= 'z') first -= 0x20;
		String n = "get" + first + name.substring(1);
		try {
			Method m = cls.getMethod(n);
			m.setAccessible(true);
			return m.invoke(obj);
		} catch (Exception e) {
			try {
				Field f = cls.getField(name);
				f.setAccessible(true);
				return f.get(obj);
			} catch (Exception e1) {
				return null;
			}
		}
	}

	/** 设置对象指定字段的值
	 * @param obj 对象实例
	 * @param name 字段名
	 * @param value 值
	 * @return
	 */
	public static boolean setField(Object obj, String name, Object value) {
		Class<?> cls = obj.getClass();
		char first = name.charAt(0);
		if (first >= 'a' && first <= 'z') first -= 0x20;
		String n = "set" + first + name.substring(1);
		try {
			Method m = cls.getMethod(n, cls);
			m.setAccessible(true);
			m.invoke(obj, value);
		} catch (Exception e) {
			try {
				Field f = cls.getField(name);
				f.setAccessible(true);
				f.set(obj, value);
			} catch (Exception e1) {
				return false;
			}
		}
		return true;
	}

	/** 自动流拷贝
	 * @param in 输入流
	 * @param out 输出流
	 * @param autoClose 自动关闭标志
	 * @throws IOException
	 */
	public static void transTo(InputStream in, OutputStream out, boolean autoClose)
			throws IOException {
		byte[] buf = new byte[4096];
		try {
			int count = in.read(buf);
			while (count > 0) {
				out.write(buf, 0, count);
				count = in.read(buf);
			}
		} finally {
			if (autoClose) close(in, out);
		}
	}

	/** 读取文件内容到字节数组
	 * @param filename 文件名
	 * @return 文件内容的字节数组
	 * @throws IOException
	 */
	public static byte[] readFromFile(String filename) throws IOException {
		File f = new File(filename);
		if (!f.exists()) throw new FileNotFoundException();
		int len = (int) f.length();
		byte[] buf = new byte[len];
		FileInputStream fis = new FileInputStream(f);
		try {
			int pos = 0, count = fis.read(buf);
			while (count > 0) {
				pos += count;
				count = fis.read(buf, pos, len - pos);
			}
		} finally {
			close(fis);
		}

		return buf;
	}

	/** 读取流内容到字节数组
	 * @param in 输入流
	 * @param autoClose 自动关闭标志
	 * @return 流内容字节数组
	 * @throws IOException
	 */
	public static byte[] readFromStream(InputStream in, boolean autoClose) throws IOException {
		// 缓冲区长度必须为2的n次幂, 因为下面的计算中使用了位移算法
		final int BUF_SIZE = 4096;
		ArrayList<byte[]> list = new ArrayList<>();
		int readLength = 0, pos = 0;
		byte[] bytes = new byte[BUF_SIZE];
		try {
			while (true) {
				int rlen = in.read(bytes, pos, BUF_SIZE - pos);
				if (rlen == -1) {
					if (pos > 0) list.add(bytes);
					break;
				}
				readLength += rlen;
				pos += rlen;
				if (pos >= BUF_SIZE) {
					list.add(bytes);
					bytes = new byte[BUF_SIZE];
					pos = 0;
				}
			}
		} finally {
			if (autoClose) close(in);
		}

		byte[] ret = new byte[readLength];
		int maxIndex = list.size() - 1;
		// 先把所有读满的缓冲区复制到返回对象
		for (int i = 0; i < maxIndex; ++i)
			System.arraycopy(list.get(i), 0, ret, i << 12, BUF_SIZE);
		// 处理最后一块可能长度不满的缓冲区
		System.arraycopy(list.get(maxIndex), 0, ret,
				maxIndex << 12, readLength & (BUF_SIZE - 1));

		return ret;
	}

	/** 读取文件内容到字符串
	 * @param filename 文件名
	 * @return 读取的字符串
	 * @throws IOException
	 */
	public static String readStringFromFile(String filename) throws IOException {
		FileInputStream fis = new FileInputStream(filename);
		return readStringFromStream(fis, true);
	}

	/** 读取流内容到字符串(UTF8格式)
	 * @param in 输入流
	 * @param autoClose 是否自动关闭
	 * @return 流内容的字符串表示形式
	 * @throws IOException
	 */
	public static String readStringFromStream(InputStream in, boolean autoClose)
			throws IOException {
		char[] buf = new char[4096];
		StringBuilder sb = new StringBuilder();
		try {
			Reader r = new InputStreamReader(in, "UTF8");
			int count = r.read(buf);
			while (count > 0) {
				sb.append(buf, 0, count);
				count = r.read(buf);
			}
		} finally {
			if (autoClose) close(in);
		}
		return sb.toString();
	}

	/** 关闭资源，抛弃异常
	 * @param closeable 实现可关闭接口的对象
	 */
	public static void close(Closeable closeable) {
		if (closeable != null)
			try { closeable.close(); } catch(IOException e) {}
	}

	/** 关闭多个资源，抛弃异常
	 * @param closeables 多个实现可关闭接口的对象
	 */
	public static void close(Closeable... closeables) {
		// 双重嵌套循环，减少循环中频繁设置try/catch的性能损失
		for (int i = 0, len = closeables.length; i < len; ++i) {
			try {
				for (; i < len; ++i) {
					Closeable c = closeables[i];
					if (c != null) c.close();
				}
			}
			catch(IOException e) {}
		}
	}

	/** 采用反射的方式调用对象的close函数关闭资源，抛弃异常
	 * @param args 多个需要关闭的对象
	 */
	public static void close(Object...args) {
		// 双重嵌套循环，减少循环中频繁设置try/catch的性能损失
		for (int i = 0, len = args.length; i < len; ++i) {
			try {
				for (; i < len; ++i) {
					Object obj = args[i];
					if (obj == null) continue;
					Method m = obj.getClass().getMethod("close");
					if (m != null && m.getParameterCount() == 0)
						m.invoke(obj);
				}
			}
			catch(Exception e) {}
		}
	}

    /** 返回第一个回调为true的元素在数组中的索引位置
     * @param array 数组
     * @param start 起始查找位置
     * @param predicate 回调函数
     * @return 找到返回指定索引, 否则返回-1
     */
    public static <T> int indexOf(T[] array, int start, Predicate<T> predicate) {
    	if (array == null || array.length == 0) return -1;
    	for (int i = start, len = array.length; i < len; ++i)
    		if (predicate.test(array[i])) return i;
    	return -1;
    }

    /** 返回表达式为true时的数组所在位置的元素
     * @param array 迭代的数组
     * @param start 起始迭代位置
     * @param predicate 回调函数
     * @return 返回找到的元素, 否则返回null
     */
    public static <T> T elementAt(T[] array, int start, Predicate<T> predicate) {
    	if (array == null || array.length == 0) return null;
    	for (int i = start, len = array.length; i < len; ++i)
    		if (predicate.test(array[i])) return array[i];
    	return null;
    }

    /** 查找元素
     * @param array 进行迭代查找的数组
     * @param predicate 回调函数
     * @return 找到则返回该元素, 否则返回false
     */
    public static <T> T find(T[] array, Predicate<T> predicate) {
    	return elementAt(array, 0, predicate);
    }

    /** 查找元素
     * @param collection 要迭代的集合
     * @param predicate 回调函数
     * @return 找到返回该元素, 否则返回false
     */
    public static <T> T find(Collection<T> collection, Predicate<T> predicate) {
    	if (collection == null || collection.isEmpty()) return null;
    	Iterator<T> iter = collection.iterator();
    	while (iter.hasNext()) {
    		T v = iter.next();
    		if (predicate.test(v)) return v;
    	}
    	return null;
    }

	/** 转换为Date类型 */
	public static Date toDate(LocalDate date) {
		return Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault())
				.toInstant());
	}

	/** 转换为Date类型 */
	public static Date toDate(LocalDateTime daettime) {
		return Date.from(daettime.atZone(ZoneId.systemDefault()).toInstant());
	}

	/** 转换为Date类型 */
	public static Date toDate(LocalDate date, LocalTime time) {
		return toDate(LocalDateTime.of(date, time));
	}

	/** 转换为Date类型 */
	public static Date toDate(int year, int month, int day) {
		return toDate(year, month, day, 0, 0, 0, 0);
	}

	/** 转换为Date类型 */
	public static Date toDate(int year, int month, int day, int hour,
			int minute, int second, int millseconds) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(year, month - 1, day, hour, minute, second);
		calendar.set(Calendar.MILLISECOND, millseconds);
		return calendar.getTime();
	}

	/** 转换为LocalDate类型 */
	public static LocalDate toLocalDate(Date value) {
		return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

	/** 转换为LocalTime类型 */
	public static LocalTime toLocalTime(Date value) {
		return value.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
	}

	/** 转换为LocalDateTime类型 */
	public static LocalDateTime toLocalDateTime(Date value) {
		return value.toInstant().atZone(ZoneId.systemDefault())
				.toLocalDateTime();
	}

	public static LocalDateTime localToUTC(LocalDateTime value) {
		return value.atZone(ZoneId.systemDefault())
				.withZoneSameInstant(ZoneOffset.UTC)
				.toLocalDateTime();
	}

	public static LocalDateTime UTCToLocal(LocalDateTime value) {
		return value.atZone(ZoneOffset.UTC)
				.withZoneSameInstant(ZoneId.systemDefault())
				.toLocalDateTime();
	}

	/** 获取系统的今天，只包含日期部分 */
	public static Date today() {
		return new Date((System.currentTimeMillis() + LOCAL_ZONE_OFFSET)
				/ MS_OF_DAY * MS_OF_DAY - LOCAL_ZONE_OFFSET);
	}

	/** 增加年份 */
	public static Date addYears(Date date, int years) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		if (years != 0) calendar.add(Calendar.YEAR, years);
		return calendar.getTime();
	}

	/** 增加月份 */
	public static Date addMonths(Date date, int months) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		if (months != 0) calendar.add(Calendar.MONTH, months);
		return calendar.getTime();
	}

	/** 增加天数 */
	public static Date addDays(Date date, int days) {
		return new Date(date.getTime() + MS_OF_DAY * days);
	}

	/** 增加年月日 */
	public static Date addDate(Date date, int years, int months, int days) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		if (years != 0) calendar.add(Calendar.YEAR, years);
		if (months != 0) calendar.add(Calendar.MONTH, months);
		if (days != 0) calendar.add(Calendar.DAY_OF_MONTH, days);
		return calendar.getTime();
	}

	/** 增加年月日时分秒 */
	public static Date addDate(Date date, int years, int months, int days,
			int hours, int minutes, int seconds) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		if (years != 0) calendar.add(Calendar.YEAR, years);
		if (months != 0) calendar.add(Calendar.MONTH, months);
		if (days != 0) calendar.add(Calendar.DAY_OF_MONTH, days);
		if (hours != 0) calendar.add(Calendar.HOUR, hours);
		if (minutes != 0) calendar.add(Calendar.MINUTE, minutes);
		if (seconds != 0) calendar.add(Calendar.SECOND, seconds);
		return calendar.getTime();
	}

	/** 增加时分秒 */
	public static Date addTime(Date date, int hours, int minutes, int seconds) {
		return new Date(date.getTime()
				+ hours * 3600000 + minutes * 60000 + seconds * 1000);
	}

	/** 返回一个新的日期变量，值为日期参数的日期部分 */
	public static Date onlyDate(Date date) {
		return new Date((date.getTime()
				+ LOCAL_ZONE_OFFSET) / MS_OF_DAY * MS_OF_DAY - LOCAL_ZONE_OFFSET);
	}

	/** 设置日期参数的时分秒为0 */
	public static Date trimTime(Date date) {
		date.setTime((date.getTime() + LOCAL_ZONE_OFFSET) / MS_OF_DAY * MS_OF_DAY - LOCAL_ZONE_OFFSET);
		return date;
	}

	/** GMT转换为本地时间 */
	public static Date fromGmt(Date date) {
		return new Date(date.getTime() + LOCAL_ZONE_OFFSET);
	}

	/** 本地时间转换为GMT */
	public static Date toGmt(Date date) {
		return new Date(date.getTime() - LOCAL_ZONE_OFFSET);
	}

	/** 合并两个int成long */
	public static long mergeInt(int high, int low) {
		return ((long)high << 32) | ((long)low & 0xFFFFFFFFL);
	}

	/** 获取long的高位int */
	public static int highInt(long value) {
		return (int)(value >> 32 & 0xFFFFFFFFL);
	}

	/** 获取long的低位int */
	public static int lowInt(long value) {
		return (int)(value &0xFFFFFFFFL);
	}

	@FunctionalInterface
	public interface Act {
		void accept();
	}

	@FunctionalInterface
	public interface Act1<T1> {
		void accept(T1 arg);
	}

	@FunctionalInterface
	public interface Act2<T1, T2> {
		void accept(T1 arg1, T2 arg2);
	}

	@FunctionalInterface
	public interface Act3<T1, T2, T3> {
		void accept(T1 arg1, T2 arg2, T3 arg3);
	}

	@FunctionalInterface
	public interface Act4<T1, T2, T3, T4> {
		void accept(T1 arg1, T2 arg2, T3 arg3, T4 arg4);
	}

	@FunctionalInterface
	public interface Func<R> {
		R apply();
	}

	@FunctionalInterface
	public interface Func1<T1, R> {
		R apply(T1 arg);
	}

	@FunctionalInterface
	public interface Func2<T1, T2, R> {
		R apply(T1 arg1, T2 arg2);
	}

	@FunctionalInterface
	public interface Func3<T1, T2, T3, R> {
		R apply(T1 arg1, T2 arg2, T3 arg3);
	}

	@FunctionalInterface
	public interface Func4<T1, T2, T3, T4, R> {
		R apply(T1 arg1, T2 arg2, T3 arg3, T4 arg4);
	}
}
