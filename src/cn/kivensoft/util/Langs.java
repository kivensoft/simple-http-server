package cn.kivensoft.util;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Predicate;

/** 基于jdk8的实用函数集合
 * @author Kiven Lee
 * @version 1.2.0
 */
/**
 * @author kiven
 *
 */
/**
 * @author kiven
 *
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
	
	/** 比较两个日期时间对象是否相等
	 * @param obj 整数对象
	 * @param i 整数基本值
	 * @return
	 */
	public static boolean isEquals(Date d1, Date d2) {
		return d1 == null ? d2 == null
				: d2 == null ? false : d1.getTime() == d2.getTime();
	}
	
	/** 判断参数列表中是否有null的对象
	 * @param args 参数列表
	 * @return 有null返回true，没有返回false
	 */
	public static boolean isAnyNull(Object...args) {
		int count = args.length;
		while (--count >= 0) if (args[count] == null) return true;
		return false;
	}
	
	/** 判断参数列表中是否有不为null的对象
	 * @param args 参数列表
	 * @return 有非null对象返回true，全null返回false
	 */
	public static boolean isAnyNotNull(Object... args) {
		int count = args.length;
		while (--count >= 0) if (args[count] != null) return true;
		return false;
	}
	
	@FunctionalInterface
	public static interface MyConsumer<T> {
		void accept(T t) throws Exception;
	}
	
	@FunctionalInterface
	public static interface MyBiConsumer<T, U> {
		void accept(T t, U u) throws Exception;
	}
	
	/** 双重嵌套的数组循环函数, 减少性能损失
	 * @param args 需要循环的数组
	 * @param value 循环时用到的变量
	 * @param consumer 循环回调函数
	 */
	public static <T> void forEachWithCatch(T[] args, MyConsumer<T> consumer) {
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
	public static <T, R> R forEachWithCatch(T[] args, R value, MyBiConsumer<T, R> consumer) {
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
	public static <T> void forEachWithCatch(Collection<T> args, MyConsumer<T> consumer) {
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
	public static <T, R> R forEachWithCatch(Collection<T> args, R value, MyBiConsumer<T, R> consumer) {
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
				Method m = dstCls.getMethod(mname, method.getReturnType());
				m.invoke(dst, method.invoke(src));
				names.add(fname);
			} catch (Exception e) {
				try {
					Field f2 = dstCls.getField(fname);
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
				Field f2 = dstCls.getField(name);
				f2.set(dst, field.get(src));
			} catch (Exception e) {
				sb.setLength(0);
				String mname = sb.append("set")
					.append(Character.toUpperCase(name.charAt(0)))
					.append(name, 1, name.length()).toString();
				try {
					Method m = dstCls.getMethod(mname, field.getType());
					m.invoke(dst, field.get(src));
				} catch (Exception e2) {}
			}
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
    
    /** 返回第一个回调为true的元素在数组中的索引位置
     * @param array 数组
     * @param start 起始查找位置
     * @param predicate 回调函数
     * @return 找到返回指定索引, 否则返回-1
     */
    public static <T> int indexOfArray(T[] array, int start, Predicate<T> predicate) {
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
    public static <T> T elementAtArray(T[] array, int start, Predicate<T> predicate) {
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
    	return elementAtArray(array, 0, predicate);
    }
    
    /** 查找元素
     * @param collection 要迭代的集合
     * @param predicate 回调函数
     * @return 找到返回该元素, 否则返回false
     */
    public static <T> T find(Collection<T> collection, Predicate<T> predicate) {
    	Iterator<T> iter = collection.iterator();
    	while (iter.hasNext()) {
    		T v = iter.next();
    		if (predicate.test(v)) return v;
    	}
    	return null;
    }

	private static Calendar calendar = null;
	
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
		if (calendar == null) calendar = Calendar.getInstance();
		synchronized (calendar) {
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
		return value.toInstant().atZone(ZoneId.systemDefault())
				.toLocalDateTime();
	}
	
	/** 获取系统的今天，只包含日期部分 */
	public static Date today() {
		return new Date((System.currentTimeMillis() + tzOffset)
				/ msOfDay * msOfDay - tzOffset);
	}
	
	/** 增加年份 */
	public static Date addYears(Date date, int years) {
		if (calendar == null) calendar = Calendar.getInstance();
		synchronized (calendar) {
			calendar.setTime(date);
			if (years != 0) calendar.add(Calendar.YEAR, years);
			return calendar.getTime();
		}
	}

	/** 增加月份 */
	public static Date addMonths(Date date, int months) {
		if (calendar == null) calendar = Calendar.getInstance();
		synchronized (calendar) {
			calendar.setTime(date);
			if (months != 0) calendar.add(Calendar.MONTH, months);
			return calendar.getTime();
		}
	}

	/** 增加天数 */
	public static Date addDays(Date date, int days) {
		return new Date(date.getTime() + msOfDay * days);
	}
	
	/** 增加年月日 */
	public static Date addDate(Date date, int years, int months, int days) {
		if (calendar == null) calendar = Calendar.getInstance();
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
		if (calendar == null) calendar = Calendar.getInstance();
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
		return new Date(date.getTime()
				+ hours * 3600000 + minutes * 60000 + seconds * 1000);
	}
	
	/** 返回一个新的日期变量，值为日期参数的日期部分 */
	public static Date onlyDate(Date date) {
		return new Date((date.getTime()
				+ tzOffset) / msOfDay * msOfDay - tzOffset);
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
