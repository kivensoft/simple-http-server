package cn.kivensoft.util;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

final public class Strings {
	private final static String UTF8 = "UTF-8";
	private final static char[] HEX_DIGEST = { '0', '1', '2', '3', '4',
			'5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	private final static char[] BASE64_DIGEST = {
			'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
			'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
			'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
			'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/' };
	private final static char PAD = '=';

	private static int[] INV = null;

	private Strings() {}

	/** 字符串空值取缺省值
	 * @param string 要判断的字符串
	 * @param def 缺省值
	 * @return 不为空返回自己, 为空返回缺省值
	 */
	public static String nullToDefault(String string, String def) {
		return string == null ? def : string;
	}

	/** null或空字符串返回缺省值
	 * @param string 要判断的字符串
	 * @return 参数为null或空字符串时返回缺省值, 否则返回原字符串
	 */
	public static String emptyToDefault(String string, String def) {
		return (string == null || string.isEmpty()) ? def : string;
	}

	/** 字符串非空判断,空值和空字符串都返回true */
	public static boolean isNullOrEmpty(String string) {
		return string == null || string.isEmpty();
	}

	/** 右对齐字符串，长度小于minLength则在左边加上padChar
	 * @param src 源字符串
	 * @param minLength 最小长度
	 * @param padChar 补齐字符
	 * @return 生成的字符串
	 */
	public static String padLeft(String src, int minLength, char padChar) {
		int len = src == null ? 0 : src.length();
		if (len >= minLength) return src;
		char[] chars = new char[minLength];
		for(int i = 0, imax = minLength - len; i < imax; ++i)
			chars[i] = padChar;
		if (src != null)
			src.getChars(0, len, chars, minLength - len);
		return new String(chars);
	}

	/** 左对齐字符串，长度小于minLength则在右边加上padChar
	 * @param src 源字符串
	 * @param minLength 最小长度
	 * @param padChar 补齐字符
	 * @return 生成的字符串
	 */
	public static String padRight(String src, int minLength, char padChar) {
		int len = src == null ? 0 : src.length();
		if (len >= minLength) return src;
		char[] chars = new char[minLength];
		if (src != null)
			src.getChars(0, len, chars, 0);
		for(int i = len; i < minLength; ++i)
			chars[i] = padChar;
		return new String(chars);
	}

	/** 路径连接, 去除中间多余的路径分隔符'/'或'\' 
	 * @param paths 要连接的多个路径字符串
	 * @return 生成的路径
	 */
	public static String joinPath(String... paths) {
		StringBuilder sb = new StringBuilder(nullToDefault(paths[0], ""));
		for (int i = 1, n = paths.length; i < n; ++i) {
			String p = paths[i];
			if (p == null || p.isEmpty()) continue;
			char p_char = p.charAt(0);
			boolean begin = p_char == '/' || p_char == '\\';
			int sb_len = sb.length();
			char sb_char = sb_len == 0 ? '\0' : sb.charAt(sb_len - 1);
			boolean last = sb_char == '/' || sb_char == '\\';
			if (last && begin)
				sb.setLength(sb.length() - 1);
			else if (!last && !begin)
				sb.append('/');
			sb.append(p);
		}
		return sb.toString();
	}

	/** 生成重复N次的字符串 */
	public static String repeat(String string, int count) {
		int strlen = string.length();
		//如果字符串长度为1，转为调用速度更快的方法
		if(strlen == 1) return repeat(string.charAt(0), count);

		char[] retChars = new char[strlen * count];
		for(int i = 0; i < count; ++i)
			string.getChars(0, strlen, retChars, i * strlen);

		return new String(retChars);
	}

	/** 生成重复N次的字符串 */
	public static String repeat(char ch, int count) {
		char[] chars = new char[count];
		for(int i = 0; i < count; ++i) chars[i] = ch;
		return new String(chars);
	}

	/** 字节数组转字符串 */
	public static String fromBytes(byte[] bytes) {
		return fromBytes(bytes, 0, bytes.length);
	}

	/** 字节数组转字符串 */
	public static String fromBytes(byte[] bytes, int offset) {
		return fromBytes(bytes, 0, bytes.length - offset);
	}

	/** 字节数组转字符串 */
	public static String fromBytes(byte[] bytes, int offset, int length) {
		try {
			return new String(bytes, offset, length, UTF8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/** 字符串转字节数组 */
	public static byte[] toBytes(String string) {
		try {
			return string.getBytes(UTF8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/** md5加密
	 * @param bytes 要加密的内容
	 * @return 加密后的内容
	 */
	public static byte[] md5(final byte[] bytes) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			return md.digest(bytes);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	/** md5加密
	 * @param text 要加密的文本
	 * @return 加密后的内容的16进制表示
	 */
	public static String md5(String text) {
		try {
			return toHex(md5(text.getBytes(UTF8)));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/** hmacsha1加密
	 * @param key 密钥
	 * @param bytes 要加密的内容
	 * @return 加密后的内容
	 */
	public static byte[] hmacsha1(final byte[] key, final byte[] bytes) {
		try {
			SecretKey secretKey = new SecretKeySpec(key, "HmacSHA1");
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(secretKey);
			return mac.doFinal(bytes);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**hmacsha1加密
	 * @param key 密钥
	 * @param text 要加密的文本
	 * @return 加密后的内容的base64编码表示
	 */
	public static String hmacsha1(String key, String text) {
		try {
			return toBase64(hmacsha1(key.getBytes(UTF8), text.getBytes(UTF8)));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/** 转换成16进制 */
	public static String toHex(final byte[] bytes) {
		return toHex(bytes, '\0');
	}

	/** 转换成16进制 */
	public static String toHex(final byte[] bytes, final char separator) {
		if(bytes == null) return null;
		if (bytes.length == 0) return "";
		//转码后的长度是字节数组长度的2倍
		int len = bytes.length * 2;
		if (separator != '\0') len += bytes.length - 1;
		char[] chars = new char[len];

		int idx = -1;
		int b = bytes[0];
		chars[++idx] = HEX_DIGEST[(b >> 4) & 0xF]; //左移4位，取高4位
		chars[++idx] = HEX_DIGEST[b & 0xF]; //取低4位

		for(int i = 1, n = bytes.length; i < n; ++i) {
			if (separator != '\0') chars[++idx] = separator;
			b = bytes[i];
			chars[++idx] = HEX_DIGEST[(b >> 4) & 0xF]; //左移4位，取高4位
			chars[++idx] = HEX_DIGEST[b & 0xF]; //取低4位
		}

		return new String(chars);
	}

	/** 从16进制转换成字符串 */
	public static byte[] fromHex(final String string) {
		if (string == null || string.length() < 2) return null;

		// 计算有效字符数量, 空格逗号等分隔符不计入内
		byte[] sb = string.getBytes();
		int count = 0;
		for (int i = 0, n = sb.length; i < n; ++i) {
			byte b = sb[i];
			if (b >= 0x30 && b <= 0x39 || b >= 0x41 && b <= 0x46
					|| b >= 0x61 && b <= 0x66)
				++count;
		}

		byte[] ret = new byte[count >> 1];
		int pos = -1;
		int fb = -1, tb;
		for(int i = 0, n = sb.length; i < n; ++i) {
			Byte b = sb[i];
			if(b >= 0x30 && b <= 0x39) tb = b - 0x30; //0-9
			else if(b >= 0x41 && b <= 0x46) tb = b - 0x37; //A-F
			else if(b >= 0x61 && b <= 0x66) tb = b - 0x57; //a-f
			else continue;
			if (fb == -1) fb = tb;
			else {
				ret[++pos] = (byte)(fb << 4 | tb);
				fb = -1;
			}
		}

		return ret;
	}

	/** BASE64简单编码，生成的编码不自动换行 */
	public static String toBase64(final byte[] bytes) {
		return toBase64(bytes, false);
	}

	/** base64编码, 3个字节转4个字节，即3个8位转成4个前两位固定为0的8位共24位
	 * @param bytes 要编码的字节数组
	 * @param lineBreak 是否每76个字符换行标志
	 * @return 编码后的字符串
	 */
	public static String toBase64(final byte[] bytes, boolean lineBreak) {
		if (bytes == null) return null;
		if (bytes.length == 0) return "";
		int len = bytes.length;
		// 转码数组长度，3的倍数乘4
		int dlen = (len + 2) / 3 * 4;
		// 有换行要求, 则转码数组长度要加上每76个字符\r\n2个换行符
		if (lineBreak) dlen += (dlen - 1) / 76 << 1;
		char[] chars = new char[dlen];
		// 字符数组和字节数组的当前写入和读取的索引位置
		int bpos = 0, cpos = 0, cc = 0;
		// 循环处理, 剩余的字节再单独做补"="字符处理
		for (int slen = len - 2, rdlen = dlen - 2; bpos < slen; bpos += 3) {
			int b1 = bytes[bpos] & 0xFF; //与FF是防止java的负数二进制补码扩展
			int b2 = bytes[bpos + 1] & 0xFF;
			int b3 = bytes[bpos + 2] & 0xFF;
			//原第一字节的头6位
			chars[cpos] = BASE64_DIGEST[b1 >>> 2];
			//原第一字节的后2位+原第二字节的前4位
			chars[cpos + 1] = BASE64_DIGEST[((b1 << 4) | (b2 >>> 4)) & 0x3F];
			//原第二字节的前4位+原第三字节的后2位
			chars[cpos + 2] = BASE64_DIGEST[((b2 << 2) | (b3 >>> 6)) & 0x3F];
			//原第四字节的后6位
			chars[cpos + 3] = BASE64_DIGEST[b3 & 0x3F];
			cpos += 4;

			if (lineBreak && ++cc == 19 && cpos < rdlen) {
				chars[cpos] = '\r';
				chars[cpos + 1] = '\n';
				cpos += 2;
				cc = 0;
			}
		}

		int modcount = bytes.length % 3;
		if(modcount > 0) { //非字节对齐时的处理，不足后面补=号，余数为1补2个，余数为2补1个
			int b1 = bytes[bpos++] & 0xFF;
			chars[cpos++] = BASE64_DIGEST[b1 >>> 2];
			if(modcount == 2){
				int b2 = bytes[bpos++] & 0xFF;
				chars[cpos++] = BASE64_DIGEST[((b1 << 4) | (b2 >>> 4)) & 0x3F];
				chars[cpos++] = BASE64_DIGEST[(b2 << 2) & 0x3F];
			}
			else{ 
				chars[cpos++] = BASE64_DIGEST[(b1 << 4) & 0x3F];
				chars[cpos++] = PAD; //余数为1，第三个也是=号
			}
			chars[cpos++] = PAD; //余数为1，第三个也是=号
		}

		return new String(chars);
	}

	/** base64简单解码，处理自动换行 */
	public static byte[] fromBase64(final String base64) {
		if (INV == null) {
			synchronized (Strings.class) {
				if (INV == null) {
					INV = new int[128];
					Arrays.fill(INV, -1);
					for (int i = 0, n = BASE64_DIGEST.length; i < n; ++i)
						INV[BASE64_DIGEST[i]] = i;
					INV[PAD] = 0;
				}
			}
		}

		byte[] bb = base64.getBytes();
		// 计算"="等号数量
		int padc = 0, nl = 0, i = bb.length;
		while (--i >= 0) {
			byte b =  bb[i];
			if (b == '=') padc++;
			else if (b != '\r' && b != '\n') break;
		}
		// 计算回车换行数量
		i = bb.length;
		while (--i >= 0) {
			byte b = bb[i];
			if (b == '\r' || b == '\n') ++nl;
		}
		// 分配解码内存
		byte[] ret = new byte[(bb.length - nl) / 4 * 3 - padc];
		int pos = 0, bpos = 0, cc = 0, tmp = 0;
		for (int blen = bb.length, clen = ret.length - 3; bpos < blen; ++bpos) {
			byte b = bb[bpos];
			if (b == '\r' || b == '\n') continue;
			switch (cc++) {
				case 0: tmp = (INV[b] << 18) & 0xFFFFFF; break;
				case 1: tmp |= (INV[b] << 12) & 0x3FFFF; break;
				case 2: tmp |= (INV[b] << 6) & 0xFFF; break;
				case 3:
					if (pos < clen) {
						ret[pos] = (byte)(tmp >> 16);
						ret[pos + 1] = (byte)(tmp >> 8);
						ret[pos + 2] = (byte)(tmp | INV[b]);
						pos += 3;
						cc = 0;
					}
					else {
						ret[pos] = (byte)(tmp >> 16);
						if (padc < 2) ret[pos + 1] = (byte)(tmp >> 8);
						if (padc < 1) ret[pos + 2] = (byte)(tmp | INV[b]);
					}
			}
		}

		return ret;
	}

	/** 判断是否是纯数字,允许开头有加减号
	 * @param text 要判断的文本
	 * @return true:纯数字,false:不是纯数字
	 */
	public static boolean isInt(String text) {
		if (text == null || text.isEmpty()) return false;
		char c = text.charAt(0);
		if (c != '-' && c != '+' && (c < '0' || c > '9')) return false;
		for (int i = 1, n = text.length(); i < n; ++i) {
			c = text.charAt(i);
			if (c < '0' || c > '9') return false;
		}
		return true;
	}

	/** 判断字符串是否浮点数格式
	 * @param text 要判断的文本
	 * @return true:浮点数,false:不是浮点数
	 */
	public static boolean isNumber(String text) {
		if (text == null || text.isEmpty()) return false;

		int index = 0, len = text.length();
		char c = text.charAt(index++);
		if (c < '0' || c > '9') {
			if (c != '-' && c != '+') return false;
			if (len < 2) return false;
			c = text.charAt(index++);
			if (c < '0' || c > '9') return false;
		}

		for (boolean findedDot = false; index < len; ++index) {
			c = text.charAt(index);
			if (c < '0' || c > '9') {
				if (c == '.') {
					if (findedDot) return false;
					else findedDot = true;
				}
				else return false;
			}
		}
		return true;
		//return Pattern.matches("-?[0-9]+(\\.[0-9]+)?", text);
	}

	/** 判断是否金额格式
	 * @param text 要判断的文本
	 * @return true:金额格式,false:不是金额格式
	 */
	public static boolean isMoney(String text) {
		if (text == null || text.isEmpty()) return false;

		boolean firstNumber = false, hasDot = false;
		int lastNumber = 0;
		for (int i = 0, len = text.length(); i < len; ++i) {
			char c = text.charAt(i);
			if (c >= '0' && c <= '9') {
				if (!firstNumber) firstNumber = true;
				if (hasDot || ++lastNumber > 2) return false;
			}
			else if (c == '.') {
				if (!firstNumber || hasDot) return false;
				hasDot = true;
			} else if (i == 0 && (c == '-' || c == '+')) {
			} else {
				return false;
			}
		}

		return true;
		//return Pattern.matches("-?[0-9]+(\\.[0-9][0-9]?)?", text);
	}

	/** 判断是否是手机号码 */
	public static boolean isMobile(String text) {
		if (text == null || text.length() != 11) return false;
		if (text.charAt(0) != '1') return false;
		char c2 = text.charAt(1);
		if (c2 < '3' || c2 > '9') return false;
		for (int i = 2, imax = text.length(); i < imax; ++i) {
			char c3 = text.charAt(i);
			if (c3 < '0' || c3 > '9') return false;
		}
		return true;
	}

	/** 判断是否email地址 */
	public static boolean isEmail(String text) {
		if (text == null || text.length() < 5) return false;
		int len = text.length(), at = -1, dot = -1;
		for (int i = 0; i < len; ++i) {
			char c = text.charAt(i);
			if (c == '@') {
				if (at != -1 || i == 0) return false;
				at = i;
			} else if (c == '.') {
				if (dot != -1 || i < 3) return false;
				dot = i;
			}
		}

		return at > 0 && at < len - 3 && dot >= 3 && dot < len - 1;
	}

	/** 格式化日期时间为"yyyy-MM-dd HH:mm:ss"格式
	 * @param date 要格式化的日期对象
	 * @return 格式化后的文本
	 */
	public static String formatDateTime(Date date) {
		if (date == null) return "";
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
	}

	public static String formatDateTime(Date date, DateFormat dateFormat) {
		if (date == null) return "";
		return dateFormat.format(date);
	}

	/** 格式化日期时间为"yyyy-MM-dd"格式
	 * @param date 要格式化的日期对象
	 * @return 格式化后的文本
	 */
	public static String formatDate(Date date) {
		if (date == null) return "";
		return new SimpleDateFormat("yyyy-MM-dd").format(date);
	}

	public static String formatDate(Date date, DateFormat dateFormat) {
		if (date == null) return "";
		return dateFormat.format(date);
	}

	/** 格式化日期时间为"HH:mm:ss"格式
	 * @param date 要格式化的日期对象
	 * @return 格式化后的文本
	 */
	public static String formatTime(Date date) {
		if (date == null) return "";
		return new SimpleDateFormat("HH:mm:ss").format(date);
	}

	public static String formatTime(Date date, DateFormat dateFormat) {
		if (date == null) return "";
		return dateFormat.format(date);
	}

	/** 格式化日期时间为"yyyy-MM-dd'T'HH:mm:ss'Z'"格式
	 * @param date 要格式化的日期对象
	 * @return 格式化后的文本
	 */
	public static String formatGmtDateTime(Date date) {
		if (date == null) return "";
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		df.setTimeZone(TimeZone.getTimeZone("GMT"));	
		return df.format(date);
	}

	public static String changeExt(String origin, String newExt, char sep) {
		if (origin == null) return null;
		int idx = origin.lastIndexOf(sep);
		StringBuilder sb = new StringBuilder();
		if (idx != -1) sb.append(origin, 0, idx + 1);
		else sb.append(sep);
		sb.append(newExt);
		return sb.toString();
	}

	@FunctionalInterface
	public static interface OnSplitInt<T> {
		void accept (int index, int value, T result);
	}

	/** 解析由逗号或空格分隔的数字字符串成整数数组 */
    public static <R> R splitInt(String value, R result, OnSplitInt<R> consumer) {
		if (value == null || value.isEmpty()) return result;
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
				consumer.accept(index++, num, result);
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
		return result;
	}

	/** 解析由逗号或空格分隔的数字字符串成整数数组 */
    public static List<Integer> splitInt(String value) {
    	return splitInt(value, new ArrayList<Integer>(), (i, v, r) -> r.add(v));
    }

	/** 解析日期时间字段成 年/月/日/时/分/秒/毫秒 数组 */
	public static int[] splitDate (String value) {
		return splitInt(value, new int[7],
				(i, v, r) -> { if (i < 7) r[i] = v;
		});
	}

	/** 解析时间日期格式, yyyy-MM-dd HH:mm:ss.sss格式 或iso8601格式 */
	public static Date parseDate(String text) {
		if (text == null || text.isEmpty()) return null;
		if (text.indexOf('-') < 1 || text.length() < 5) return null;
		boolean isGmt = text.indexOf('T') > 0;
		int[] vs = splitDate(text);
		Date ret = null;
		Calendar cal = Calendar.getInstance();
		if (isGmt) cal.setTimeZone(TimeZone.getTimeZone("GMT"));

		cal.set(vs[0], vs[1] - 1, vs[2], vs[3], vs[4], vs[5]);
		cal.set(Calendar.MILLISECOND, vs[6]);
		ret = cal.getTime();

		return ret;
	}

	/** 解析时间日期格式, yyyy-MM-dd HH:mm:ss.sss格式 或iso8601格式 */
	public static Date parseDate(String text, Calendar cal) {
		if (text == null || text.isEmpty()) return null;
		if (text.indexOf('-') < 1 || text.length() < 5) return null;
		boolean isGmt = text.indexOf('T') > 0;
		int[] vs = splitDate(text);
		Date ret = null;

		boolean cal_gmt = "GMT".equals(cal.getTimeZone().getID());
		if (isGmt && !cal_gmt)
			cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		else if (!isGmt && cal_gmt)
			cal.setTimeZone(TimeZone.getDefault());

		cal.set(vs[0], vs[1] - 1, vs[2], vs[3], vs[4], vs[5]);
		cal.set(Calendar.MILLISECOND, vs[6]);
		ret = cal.getTime();

		return ret;
	}

	/** 解析本地日期格式 yyyy-MM-dd格式 */
	public static LocalDate parseLocalDate(String text) {
		if (text == null || text.isEmpty()) return null;
		if (text.indexOf('-') < 1 || text.length() < 5) return null;
		int[] vs = splitDate(text);
		return LocalDate.of(vs[0], vs[1], vs[2]);
	}

	/** 解析本地日期时间格式 yyyy-MM-dd HH:mm:ss.sss 格式 */
	public static LocalDateTime parseLocalDateTime(String text) {
		if (text == null || text.isEmpty()) return null;
		if (text.indexOf('-') < 1 || text.length() < 5) return null;
		int[] vs = splitDate(text);
		return LocalDateTime.of(vs[0], vs[1], vs[2], vs[3], vs[4], vs[5], vs[6]);
	}

	/** 解析本地时间格式 HH:mm:ss.sss格式 */
	public static LocalTime parseLocalTime(String text) {
		if (text == null || text.isEmpty()) return null;
		if (text.indexOf(':') < 1 || text.length() < 5) return null;
		int[] vs = splitDate(text);
		return LocalTime.of(vs[0], vs[1], vs[2], vs[3]);
	}

	/** 解析命令行参数，双引号""表示一个完整的参数，反斜杠\表示转义字符
	 * @param line 命令行
	 * @return 解析后的参数
	 */
	public static List<String> parseCmdLine(String line) {
		List<String> args = new ArrayList<String>();
		if (line == null || line.isEmpty()) return args;

		StringBuilder sb = new StringBuilder();
		boolean inWord = false, isQuota = false;
		// 处理用户输入的命令, 分割成标准的命令行参数
		for (int pos = 0, len = line.length(); pos < len; pos++) {
			char c = line.charAt(pos);
			// 上一个是转义字符, 直接写入本字符
			switch (c) {
				// 双引号内的空格不做处理, 否则生成一个命令或参数
				case ' ':
					if (inWord) {
						if (isQuota) sb.append(c);
						else {
							args.add(sb.toString());
							sb.delete(0, sb.length());
							inWord = false;
						}
					}
					break;
				// 起始双引号做标记, 结束双引号生成命令或参数
				case '"':
					if (inWord) {
						if (isQuota) {
							args.add(sb.toString());
							sb.delete(0, sb.length());
							isQuota = false;
							inWord = false;
						}
						else sb.append(c);
					}
					else {
						inWord = true;
						isQuota = true;
					}
					break;
				// 转义字符, 标记
				case '\\':
					if (pos + 1 < len && line.charAt(pos + 1) == '"') {
						sb.append('"');
						++pos;
					}
					else sb.append(c);
					break;

				default:
					sb.append(c);
					inWord = true;
					break;
			}
		}
		if (sb.length() > 0) args.add(sb.toString());

		return args;
	}

	/** 解析字符串,按指定的字符做分隔符
	 * @param text 要解析的文本
	 * @param separator 分隔符
	 * @return 解析后的字符串列表
	 */
	public static List<String> split(String text, char separator) {
		return split(text, separator, new ArrayList<>(),
				(b, e, r) -> r.add(text.substring(b, e)));
	}

	/** 解析字符串,按指定的字符做分隔符
	 * @param text 要解析的文本
	 * @param separator 分隔符, 多个
	 * @return 解析后的字符串列表
	 */
	public static List<String> split(String text, char... separators) {
		return split(text, separators, new ArrayList<String>(),
				(b, e, r) -> r.add(text.substring(b, e)));
	}

	/** 解析字符串,按指定的字符做分隔符
	 * @param text 要解析的文本
	 * @param separator 分隔符字符串, 多个
	 * @return 解析后的字符串列表
	 */
	public static List<String> split(String text, String separator) {
		return split(text, separator, new ArrayList<String>(),
				(b, e, r) -> r.add(text.substring(b, e)));
	}

	@FunctionalInterface
    public static interface SplitConsumer<T> {
		void accept(int start, int stop, T result);
	}

	/** 解析字符串,按指定的字符做分隔符
	 * @param text 要解析的文本
	 * @param separator 分隔符
	 */
	public static <R> R split(String text, char separator, R result,
			SplitConsumer<R> consumer) {
		if (text == null || text.isEmpty()) return result;
		int index = -1;
		for (int i = 0, len = text.length(); i < len; ++i) {
			char c = text.charAt(i);
			if (c == separator) {
				if (index != -1) {
					consumer.accept(index, i, result);
					index = -1;
				}
			}
			else if (index == -1) index = i;
		}
		if (index != -1) consumer.accept(index, text.length(), result);
		return result;
	}

	/** 解析字符串,按指定的字符做分隔符
	 * @param text 要解析的文本
	 * @param separator 分隔符字符串, 多个
	 */
	public static <R> R split(String text, String separator, R result,
			SplitConsumer<R> consumer) {
		if (text == null || text.isEmpty()) return result;
		int len = separator.length();
		char[] chars = new char[len];
		separator.getChars(0, len, chars, 0);
		return split(text, chars, result, consumer);
	}

	/** 解析字符串,按指定的字符做分隔符
	 * @param text 要解析的文本
	 * @param separator 分隔符字符串, 多个
	 */
	public static <R> R split(String text, char[] separators, R result,
			SplitConsumer<R> consumer) {
		if (text == null || text.isEmpty()) return result;
		int slen = separators.length, index = -1;
		for (int i = 0, n = text.length(); i < n; ++i) {
			char c = text.charAt(i);
			int j = 0;
			while (j < slen && c != separators[j]) ++j;
			if (j < slen) {
				if (index != -1) {
					consumer.accept(index, i, result);
					index = -1;
				}
			}
			else if (index == -1) index = i;
		}
		if (index != -1) consumer.accept(index, text.length(), result);
		return result;
	}

	/** 转换文本内容为对象
	 * @param cls 对象类型
	 * @param value 文本内容
	 * @return 转换后生成的对象，转换失败返回null
	 */
	@SuppressWarnings("unchecked")
	public static <T> T valueOf(String value, Class<T> cls) {
		if (value == null || value.isEmpty()) return null;

		T ret = null;
		switch (cls.getName()) {
			case "java.lang.String":
				ret = (T) value;
				break;
			case "java.lang.Integer":
				ret = (T) Integer.valueOf(value);
				break;
			case "java.lang.Long":
				ret = (T) Long.valueOf(value);
				break;
			case "java.lang.Byte":
				ret = (T) Byte.valueOf(value);
				break;
			case "java.lang.Short":
				ret = (T) Short.valueOf(value);
				break;
			case "java.lang.Character":
				ret = (T) Character.valueOf(value.charAt(0));
				break;
			case "java.lang.Boolean":
				ret = (T) Boolean.valueOf(value);
				break;
			case "java.lang.Double":
				ret = (T) Double.valueOf(value);
				break;
			case "java.lang.Float":
				ret = (T) Float.valueOf(value);
				break;
			case "java.util.Date":
				ret = (T) parseDate(value);
				break;
			case "java.time.LocalDate":
				ret = (T) parseLocalDate(value);
				break;
			case "java.time.LocalTime":
				ret = (T) parseLocalTime(value);
				break;
			case "java.time.LocalDateTime":
				ret = (T) parseLocalDateTime(value);
				break;
			case "java.math.BigDecimal":
				ret = (T) new BigDecimal(value);
				break;
			case "java.math.BigInteger":
				ret = (T) new BigInteger(value);
				break;
			case "[B":
				ret = (T) fromHex((String) value);
				break;
			default:
				if (cls.isEnum()) {
					for (T v : cls.getEnumConstants()) {
						if (value.equals(v.toString())) {
							ret = v;
							break;
						}
					}
				}
				break;
		}
		
		return ret;
	}

	public static void main(String[] args) throws Exception {
		if(args.length == 0)
			System.out.println("Usage: Crypt <string> <string>");
		else {
			System.out.printf("hex            : %s -> %s\n",
					args[0], toHex(args[0].getBytes("UTF-8")));
			System.out.printf("base64         : %s -> %s\n",
					args[0], toBase64(args[0].getBytes("UTF-8")));
			if(args.length == 1){
				System.out.printf("md5_hex        : %s -> %s\n",
						args[0], md5(args[0]));
				System.out.printf("md5_base64     : %s -> %s\n",
						args[0], toBase64(md5(args[0].getBytes("UTF-8"))));
			}
			else if (args.length == 2) {
				System.out.printf("hmacsha1_hex   : %s, %s -> %s\n",
						args[0], args[1], hmacsha1(args[0], args[1]));
				System.out.printf("hmacsha1_base64: %s, %s -> %s\n",
						args[0], args[1], toBase64(hmacsha1(args[0].getBytes("UTF-8"),
								args[1].getBytes("UTF-8"))));
			}
		}
	}

}
