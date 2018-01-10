package cn.kivensoft.util;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.function.Predicate;

/** 基于jdk8的实用函数集合
 * @author Kiven Lee
 * @version 1.2.0
 */
public final class Langs {
	private static long msOfDay = 86400000;
	private static long tzOffset = TimeZone.getDefault().getRawOffset();
	
	/** 比较两个对象是否相等
	 * @param src 源对象
	 * @param dst 目标对象
	 * @return 相等返回true，不等返回false
	 */
	public static <T> boolean isEquals(T src, T dst) {
		return src == null ? dst == null : src.equals(dst);
	}
	
	/** 判断参数列表中是否有null的对象
	 * @param args 参数列表
	 * @return 有null返回true，没有返回false
	 */
	public static boolean isNullAny(Object...args) {
		for(Object obj : args) if (obj == null) return true;
		return false;
	}
	
	/** 判断参数列表中是否有不为null的对象
	 * @param args 参数列表
	 * @return 有非null对象返回true，全null返回false
	 */
	public static boolean isNotNullAny(Object... args) {
		for(Object obj : args) if (obj != null) return true;
		return false;
	}
	
	/** javabean的属性复制，只支持同名同类型属性的复制
	 * @param src 复制源对象
	 * @param dst 复制的目标对象
	 */
	public static <T> T copyProperties(Object src, T dst) {
		if (src == null || dst == null) return null;
		StringBuilder sb = new StringBuilder().append('s');
		Class<?> cls = dst.getClass();
		Method[] ms = src.getClass().getMethods();
		// 双重嵌套循环，减少循环中频繁设置try/catch的性能损失
		for (int i = 0, n = ms.length; i < n; ++i) {
			try {
				for (; i < n; ++i) {
					Method m = ms[i];
					String name = m.getName();
					if (name.length() < 4 || name.equals("getClass")
							|| !name.startsWith("get")
							|| m.getParameterCount() > 0)
						continue;
					sb.setLength(1); // 第一个字符固定为's'
					Method m2 = cls.getMethod(
							sb.append(name, 1, name.length()).toString(),
							m.getReturnType());
					if (m2 != null) m2.invoke(dst, m.invoke(src));
				}
			}
			catch (Exception e) {}
		}
		return dst;
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

	/** 根据数组类型的class创建对应类型的数组
     * @param <T> 目标类型
     * @param clazz
     * @param length 数组长度
     * @return
     */
    @SuppressWarnings("unchecked")
	public static <T> T[] newArrayByArrayClass(Class<T[]> clazz, int length) {
        return (T[]) Array.newInstance(clazz.getComponentType(), length);
    }
     
    /** 根据普通类型的class创建数组
     * @param <T> 目标类型
     * @param clazz
     * @param length 数组长度
     * @return
     */
    @SuppressWarnings("unchecked")
	public static <T> T[] newArrayByClass(Class<T> clazz, int length) {
        return (T[]) Array.newInstance(clazz, length);
    }
    
    public static <T> int indexOfArray(T[] array, int start, Predicate<T> predicate) {
    	for (int i = start, len = array.length; i < len; ++i)
    		if (predicate.test(array[i])) return i;
    	return -1;
    }

    public static <T> T elementAtArray(T[] array, int start, Predicate<T> predicate) {
    	for (int i = 0, len = array.length; i < len; ++i)
    		if (predicate.test(array[i])) return array[i];
    	return null;
    }

	/** 转换为Date类型 */
	public static Date toDate(LocalDate date) {
		return Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()) .toInstant());
	}
	
	/** 转换为Date类型 */
	public static Date toDate(LocalDateTime daettime) {
		return Date.from(daettime.atZone(ZoneId.systemDefault()) .toInstant());
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
		synchronized (calendar) {
			calendar.setTimeZone(TimeZone.getDefault());
			calendar.set(year, month - 1, day, hour, minute, second);
			calendar.set(Calendar.MILLISECOND, millseconds);
			return calendar.getTime();
		}
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
		return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	}
	
	/** 获取系统的今天，只包含日期部分 */
	public static Date today() {
		return new Date((System.currentTimeMillis() + tzOffset)
				/ msOfDay * msOfDay - tzOffset);
	}
	
	/** 增加年份 */
	public static Date addYears(Date date, int years) {
		synchronized (calendar) {
			calendar.setTime(date);
			if (years != 0) calendar.add(Calendar.YEAR, years);
			return calendar.getTime();
		}
	}

	/** 增加月份 */
	public static Date addMonths(Date date, int months) {
		synchronized (calendar) {
			calendar.setTime(date);
			if (months != 0) calendar.add(Calendar.MONTH, months);
			return calendar.getTime();
		}
	}

	/** 增加天数 */
	public static Date addDays(Date date, int days) {
		synchronized (calendar) {
			calendar.setTime(date);
			if (days != 0) calendar.add(Calendar.DAY_OF_MONTH, days);
			return calendar.getTime();
		}
	}
	
	/** 增加年月日 */
	public static Date addDate(Date date, int years, int months, int days) {
		synchronized (calendar) {
			calendar.setTime(date);
			if (years != 0) calendar.add(Calendar.YEAR, years);
			if (months != 0) calendar.add(Calendar.MONTH, months);
			if (days != 0) calendar.add(Calendar.DAY_OF_MONTH, days);
			return calendar.getTime();
		}
	}

	/** 增加年月日时分秒 */
	public static Date addDate(Date date, int years, int months, int days,
			int hours, int minutes, int seconds) {
		synchronized (calendar) {
			calendar.setTime(date);
			if (years != 0) calendar.add(Calendar.YEAR, years);
			if (months != 0) calendar.add(Calendar.MONTH, months);
			if (days != 0) calendar.add(Calendar.DAY_OF_MONTH, days);
			if (hours != 0) calendar.add(Calendar.HOUR, hours);
			if (minutes != 0) calendar.add(Calendar.MINUTE, minutes);
			if (seconds != 0) calendar.add(Calendar.SECOND, seconds);
			return calendar.getTime();
		}
	}

	/** 增加时分秒 */
	public static Date addTime(Date date, int hours, int minutes, int seconds) {
		synchronized (calendar) {
			calendar.setTime(date);
			if (hours != 0) calendar.add(Calendar.HOUR, hours);
			if (minutes != 0) calendar.add(Calendar.MINUTE, minutes);
			if (seconds != 0) calendar.add(Calendar.SECOND, seconds);
			return calendar.getTime();
		}
	}
	
	/** 返回一个新的日期变量，值为日期参数的日期部分 */
	public static Date onlyDate(Date date) {
		return new Date((date.getTime() + tzOffset) / msOfDay * msOfDay - tzOffset);
	}

	/** 设置日期参数的时分秒为0 */
	public static void trimTime(Date date) {
		date.setTime((date.getTime() + tzOffset) / msOfDay * msOfDay - tzOffset);
	}
	
	/** 转换为GMT时间 */
	public static Date gmtTime(Date date) {
		return new Date(date.getTime() + tzOffset);
	}
	
	/** 合并两个int成long */
	public static long mergeTwoInt(int high, int low) {
		return ((long)(high << 32)) | ((long)low); 
	}
	
	/** 获取long的高位int */
	public static int highIntFromLong(long value) {
		return (int)(value >> 32 & 0xFFFFFFFF);
	}
	
	/** 获取long的低位int */
	public static int lowIntForLong(long value) {
		return (int)(value &0xFFFFFFFF);
	}
	
	/** 转换文本内容为对象
	 * @param cls 对象类型
	 * @param value 文本内容
	 * @return 转换后生成的对象，转换失败返回null
	 */
	@SuppressWarnings("unchecked")
	public static <T> T valueOf(Class<T> cls, String value) {
		if (value == null || value.isEmpty())
			return null;
		if (cls == String.class)
			return (T)value;
		try {
			if (cls == Integer.class || cls == Integer.TYPE)
				return (T)Integer.valueOf(value);
			if (cls == Long.class || cls == Long.TYPE)
				return (T)Long.valueOf(value);
			if (cls == Float.class || cls == Float.TYPE)
				return (T)Float.valueOf(value);
			if (cls == Date.class)
				return (T)parseDate(value);
			if (cls == Boolean.class || cls == Boolean.TYPE)
				return (T)Boolean.valueOf(value);
			if (cls == BigDecimal.class)
				return (T)new BigDecimal(value);
			if (cls == BigInteger.class)
				return (T)new BigInteger(value);
			if (cls == LocalDate.class)
				return (T)parseLocalDate(value);
			if (cls == LocalDateTime.class)
				return (T)parseLocalDateTime(value);
			if (cls == LocalTime.class)
				return (T)parseLocalTime(value);
			if (cls == Double.class || cls == Double.TYPE)
				return (T)Double.valueOf(value);
			if (cls == Byte.class || cls == Byte.TYPE)
				return (T)Byte.valueOf(value);
			if (cls == Short.class || cls == Short.TYPE)
				return (T)Short.valueOf(value);
			if (cls == Character.class || cls == Character.TYPE)
				return (T)Character.valueOf(value.charAt(0));
			if (cls.isEnum()) {
				Enum<?>[] vs = (Enum<?>[])cls.getMethod("values").invoke(null);
				try {
					int n = Integer.parseInt(value);
					return (T)vs[n];
				}
				catch (NumberFormatException e) {
					for (int i = 0, n = vs.length; i < n; ++i)
						if (vs[i].name().equals(value)) return (T)vs[i];
				}
			}
		}
		catch (Exception e) {}
		return null;
	}
	
    public static interface SplitFunc { void accept(int index, int value); }

    public static void splitInt(String value, SplitFunc consumer) {
		if (value == null || value.isEmpty()) return;
		int i = 0, len = value.length(), index = 0;
		while (i < len) {
			char c = value.charAt(i++);
			if (c >= '0' && c <= '9') {
				int num = c - 48;
				while (i < len) {
					char ch = value.charAt(i++);
					if (ch < '0' || ch > '9') break;
					//number * 10 = number * 8 + number * 2 = number << 3 + number << 1
					num = (num << 3) + (num << 1) + (ch - 48);
				}
				consumer.accept(index++, num);
			}
			else {
				while (i < len) {
					char ch = value.charAt(i++);
					if (ch >= '0' && ch <= '9') {
						--i;
						break;
					}
				}
			}
		}
	}
	
	/** 解析日期时间字段成 年/月/日/时/分/秒/毫秒 数组 */
	public static int[] splitDate (String value) {
		int[] vs = new int[7];
		splitInt(value, (i, v) -> { if (i < 7) vs[i] = v; });
		return vs;
	}
	
	private static Calendar calendar = Calendar.getInstance();
	public static Date parseDate(String text) {
		if (text == null || text.isEmpty()) return null;
		if (text.indexOf('-') < 1 || text.length() < 5) return null;
		boolean isGmt = text.indexOf('T') > 0;
		int[] vs = splitDate(text);
		Date ret = null;
		synchronized (calendar) {
			if (isGmt) calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
			else calendar.setTimeZone(TimeZone.getDefault());
			calendar.set(vs[0], vs[1] - 1, vs[2], vs[3], vs[4], vs[5]);
			calendar.set(Calendar.MILLISECOND, vs[6]);
			ret = calendar.getTime();
		}
		return ret;
	}
	
	public static LocalDate parseLocalDate(String text) {
		if (text == null || text.isEmpty()) return null;
		if (text.indexOf('-') < 1 || text.length() < 5) return null;
		int[] vs = splitDate(text);
		return LocalDate.of(vs[0], vs[1], vs[2]);
	}

	public static LocalDateTime parseLocalDateTime(String text) {
		if (text == null || text.isEmpty()) return null;
		if (text.indexOf('-') < 1 || text.length() < 5) return null;
		int[] vs = splitDate(text);
		return LocalDateTime.of(vs[0], vs[1], vs[2], vs[3], vs[4], vs[5], vs[6]);
	}

	public static LocalTime parseLocalTime(String text) {
		if (text == null || text.isEmpty()) return null;
		if (text.indexOf(':') < 1 || text.length() < 5) return null;
		int[] vs = splitDate(text);
		return LocalTime.of(vs[0], vs[1], vs[2], vs[3]);
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
