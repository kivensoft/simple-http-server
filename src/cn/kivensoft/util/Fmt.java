package cn.kivensoft.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;

/**格式化到动态字符串缓冲区的类，采取缓存方式实现，增强对日期、数组、列表的格式化
 * @author Kiven Lee
 * @version 1.3
 */
/**
 * @author kiven
 *
 */
public final class Fmt implements Appendable, CharSequence {
	private static final String UTF8 = "UTF-8";
	//允许回收的StringBuilder的最大长度，超过该长度的不进行回收
	private static final int DEF_BUF_SIZE = 256;
	//缓冲对象的数量，超过的丢弃
	//private static final int MAX_CACHE_COUNT = 10;
	
	// 全局无锁非阻塞堆栈头部指针
	private static AtomicReference<WeakReference<Fmt>> head = new AtomicReference<>();
	private WeakReference<Fmt> self, next;
	
	// 无锁非阻塞弹出栈顶元素
	public static Fmt pop() {
		WeakReference<Fmt> _old_head, _new_head;
		Fmt _item;
		do {
			_old_head = head.get();
			if (_old_head == null) return null;
			_item = _old_head.get();
			if (_item == null) {
				head.compareAndSet(_old_head, null);
				return null;
			}
			_new_head = _item.next;
		} while (!head.compareAndSet(_old_head, _new_head));
		_item.next = null;
		return _item;
	}
	
	// 无锁非阻塞元素压入栈顶
	public static void push(Fmt value) {
		if (value.next != null) return;
		WeakReference<Fmt> _old_head;
		do {
			_old_head = head.get();
			if (_old_head != null && _old_head.get() != null)
				value.next = _old_head;
		} while (!head.compareAndSet(_old_head, value.self));
	}
	
	private StringBuilder buffer;
	private Calendar calendar;
	private Formatter formatter;
	private String newline;
	
	/** 缺省构造函数 */
	public Fmt() {
		this(DEF_BUF_SIZE);
	}

	/** 构造函数
	 * @param capacity 初始化容量大小
	 */
	public Fmt(int capacity) {
		buffer = new StringBuilder(capacity);
		self = new WeakReference<>(this);
	}


	//静态公共函数-------------------------------------------------------------

	/** 获取缓存中的Fmt实例 */
	public static Fmt get() {
		Fmt f = pop();
		return f != null ? f : new Fmt();
	}
	
	//回收对象
	public void recycle() {
		clear();
		push(this);
	}
	
	/** 以{}为格式化标识符进行快速格式化，类似日志输出
	 * @param format 格式化字符串
	 * @param args 格式化参数
	 * @return
	 */
	public static String fmt(String format, Object... args) {
		return get().format(format, args).release();
	}
	
	/** 以{}为格式化标识符进行快速格式化，类似日志输出
	 * @param format 格式化字符串
	 * @param arg1 格式化参数1
	 * @return
	 */
	public static String fmt(String format, Object arg1) {
		return get().format(format, arg1, null, null).release();
	}
	
	/** 以{}为格式化标识符进行快速格式化，类似日志输出
	 * @param format 格式化字符串
	 * @param arg1 格式化参数1
	 * @param arg2 格式化参数2
	 * @return
	 */
	public static String fmt(String format, Object arg1, Object arg2) {
		return get().format(format, arg1, arg2, null).release();
	}
	
	/** 以{}为格式化标识符进行快速格式化，类似日志输出
	 * @param format 格式化字符串
	 * @param arg1 格式化参数1
	 * @param arg2 格式化参数2
	 * @param arg3 格式化参数3
	 * @return
	 */
	public static String fmt(String format, Object arg1, Object arg2, Object arg3) {
		return get().format(format, arg1, arg2, arg3).release();
	}
	
	/** 以{}为格式化标识符进行快速格式化，类似日志输出
	 * @param format 格式化字符串
	 * @param func 返回格式化参数的lambda表达式
	 * @return
	 */
	public static String fmt(String format, IntFunction<Object> func) {
		return get().format(format, func).release();
	}
	
	/** C样式的格式化输出
	 * @param format C样式的格式化字符串
	 * @param args 格式化参数
	 * @return
	 */
	public static String pf(String format, Object... args) {
		return get().printf(format, args).release();
	}
	
	/** 输出到控制台
	 * @param format 格式化字符串
	 * @param args 格式化参数
	 */
	public static void pl(String format, Object... args) {
		get().format(format, args).toStream(System.out);;
	}

	/** 输出到控制台
	 * @param format 格式化字符串
	 * @param func 返回格式化参数的lambda表达式
	 */
	public static void pl(String format, IntFunction<Object> func) {
		get().format(format, func).toStream(System.out);;
	}
	
	/** 输出到流
	 * @param stream 指定输出的流
	 * @param format 格式化字符串
	 * @param args 格式化参数列表
	 */
	public static void out(PrintStream stream, String format, Object... args) {
		get().format(format, args).toStream(stream);
	}

	/** 输出到流
	 * @param stream 指定输出的流
	 * @param format 格式化字符串
	 * @param args 格式化参数列表
	 */
	public static void out(OutputStream stream, String format, Object... args) {
		get().format(format, args).toStream(stream);
	}

	/** 以{}为格式化标识符进行快速格式化，类似日志输出
	 * @param format 格式化字符串
	 * @param args 格式化参数
	 * @return
	 */
	public static String fmtJson(String format, Object... args) {
		return get().formatJson(format, args).release();
	}
	
	/** 以{}为格式化标识符进行快速格式化，类似日志输出
	 * @param format 格式化字符串
	 * @param func 返回格式化参数的lambda表达式
	 * @return
	 */
	public static String fmtJson(String format, IntFunction<Object> func) {
		return get().formatJson(format, func).release();
	}
	
	/** 返回json化后的字符串，用缓冲区对象进行
	 * @param value 要json化的参数
	 * @return
	 */
	public static String toJson(Object value) {
		return get().appendJson(value).release();
	}
	
	/** 连接2个字符串成1个 */
	public static String concat(String arg1, String arg2) {
		return get().append(arg1).append(arg2).release();
	}

	/** 连接3个字符串成1个 */
	public static String concat(String arg1, String arg2, String arg3) {
		return get().append(arg1).append(arg2).append(arg3).release();
	}
	
	/** 连接多个字符串成1个 */
	public static String concat(String... args) {
		Fmt f = get();
		for (int i = 0, n = args.length; i < n; ++i)
			f.buffer.append(args[i]);
		return f.release();
	}
	
	/** 连接2个对象成1个 */
	public static String concat(Object arg1, Object arg2) {
		return get().append(arg1).append(arg2).release();
	}

	/** 连接3个对象串成1个 */
	public static String concat(Object arg1, Object arg2, Object arg3) {
		return get().append(arg1).append(arg2).append(arg3).release();
	}
	
	/** 连接多个对象成1个 */
	public static String concat(Object... args) {
		Fmt f = get();
		for (int i = 0, n = args.length; i < n; ++i)
			f.buffer.append(args[i]);
		return f.release();
	}
	
	/** 生成指定重复数量的字符串
	 * @param c 指定重复的字符
	 * @param count 重复数量
	 * @return 字符串结果
	 */
	public static String rep(char c, int count) {
		return Fmt.get().repeat(c, count).release();
	}
	
	/** 生成指定重复次数的字符串
	 * @param text 指定要重复的字符串
	 * @param count 重复数量
	 * @return 生成的字符串
	 */
	public static String rep(String text, int count) {
		return get().repeat(text, count, '\0', '\0').release();
	}
	
	/** 生成指定重复次数的字符串
	 * @param text 指定要重复的字符串
	 * @param count 重复数量
	 * @param delimiter 重复分隔符
	 * @return 生成的字符串
	 */
	public static String rep(String text, int count, char delimiter) {
		return get().repeat(text, count, delimiter, '\0').release();
	}
	
	/** 生成指定重复次数的字符串
	 * @param text 指定要重复的字符串
	 * @param count 重复数量
	 * @param delimiter1 重复前缀分隔符
	 * @param delimiter2 重复后缀分隔符
	 * @return 生成的字符串
	 */
	public static String rep(String text, int count, char delimiter1, char delimiter2) {
		return get().repeat(text, count, delimiter1, delimiter2).release();
	}
	
	/** 返回格式化后的字符串，用缓冲区对象进行
	 * @param array 要格式化的数组
	 * @param delimiter 分隔符
	 * @param func lamdba表达式,参数是数组的每个对象,返回Object
	 * @return
	 */
	public static <T> String join(T[] array, String delimiter, Function<T, Object> func) {
		return get().append(array, delimiter, null, null, func).release();
	}

	/** 返回格式化后的字符串，用缓冲区对象进行
	 * @param array 要格式化的数组
	 * @param delimiter 分隔符
	 * @return
	 */
	public static <T> String join(T[] array, String delimiter) {
		return get().append(array, delimiter, null, null, null).release();
	}

	/** 返回格式化后的字符串
	 * @param iterable 要格式化的参数
	 * @param delimiter 分隔符
	 * @param func lamdba表达式,参数是数组的每个对象,返回Object
	 * @return
	 */
	public static <T> String join(Iterable<T> iterable, String delimiter, Function<T, Object> func) {
		return get().append(iterable, delimiter, null, null, func).release();
	}

	/** 返回格式化后的字符串
	 * @param iterable 要格式化的参数
	 * @param delimiter 分隔符
	 * @return
	 */
	public static <T> String join(Iterable<T> iterable, String delimiter) {
		return get().append(iterable, delimiter, null, null, null).release();
	}

	/** 转成16进制字符串
	 * @param value 要转换的字节数组
	 * @return
	 */
	public static String toHex(final byte[] value) {
		return get().appendHex(value).release();
	}
	
	/** 转成16进制字符串
	 * @param value 要转换的字节数组
	 * @param delimiter 分隔符
	 * @return
	 */
	public static String toHex(final byte[] value, final char delimiter) {
		return get().appendHex(value, delimiter).release();
	}
	
	/** 转成16进制字符串 */
	public static String toHex(final int value) {
		return get().appendHex(value).release();
	}
	
	/** 转成16进制字符串 */
	public static String toHex(final int... args) {
		return get().appendHex(args).release();
	}	
	
	/** 转成16进制字符串 */
	public static String toHex(final char delimiter, final int... args) {
		return get().appendHex(delimiter, args).release();
	}
	
	/** 转成16进制字符串 */
	public static String toHex(final long value) {
		return get().appendHex(value).release();
	}
	
	/** 转成16进制字符串 */
	public static String toHex(final long... args) {
		return get().appendHex(args).release();
	}
	
	/** 转成16进制字符串 */
	public static String toHex(final char delimiter, final long... args) {
		return get().appendHex(delimiter, args).release();
	}
	
	/** 转成base64编码
	 * @param value 要编码的字节数组
	 * @param lineSeparator 是否按76个字符分行
	 * @return
	 */
	public static String toBase64(final byte[] value, boolean lineSeparator) {
		return get().appendBase64(value, lineSeparator).release();
	}
	
	/** json化字符串并输出到控制台
	 * @param fmt 格式化样式
	 * @param args 格式化参数
	 */
	public static void printJson(String fmt, Object... args) {
		System.out.println(Fmt.fmtJson(fmt, args));
	}

	@Override
	public String toString() {
		return buffer.toString();
	}

	/** 将字符串内容转成UTF-8字节数组返回
	 * @return 返回的字节数组
	 */
	public byte[] toBytes() {
		return toBytes(0, buffer.length());
	}
	
	/** 将字符串内容转成UTF-8字节数组返回
	 * @param start 起始位置
	 * @param end 结束位置
	 * @return 返回的字节数组
	 */
	public byte[] toBytes(int start, int end) {
		try {
			byte[] bytes = buffer.substring(start, end).getBytes(UTF8);
			recycle();
			return bytes;
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	
	/** 输出到标准输出流 */
	public void toStream() {
		toStream(System.out);
	}
	
	/** 输出到流
	 * @param stream 输出的目标流
	 */
	public void toStream(PrintStream stream) {
		stream.append(buffer);
		recycle();
	}
	
	/** 以UTF-8编码格式输出到流
	 * @param stream 输出的目标流
	 */
	public void toStream(OutputStream stream) {
		toStream(stream, UTF8);
	}

	/** 输出到流
	 * @param stream 输出的目标流
	 * @param charsetName 字符串编码
	 */
	public void toStream(OutputStream stream, String charsetName) {
		try {
			stream.write(release().getBytes(charsetName));
		} catch (IOException e) { }
	}

	/** 回收对象，返回对象生成的字符串 */
	public String release() {
		String ret = toString();
		recycle();
		return ret;
	}
	
	/** 回收对象，返回对象生成的字符串 */
	public String release(int start) {
		return release(start, buffer.length());
	}
	
	/** 回收对象，返回对象生成的字符串 */
	public String release(int start, int end) {
		String ret = buffer.substring(start, end);
		recycle();
		return ret;
	}
	
	/** 清空对象内容 */
	public void clear() {
		buffer.setLength(0);
	}

	/** 使用formatter进行格式化，类似传统C语言格式化方式 */
	public Fmt printf(String format, Object... args) {
		if (formatter == null) formatter = new Formatter(buffer);
		formatter.format(format, args);
		return this;
	}

	/** 使用{}作为格式化参数进行格式化 */
	public Fmt format(String format, Object...args) {
		if (args.length == 0) buffer.append(format);
		else
			for (int i = -1, pos = nextPlaceHolder(format, 0);
					pos >= 0; pos = nextPlaceHolder(format, pos)) 
				append(args[++i]);
		return this;
	}
	
	/** 使用{}作为格式化参数进行格式化 
	 * @param format 字符串格式模板
	 * @param arg1 格式化参数1
	 * @return
	 */
	public Fmt format(String format, Object arg1) {
		return format(format, arg1, null, null);
	}

	/** 使用{}作为格式化参数进行格式化 
	 * @param format 字符串格式模板
	 * @param arg1 格式化参数1
	 * @param arg2 格式化参数2
	 * @return
	 */
	public Fmt format(String format, Object arg1, Object arg2) {
		return format(format, arg1, arg2, null);
	}
	
	/** 使用{}作为格式化参数进行格式化 
	 * @param format 字符串格式模板
	 * @param arg1 格式化参数1
	 * @param arg2 格式化参数2
	 * @param arg3 格式化参数3
	 * @return
	 */
	public Fmt format(String format, Object arg1, Object arg2, Object arg3) {
		for (int i = -1, pos = nextPlaceHolder(format, 0);
				pos >= 0; pos = nextPlaceHolder(format, pos)) 
			switch (++i) {
				case 0: append(arg1); break;
				case 1: append(arg2); break;
				case 2: append(arg3); break;
			}
		return this;
	}
	
	/** 使用{}作为格式化参数进行格式化 */
	public Fmt format(String format, IntFunction<Object> func) {
		for (int i = -1, pos = nextPlaceHolder(format, 0);
				pos >= 0; pos = nextPlaceHolder(format, pos)) 
			append(func.apply(++i));
		return this;
	}

	/** 使用{}作为格式化参数进行格式化 */
	public Fmt formatJson(String format, Object... args) {
		for (int i = -1, pos = nextPlaceHolder(format, 0);
				pos >= 0; pos = nextPlaceHolder(format, pos)) 
			appendJson(args[++i]);
		return this;
	}

	/** 使用{}作为格式化参数进行格式化 */
	public Fmt formatJson(String format, IntFunction<Object> func) {
		for (int i = -1, pos = nextPlaceHolder(format, 0);
				pos >= 0; pos = nextPlaceHolder(format, pos)) 
			appendJson(func.apply(++i));
		return this;
	}

	protected int nextPlaceHolder(String format, int start) {
		int len = format.length();
		while (start < len) {
			int idx = format.indexOf('{', start);
			if (idx < 0) {
				buffer.append(format, start, len);
				break;
			}
			else if (idx > 0 && format.charAt(idx - 1) == '\\') {
				buffer.append(format, start, idx - 1).append('{');
				start = idx + 1;
			}
			else if (idx < len - 1 && format.charAt(idx + 1) == '}') {
				buffer.append(format, start, idx);
				return idx + 2;
			}
			else {
				buffer.append(format, start, idx).append('{');
				start = idx + 1;
			}
		}
		return -1;
	}
	
	/** 获取平台相关的回车换行符
	 * @return 回车换行符
	 */
	public String NL() {
		if (newline == null) newline = System.getProperty("line.separator");
		return newline;
	}
	
	/** 添加回车换行,与系统平台相关 */
	public Fmt nl() {
		return append(NL());
	}

	/** 对象内容追加进缓冲区, 函数自动判断大部分系统自带类型进行追加
	 * @param obj 要追加内容的对象实例
	 * @return
	 */
	public Fmt append(Object obj) {
		if (obj == null) {
			appendNull();
			return this;
		}
		Class<?> cls = obj.getClass();
		if (cls == Integer.class)
			buffer.append(((Integer)obj).intValue());
		else if (cls == String.class)
			buffer.append((String)obj);
		else if (cls == Date.class)
			appendDateTime((Date)obj);
		else if (cls == Long.class)
			buffer.append(((Long)obj).longValue());
		else if (cls == LocalDateTime.class)
			append((LocalDateTime)obj);
		else if (cls == LocalDate.class)
			append((LocalDate)obj);
		else if (cls == LocalTime.class)
			append((LocalTime)obj);
		else if (cls == Float.class)
			buffer.append(((Float)obj).floatValue());
		else if (cls == Double.class)
			buffer.append(((Double)obj).doubleValue());
		else if (cls == Character.class)
			buffer.append(((Character)obj).charValue());
		else if (cls == Short.class)
			buffer.append(((Short)obj).shortValue());
		else if (cls == Byte.class)
			buffer.append(((Byte)obj).byteValue());
		else if (cls == Boolean.class)
			buffer.append(((Boolean)obj).booleanValue());
		else if (Iterable.class.isAssignableFrom(cls))
			append((Iterable<?>)obj, ",");
		else if (obj.getClass().isArray()) {
			if (cls.isArray() && cls.getComponentType() == char.class)
				buffer.append((char[])obj);
			else if (obj.getClass().getComponentType().isPrimitive())
				appendPrimitiveArray(obj, ",");
			else append((Object[])obj, ",");
		}
		else if (CharSequence.class.isAssignableFrom(cls))
			buffer.append((CharSequence)obj);
		else if (Stream.class.isAssignableFrom(cls))
			append((Stream<?>)obj, ",");
		else if (cls.isEnum())
			buffer.append(((Enum<?>)obj).name());
		else if (Calendar.class.isAssignableFrom(cls))
			append((Calendar)obj);
		else
			buffer.append(obj);
		return this;
	}
	
	/** 追加一个整数值 */
	public Fmt append(int value) {
		buffer.append(value); return this;
	}

	/** 追加一个长整数值 */
	public Fmt append(long value) {
		buffer.append(value); return this;
	}

	/** 追加一个值 */
	public Fmt append(boolean value) {
		buffer.append(value); return this;
	}

	/** 追加一个值 */
	public Fmt append(float value) {
		buffer.append(value); return this;
	}

	/** 追加一个值 */
	public Fmt append(double value) {
		buffer.append(value); return this;
	}

	@Override
	public Fmt append(char c) {
		buffer.append(c); return this;
	}

	/** 追加一个值 */
	public Fmt append(char[] str) {
		buffer.append(str); return this;
	}

	/** 追加一个值 */
	public Fmt append(char str[], int offset, int len) {
		buffer.append(str, offset, len); return this;
	}

	/** 格式化字节流, 以字符串方式写入
	 * @param bytes 字节流
	 * @return
	 */
	public Fmt appendBytes(byte[] bytes) {
		return appendBytes(bytes, 0, bytes.length);
	}

	/** 格式化字节流, 以字符串方式写入
	 * @param bytes 字节流
	 * @param offset 起始偏移
	 * @param len 长度
	 * @return
	 */
	public Fmt appendBytes(byte[] bytes, int offset, int len) {
		return appendBytes(bytes, offset, len, UTF8);
	}

	/** 格式化字节流, 以字符串方式写入
	 * @param bytes 字节流
	 * @param offset 起始偏移
	 * @param len 长度
	 * @param charsetName 编码名称
	 * @return
	 */
	public Fmt appendBytes(byte[] bytes, int offset, int len, String charsetName) {
		try {
			buffer.append(new String(bytes, offset, len, charsetName));
		} catch (UnsupportedEncodingException e) {}
		return this;
	}

	/** 格式化日期 */
	public Fmt append(Date date) {
		//format("%tF %<tT", date); //输出格式 yyyy-MM-dd HH:mm:SS %tF %<tT
		return appendDateTime(date);
	}

	private Calendar getCalendar(Date date) {
		if(calendar == null) calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar;
	}
	
	/** 格式化日期 */
	public Fmt appendDate(Date date) {
		//format("%tF", date); //输出格式 yyyy-MM-dd
		return appendDate(getCalendar(date));
	}

	/** 格式化日期 */
	public Fmt appendTime(Date date) {
		//format("%tT", date); //输出格式 HH:mm:SS
		return appendTime(getCalendar(date));
	}

	/** 格式化日期 */
	public Fmt appendDateTime(Date date) {
		//format("%tF %<tT", date); //输出格式 yyyy-MM-dd HH:mm:SS
		return append(getCalendar(date));
	}
	
	/** 格式化日期 */
	public Fmt append(Calendar calendar) {
		return appendDate(calendar).append(' ').appendTime(calendar);
	}
	
	/** 格式化年月日
	 * @param year 年, 0-9999
	 * @param month 月, 1-12
	 * @param day 日, 1-31
	 * @return
	 */
	public Fmt appendDate(int year, int month, int day) {
		buffer.append(year).append('-');
		if (month < 10) buffer.append('0');
		buffer.append(month).append('-');
		if (day < 10) buffer.append('0');
		buffer.append(day);
		return this;
	}
	
	/** 格式化日期 */
	public Fmt appendDate(Calendar calendar) {
		return appendDate(calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH) + 1,
				calendar.get(Calendar.DATE));
	}
	
	/** 格式化时分秒
	 * @param hour 24小时, 0-23
	 * @param minute 分钟, 0-59
	 * @param second 秒, 0-59
	 * @return
	 */
	public Fmt appendTime(int hour, int minute, int second) {
		if (hour < 10) buffer.append('0');
		buffer.append(hour).append(':');
		if (minute < 10) buffer.append('0');
		buffer.append(minute).append(':');
		if (second < 10) buffer.append('0');
		buffer.append(second);
		return this;
	}

	/** 格式化Calendar对象 */
	public Fmt appendTime(Calendar calendar) {
		return appendTime(calendar.get(Calendar.HOUR_OF_DAY),
				calendar.get(Calendar.MINUTE),
				calendar.get(Calendar.SECOND));
	}
	
	/** 格式化LocalDate对象 */
	public Fmt append(LocalDate date) {
		return appendDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
	}
	
	/** 格式化LocalTime对象 */
	public Fmt append(LocalTime time) {
		return appendTime(time.getHour(), time.getMinute(), time.getSecond());
	}
	
	/** 格式化LocalDatetime对象 */
	public Fmt append(LocalDateTime datetime) {
		return append(datetime.toLocalDate()).append(' ').append(datetime.toLocalTime());
	}
	
	/** 格式化数组
	 * @param value 数组
	 * @param delimiter 分隔符
	 * @param prefix 格式化前缀
	 * @param suffix 格式化后缀
	 * @param func lambda表达式，为null时使用数组本身值
	 * @return
	 */
	private Fmt appendPrimitiveArray(Object obj, String delimiter) {
		if (Array.getLength(obj) > 0) append(Array.get(obj, 0));
		for (int i = 0, n = Array.getLength(obj); i < n; ++i)
			append(delimiter).append(Array.get(obj, i));
		return this;
	}

	/** 格式化数组
	 * @param value 数组
	 * @param delimiter 分隔符
	 * @return
	 */
	public <T> Fmt append(T[] value, String delimiter) {
		return append(value, delimiter, null, null, null);
	}
	
	/** 格式化数组
	 * @param value 数组
	 * @param delimiter 分隔符
	 * @param prefix 格式化前缀
	 * @param suffix 格式化后缀
	 * @return
	 */
	public <T> Fmt append(T[] value, String delimiter, String prefix, String suffix) {
		return append(value, delimiter, null, null, null);
	}
	
	/** 格式化数组
	 * @param value 数组
	 * @param delimiter 分隔符
	 * @param func lambda表达式，为null时使用数组本身值
	 * @return
	 */
	public <T> Fmt append(T[] value, String delimiter, Function<T, Object> func) {
		return append(value, delimiter, null, null, func);
	}
	
	/** 格式化数组
	 * @param value 数组
	 * @param delimiter 分隔符
	 * @param prefix 格式化前缀
	 * @param suffix 格式化后缀
	 * @param func lambda表达式，为null时使用数组本身值
	 * @return
	 */
	public <T> Fmt append(T[] value, String delimiter, String prefix,
			String suffix, Function<T, Object> func) {
		if (prefix != null) buffer.append(prefix);
		if (value == null) appendNull();
		else {
			if (value.length > 0)
				append(func == null ? value[0] : func.apply(value[0]));
			for (int i = 1, n = value.length; i < n; ++i)
				append(delimiter).append(func == null ? value[i] : func.apply(value[i]));
		}
		if (suffix != null) buffer.append(suffix);
		return this;
	}
	
	/** 格式化可迭代对象
	 * @param value 可迭代对象
	 * @param delimiter 字符串分隔符
	 * @return
	 */
	public <T> Fmt append(Iterable<T> value, String delimiter) {
		return append(value, delimiter, null, null, null);
	}
	
	/** 格式化可迭代对象
	 * @param value 可迭代对象
	 * @param delimiter 字符串分隔符
	 * @param prefix 前缀字符串
	 * @param suffix 后缀字符串
	 * @return
	 */
	public <T> Fmt append(Iterable<T> value, String delimiter, String prefix, String suffix) {
		return append(value, delimiter, prefix, suffix, null);
	}
	
	/** 格式化可迭代对象
	 * @param value 可迭代对象
	 * @param delimiter 字符串分隔符
	 * @param func 每个可迭代项的回调处理函数
	 * @return
	 */
	public <T> Fmt append(Iterable<T> value, String delimiter, Function<T, Object> func) {
		return append(value, delimiter, null, null, func);
	}
	
	/** 格式化列表
	 * @param value 列表
	 * @param delimiter 分隔符
	 * @param prefix 前缀字符串
	 * @param suffix 后缀字符串
	 * @param func lambda表达式，为null时使用列表本身值
	 * @return this
	 */
	public <T> Fmt append(Iterable<T> value, String delimiter, String prefix,
			String suffix, Function<T, Object> func) {
		if (prefix != null) buffer.append(prefix);
		if (value == null) appendNull();
		else {
			Iterator<T> iter = value.iterator();
			if (iter.hasNext())
				append(func == null ? iter.next() : func.apply(iter.next()));
			while(iter.hasNext())
				append(delimiter).append(func == null ? iter.next() : func.apply(iter.next()));
		}
		if (suffix != null) buffer.append(suffix);
		return this;
	}
	
	/** 格式化流
	 * @param stream java8的流
	 * @param delimiter 分隔符
	 * @return
	 */
	public <T> Fmt append(Stream<T> stream, String delimiter) {
		return append(stream, delimiter, null, null, null);
	}
	
	/** 格式化流
	 * @param stream java8的流
	 * @param delimiter 分隔符
	 * @param prefix 前缀字符串
	 * @param suffix 后缀字符串
	 * @return
	 */
	public <T> Fmt append(Stream<T> stream, String delimiter, String prefix, String suffix) {
		return append(stream, delimiter, prefix, suffix, null);
	}
	
	/** 格式化流
	 * @param stream java8的流
	 * @param delimiter 分隔符
	 * @param func 每个流元素的回调处理函数
	 * @return
	 */
	public <T> Fmt append(Stream<T> stream, String delimiter, Function<T, Object> func) {
		return append(stream, delimiter, null, null, func);
	}
	
	/** 格式化流元素
	 * @param stream 流
	 * @param delimiter 分隔符
	 * @param prefix 前缀
	 * @param suffix 后缀
	 * @param func 格式化lambda表达式
	 * @return
	 */
	public <T> Fmt append(Stream<T> stream, String delimiter, String prefix,
			String suffix, Function<T, Object> func) {
		if (prefix != null) buffer.append(prefix);
		boolean[] first = {true};
		stream.forEach(v -> {
			if (first[0]) first[0] = false;
			else buffer.append(delimiter);
			append(func == null ? v : func.apply(v));
		});
		if (suffix != null) buffer.append(suffix);
		return this;
	}
	
	private void appendNull() {
		buffer.append('n').append('u').append('l').append('l');
	}
	
	private final static int [] sizeTable = { 9, 99, 999, 9999, 99999, 999999, 9999999,
			99999999, 999999999, Integer.MAX_VALUE };
	
	/** 追加整数，不足前面补0
	 * @param value 要追加的整数
	 * @param width 宽度，不足前面补0
	 * @return
	 */
	public Fmt appendInt(int value, int width) {
		int size = 0;
		while (value > sizeTable[size]) ++size;
		int count = width - size - 1;
		while (count-- > 0) buffer.append('0');
		buffer.append(value);
		return this;
	}
	
	/** 追加字符串，不足前面补空格
	 * @param text 要追加的文本
	 * @param width 宽度，不足前面补空格
	 * @return
	 */
	public Fmt append(String text, int width) {
		return append(text, width, ' ');
	}
	
	/** 追加字符串，不足前面补空格
	 * @param text 要追加的文本
	 * @param width 宽度，不足前面补前缀字符
	 * @param prefix 前缀字符
	 * @return
	 */
	public Fmt append(String text, int width, char prefix) {
		if (text == null) return this;
		int count = width - text.length();
		while (count-- > 0) buffer.append(prefix);
		buffer.append(text);
		return this;
	}
	
	/** 生成指定重复次数的字符串
	 * @param text 需要重复的文本
	 * @param count 重复次数
	 * @return
	 */
	public Fmt repeat(String text, int count) {
		return repeat(text, count, '\0', '\0');
	}
	
	/** 生成指定重复次数的字符串
	 * @param text 需要重复的文本
	 * @param count 重复次数
	 * @param delimiter 分隔符
	 * @return
	 */
	public Fmt repeat(String text, int count, char delimiter) {
		return repeat(text, count, delimiter, '\0');
	}
	
	/** 生成指定重复次数的字符串
	 * @param text 需要重复的文本
	 * @param count 重复次数
	 * @param delimiter1 分隔符1
	 * @param delimiter2 分隔符2
	 * @return
	 */
	public Fmt repeat(String text, int count, char delimiter1, char delimiter2) {
		if (count-- > 0) buffer.append(text);
		if (delimiter1 != '\0' && delimiter2 != '\0')
			while (count-- > 0) buffer.append(delimiter1).append(delimiter2).append(text);
		else if (delimiter1 != '\0')
			while (count-- > '\0') buffer.append(delimiter1).append(text);
		else if (delimiter2 != '\0')
			while (count-- > 0) buffer.append(delimiter2).append(text);
		else
			while (count-- > 0) buffer.append(text);
		return this;
	}
	
	/** 生成指定重复次数的字符串
	 * @param text 需要重复的文本
	 * @param count 重复次数
	 * @param delimiter 分隔符
	 * @return
	 */
	public Fmt repeat(String text, int count, String delimiter) {
		if (count-- > 0) buffer.append(text);
		while (count-- > 0) buffer.append(delimiter).append(text);
		return this;
	}
	
	/** 生成指定重复次数的字符串
	 * @param c 需要重复的字符
	 * @param count
	 * @return
	 */
	public Fmt repeat(char c, int count) {
		while(--count >= 0) buffer.append(c);
		return this;
	}
	
	/** 将对象以json格式增加
	 * @param value 要格式化的对象
	 * @return
	 */
	public Fmt appendJson(Object value) {
		if (value == null) appendNull();
		else if (value instanceof CharSequence)
			appendJavascriptString((CharSequence)value);
		else if (value instanceof Integer)
			buffer.append(((Integer)value).intValue());
		else if (value instanceof Long)
			buffer.append(((Long)value).longValue());
		else if (value instanceof Float)
			buffer.append(((Float)value).floatValue());
		else if (value instanceof Number)
			buffer.append(((Number)value).toString());
		else if (value instanceof Date)
			append('"').appendDateTime((Date)value).append('"');
		else if (value instanceof Calendar)
			append('"').append((Calendar)value).append('"');
		else if (value instanceof LocalDateTime)
			append('"').append((LocalDateTime)value).append('"');
		else if (value instanceof LocalDate)
			append('"').append((LocalDate)value).append('"');
		else if (value instanceof LocalTime)
			append('"').append((LocalTime)value).append('"');
		else if (value instanceof Iterable<?>)
			iterableToJson((Iterable<?>)value);
		else if (value.getClass().isArray())
			arrayToJson(value);
		else if (value instanceof Map<?, ?>)
			mapToJson((Map<?, ?>)value);
		else if (value instanceof Boolean)
			buffer.append(((Boolean)value).booleanValue());
		else if (value instanceof Character)
			charToJson((Character)value);
		else if (value instanceof Enum<?>)
			appendJavascriptString(value.toString());
		else 
			objectToJson(value);

		return this;
	}

	private void objectToJson(Object value) {
		buffer.append('{');
		Class<?> cls = value.getClass();
		Method[] ms = cls.getMethods();
		boolean first = false;
		for (int i = 0, n = ms.length; i < n; ++i) {
			Method m = ms[i];
			String msn = m.getName();
			if (msn.length() < 4
					|| !msn.startsWith("get")
					|| msn.charAt(3) == '_'
					|| msn.equals("getClass")
					|| m.getParameterCount() > 0) {
				continue;
			}
			try {
				Object obj = m.invoke(value);
				if (obj == null) continue;
				if (!first) first = true;
				else buffer.append(',').append(' ');
				buffer.append('"');
				char c = msn.charAt(3);
				buffer.append((c >= 'A' && c <= 'Z') ? (char)(c + 0x20) : c);
				if (msn.length() > 4) buffer.append(msn, 4, msn.length());
				buffer.append('"').append(':').append(' ');
				appendJson(obj);
			} catch (Exception e) { }
		}
		buffer.append('}');
	}

	private void iterableToJson(Iterable<?> value) {
		Iterator<?> iter = value.iterator();
		buffer.append('[');
		if (iter.hasNext()) appendJson(iter.next());
		while (iter.hasNext()) {
			buffer.append(',').append(' ');
			appendJson(iter.next());
		}
		buffer.append(']');
	}

	private void charToJson(Character value) {
		buffer.append('"');
		switch (value) {
			case '\b': buffer.append('\\').append('b'); break;
			case '\f': buffer.append('\\').append('f'); break;
			case '\n': buffer.append('\\').append('n'); break;
			case '\r': buffer.append('\\').append('r'); break;
			case '\t': buffer.append('\\').append('t'); break;
			case '"': case '\'': case '\\': case '/': 
				buffer.append('\\').append(value);
				break;
			default: buffer.append(value);
		}
		buffer.append('"');
	}

	private void mapToJson(Map<?, ?> value) {
		buffer.append('{');
		boolean first = true;
		for (Map.Entry<?, ?> v : value.entrySet()) {
			if(first) first = false;
			else buffer.append(',').append(' ');
			buffer.append('"').append(v.getKey().toString()).append('"')
				.append(':').append(' ');
			appendJson(v.getValue());
		}
		buffer.append('}');
	}

	private void arrayToJson(Object value) {
		buffer.append('[');
		int n = Array.getLength(value);
		if (n > 0) appendJson(Array.get(value, 0));
		for (int i = 1; i < n; ++i) {
			buffer.append(',').append(' ');
			appendJson(Array.get(value, i));
		}
		buffer.append(']');
	}
	
	/*
	private static char toLowerCase(char c) {
		return (c >= 'A' && c <= 'Z') ? (char)(c + 0x20) : c;
	}
	
	private static char toUpperCase(char c) {
		return (c >= 'a' && c <= 'z') ? (char)(c - 0x20) : c;
	}
	*/

	private final static char[] HEX_DIGEST = "0123456789abcdef".toCharArray();
	
	/** 转换成16进制 */
	public Fmt appendHex(final byte[] bytes) {
		return appendHex(bytes, '\0');
	}
	
	/** 转换成16进制 */
	public Fmt appendHex(final byte[] bytes, char delimiter) {
		if(bytes == null) return append("null");
		if (bytes.length == 0) return this;
		boolean no_pad = delimiter == '\0';
		
		int b = bytes[0];
		buffer.append(HEX_DIGEST[(b >> 4) & 0xF]) //左移4位，取高4位
			.append(HEX_DIGEST[b & 0xF]); //取低4位
		
		for(int i = 1, n = bytes.length; i < n; ++i) {
			if (!no_pad) buffer.append(delimiter);
			b = bytes[i];
			buffer.append(HEX_DIGEST[(b >> 4) & 0xF]) //左移4位，取高4位
				.append(HEX_DIGEST[b & 0xF]); //取低4位
		}
		
		return this;
	}
	
	/** 转换成16进制 */
	public Fmt appendHex(final int value) {
		int i = 32;
		while (i > 0) {
			i -= 4;
			buffer.append(HEX_DIGEST[(value >> i) & 0xF]);
		}
		return this;
	}
	
	/** 转换成16进制 */
	public Fmt appendHex(final int...args) {
		for (int i = 0, n = args.length; i < n; ++i)
			appendHex(args[i]);
		return this;
	}
	
	/** 转换成16进制 */
	public Fmt appendHex(final char delimiter, final int... args) {
		if (args.length > 0) appendHex(args[0]);
		for (int i = 1, n = args.length; i < n; ++i) {
			if (delimiter != '\0') buffer.append(delimiter);
			appendHex(args[i]);
		}
		return this;
	}
	
	/** 转换成16进制 */
	public Fmt appendHex(final long value) {
		int i = 64;
		while (i > 0) {
			i -= 4;
			buffer.append(HEX_DIGEST[(int)((value >> i) & 0xF)]);
		}
		return this;
	}
	
	/** 转换成16进制 */
	public Fmt appendHex(final long...args) {
		for (int i = 0, n = args.length; i < n; ++i)
			appendHex(args[i]);
		return this;
	}
	
	/** 转换成16进制 */
	public Fmt appendHex(final char delimiter, final long... args) {
		if (args.length > 0) appendHex(args[0]);
		for (int i = 1, n = args.length; i < n; ++i) {
			if (delimiter != '\0') buffer.append(delimiter);
			appendHex(args[i]);
		}
		return this;
	}
	
	private final static char[] BASE64_DIGEST = 
		"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
	private final static char PAD = '=';

	/** base64编码
	 * @param bytes 要编码的字节数组
	 * @param lineSeparator 是否每76个字符换行标志
	 * @return 编码后的字符串
	 */
	public Fmt appendBase64(final byte[] bytes, boolean lineSeparator) {
		//base64转码为3个字节转4个字节，即3个8位转成4个前两位固定为0的共24位
		if (bytes == null) return append("null");
		if (bytes.length == 0) return this;
		int len = bytes.length;
		int bpos = -1, cpos = -1, cc = 0; //字符数组和字节数组的当前写入和读取的索引位置
		//转码数组长度，3的倍数乘4
		int dlen = (len + 2) / 3 * 4;
		if (lineSeparator) dlen += (dlen - 1) / 76 << 1;
		//char[] chars = new char[dlen];
		for (int slen = len - 3; bpos < slen; ) {
			int b1 = bytes[++bpos] & 0xFF; //与FF是防止java的负数二进制补码扩展
			int b2 = bytes[++bpos] & 0xFF;
			int b3 = bytes[++bpos] & 0xFF;
			//原第一字节的头6位
			buffer.append(BASE64_DIGEST[b1 >>> 2])
			//原第一字节的后2位+原第二字节的前4位
				.append(BASE64_DIGEST[((b1 << 4) | (b2 >>> 4)) & 0x3F])
			//原第二字节的前4位+原第三字节的后2位
				.append(BASE64_DIGEST[((b2 << 2) | (b3 >>> 6)) & 0x3F])
			//原第四字节的后6位
				.append(BASE64_DIGEST[b3 & 0x3F]);

			if (lineSeparator && ++cc == 19 && cpos < dlen - 2) {
				buffer.append('\r').append('\n');
				cc = 0;
			}
		}

		int modcount = bytes.length % 3;
		if(modcount > 0) { //非字节对齐时的处理，不足后面补=号，余数为1补2个，余数为2补1个
			int b1 = bytes[++bpos] & 0xFF;
			buffer.append(BASE64_DIGEST[b1 >>> 2]);
			if(modcount == 2){
				int b2 = bytes[++bpos] & 0xFF;
				buffer.append(BASE64_DIGEST[((b1 << 4) | (b2 >>> 4)) & 0x3F])
					.append(BASE64_DIGEST[(b2 << 2) & 0x3F]);
			}
			else{ 
				buffer.append(BASE64_DIGEST[(b1 << 4) & 0x3F])
					.append(PAD); //余数为1，第三个也是=号
			}
			buffer.append(PAD); //余数为1，第三个也是=号
		}

		return this;
	}

	@Override
	public Fmt append(CharSequence s) {
		buffer.append(s);
		return this;
	}

	@Override
	public Fmt append(CharSequence s, int start, int end) {
		buffer.append(s, start, end);
		return this;
	}

	/** 变更指定位置的字符 */
	public Fmt setCharAt(int index, char ch) {
		buffer.setCharAt(index, ch);
		return this;
	}

	/** 删除指定位置的字符 */
	public Fmt delete(int start, int end) {
		buffer.delete(start, end);
		return this;
	}

	/** 删除指定位置的字符 */
	public Fmt deleteCharAt(int index) {
		buffer.deleteCharAt(index);
		return this;
	}

	/** 删除最后一个字符 */
	public Fmt deleteLastChar() {
		return deleteLastChar(1);
	}
	
	/** 删除最后count个字符
	 * @param count 要删除的字符数量
	 * @return
	 */
	public Fmt deleteLastChar(int count) {
		if(buffer.length() >= count)
			buffer.setLength(buffer.length() - count);
		return this;
	}
	
	/** 设置字符串缓冲区长度 */
	public Fmt setLength(int newLength) {
		buffer.setLength(newLength);
		return this;
	}
	
	/** 重新调整字符串缓冲区长度 */
	public Fmt reduce(int size) {
		int len = buffer.length();
		if (size >= len) buffer.setLength(len - size);
		return this;
	}

	@Override
	public char charAt(int index) {
		return buffer.charAt(index);
	}

	@Override
	public int length() {
		return buffer.length();
	}
	
	public CharSequence subSequence(int start) {
		return subSequence(start, buffer.length());
	}
	
	@Override
	public CharSequence subSequence(int start, int end) {
		return buffer.subSequence(start, end);
	}
	
	public String substring(int start) {
		return substring(start, buffer.length());
	}
	
	public String substring(int start, int end) {
		return buffer.substring(start, end);
	}
	
	/** 以json字符串方式追加字符串, 自动对字符串进行json方式转义 */
	public void appendJavascriptString(CharSequence value) {
		if(value == null) appendNull();
		else if(value.length() == 0) buffer.append('"').append('"');
		else {
			StringBuilder buffer = this.buffer;
			buffer.append('"');
			for (int i = 0, len = value.length(); i < len; ++i) {
				char c = value.charAt(i);
				switch (c) {
					case '\b': buffer.append('\\').append('b'); break;
					case '\t': buffer.append('\\').append('t'); break;
					case '\f': buffer.append('\\').append('f'); break;
					case '\n': buffer.append('\\').append('n'); break;
					case '\r': buffer.append('\\').append('r'); break;
					case '"': case '\'': case '/': case '\\':
						buffer.append('\\').append(c);
						break;
					default:
						buffer.append(c);
				}
			}
			buffer.append('"');
		}
	}
	
}

