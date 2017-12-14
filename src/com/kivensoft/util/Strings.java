package com.kivensoft.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public abstract class Strings {
	private static final String UTF8 = "UTF-8";
	private final static char[] HEX_DIGEST = "0123456789abcdef".toCharArray();
	private final static char[] BASE64_DIGEST = 
		"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
	private final static char PAD = '=';
	private final static int[] INV = new int[128];

	static {
		Arrays.fill(INV, -1);
		for (int i = 0, n = BASE64_DIGEST.length; i < n; i++)
			INV[BASE64_DIGEST[i]] = i;
		INV[PAD] = 0;
	}

	
	/** 字符串空值转换成空字符串,如果传入参数不是空字符串则不进行转换 */
	public static String nullToEmpty(String string) {
		return (string == null) ? "" : string;
	}

	/** 空字符串转成空值，非空字符串不进行转换 */
	public static String emptyToNull(String string) {
		return (string != null && string.isEmpty()) ? null : string;
	}

	/** 字符串非空判断,空值和空字符串都返回false */
	public static boolean isNotNullOrEmpty(String string) {
		return string != null && !string.isEmpty();
	}

	/** 字符串空判断，支持同时判断多个，全部有效返回true */
	public static boolean isNotNullOrEmpty(String ...args) {
		for(String arg : args)
			if(arg == null || arg.isEmpty()) return false;
		return true;
	}

	/** 字符串非空判断,空值和空字符串都返回true */
	public static boolean isNullOrEmpty(String string) {
		return string == null || string.isEmpty();
	}
	
	/** 字符串空判断，支持同时判断多个，只要有一个不为为null或空则返回true */
	public static boolean isNullOrEmpty(String ...args) {
		for(String arg : args)
			if(arg != null && !arg.isEmpty()) return false;
		return true;
	}

	/** 右对齐字符串，长度小于minLength则在左边加上padChar */
	public static String padLeft(String string, int minLength, char padChar) {
		int strLen = string.length();
		if (strLen >= minLength) return string;
		char[] chars = new char[minLength];
		for(int i = 0, n = minLength - strLen; i < n; ++i)
			chars[i] = padChar;
		string.getChars(0, strLen, chars, strLen);
		return new String(chars);
	}

	/** 左对齐字符串，长度小于minLength则在右边加上padChar */
	public static String padRight(String string, int minLength, char padChar) {
		int strLen = string.length();
		if (string.length() >= minLength) return string;
		char[] chars = new char[minLength];
		string.getChars(0, strLen, chars, 0);
		for(int i = strLen; i < minLength; ++i)
			chars[i] = padChar;
		return new String(chars);
	}

	/** 生成重复N次的字符串 */
	public static String repeat(String string, int count) {
		int strLen = string.length();
		
		//如果字符串长度为1，转为调用速度更快的方法
		if(strLen == 1) return repeat(string.charAt(0), count);
		
		char[] chars = new char[strLen];
		char[] retChars = new char[strLen * count];
		string.getChars(0, strLen, chars, 0);
		for(int i = 0; i < count; i += strLen)
			System.arraycopy(chars, 0, retChars, i, strLen);

		return new String(chars);
	}

	/** 生成重复N次的字符串 */
	public static String repeat(char ch, int count) {
		char[] chars = new char[count];
		for(int i = 0; i < count; ++i) chars[i] = ch;
		return new String(chars);
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
			return null;
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
			return null;
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
			return null;
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
			return null;
		}
	}

	/** 转换成16进制 */
	public static String toHex(final byte[] bytes) {
		return toHex(bytes, '\0');
	}
	
	/** 转换成16进制 */
	public static String toHex(final byte[] bytes, char separator) {
		if(bytes == null) return null;
		if (bytes.length == 0) return "";
		boolean no_pad = separator == '\0';
		//转码后的长度是字节数组长度的2倍
		int len = bytes.length * 2;
		if (!no_pad) len += bytes.length - 1;
		char[] chars = new char[len];
		
		int c_idx = -1;
		int b = bytes[0];
		chars[++c_idx] = HEX_DIGEST[(b >> 4) & 0xF]; //左移4位，取高4位
		chars[++c_idx] = HEX_DIGEST[b & 0xF]; //取低4位
		
		for(int i = 1, n = bytes.length; i < n; ++i) {
			if (!no_pad) chars[++c_idx] = separator;
			b = bytes[i];
			chars[++c_idx] = HEX_DIGEST[(b >> 4) & 0xF]; //左移4位，取高4位
			chars[++c_idx] = HEX_DIGEST[b & 0xF]; //取低4位
		}
		
		return new String(chars);
	}

	/** 从16进制转换成字符串 */
	public static byte[] fromHex(final String string) {
		byte[] sb = string.getBytes();
		int sep_count = 0;
		for (int i = 0, n = sb.length; i < n; ++i) {
			byte b = sb[i];
			if (b < 0x30 || (b > 0x39 && b < 0x41)
					|| (b > 0x46 && b < 0x61) || b > 0x66)
				++sep_count;
		}
			
		byte[] ret = new byte[(sb.length - sep_count) >> 1];
		int pos = -1, cc = -1;
		byte[] bs = new byte[2];
		for(int i = 0, n = sb.length; i < n; ++i) {
			byte b = sb[i];
			if (b < 0x30 || (b > 0x39 && b < 0x41)
					|| (b > 0x46 && b < 0x61) || b > 0x66)
				continue;
			bs[++cc] = b;
			if (cc < 1) continue;
			byte b1 = bs[0];
			if(b1 >= 0x30 && b1 <= 0x39) b1 -= 0x30; //0-9
			else if(b1 >= 0x41 && b1 <= 0x46) b1 -= 0x37; //A-F
			else if(b1 >= 0x61 && b1 <= 0x66) b1 -= 0x57; //a-f
			byte b2 = bs[1];
			if(b2 >= 0x30 && b2 <= 0x39) b2 -= 0x30;
			else if(b2 >= 0x41 && b2 <= 0x46) b2 -= 0x37;
			else if(b2 >= 0x61 && b2 <= 0x66) b2 -= 0x57;
			ret[++pos] = (byte)(b1 << 4 | b2);
			cc = -1;
		}
		
		return ret;
	}

	/** BASE64简单编码，生成的编码不自动换行 */
	public static String toBase64(final byte[] bytes) {
		return toBase64(bytes, false);
	}
	
	/** base64编码
	 * @param bytes 要编码的字节数组
	 * @param lineSeparator 是否每76个字符换行标志
	 * @return 编码后的字符串
	 */
	public static String toBase64(final byte[] bytes, boolean lineSeparator) {
		//base64转码为3个字节转4个字节，即3个8位转成4个前两位固定为0的共24位
		if (bytes == null) return null;
		if (bytes.length == 0) return "";
		int len = bytes.length;
		int bpos = -1, cpos = -1, cc = 0; //字符数组和字节数组的当前写入和读取的索引位置
		//转码数组长度，3的倍数乘4
		int dlen = (len + 2) / 3 * 4;
		if (lineSeparator) dlen += (dlen - 1) / 76 << 1;
		char[] chars = new char[dlen];
		for (int slen = len - 3; bpos < slen; ) {
			int b1 = bytes[++bpos] & 0xFF; //与FF是防止java的负数二进制补码扩展
			int b2 = bytes[++bpos] & 0xFF;
			int b3 = bytes[++bpos] & 0xFF;
			//原第一字节的头6位
			chars[++cpos] = BASE64_DIGEST[b1 >>> 2];
			//原第一字节的后2位+原第二字节的前4位
			chars[++cpos] = BASE64_DIGEST[((b1 << 4) | (b2 >>> 4)) & 0x3F];
			//原第二字节的前4位+原第三字节的后2位
			chars[++cpos] = BASE64_DIGEST[((b2 << 2) | (b3 >>> 6)) & 0x3F];
			//原第四字节的后6位
			chars[++cpos] = BASE64_DIGEST[b3 & 0x3F];

			if (lineSeparator && ++cc == 19 && cpos < dlen - 2) {
				chars[++cpos] = '\r';
				chars[++cpos] = '\n';
				cc = 0;
			}
		}

		int modcount = bytes.length % 3;
		if(modcount > 0) { //非字节对齐时的处理，不足后面补=号，余数为1补2个，余数为2补1个
			int b1 = bytes[++bpos] & 0xFF;
			chars[++cpos] = BASE64_DIGEST[b1 >>> 2];
			if(modcount == 2){
				int b2 = bytes[++bpos] & 0xFF;
				chars[++cpos] = BASE64_DIGEST[((b1 << 4) | (b2 >>> 4)) & 0x3F];
				chars[++cpos] = BASE64_DIGEST[(b2 << 2) & 0x3F];
			}
			else{ 
				chars[++cpos] = BASE64_DIGEST[(b1 << 4) & 0x3F];
				chars[++cpos] = PAD; //余数为1，第三个也是=号
			}
			chars[++cpos] = PAD; //余数为1，第三个也是=号
		}

		return new String(chars);
	}

	/** base64简单解码，处理自动换行 */
	public static byte[] fromBase64(final String base64) {
		byte[] bb = base64.getBytes();
		int padc = 0;
		for(int i = bb.length - 1; i >= 0; --i) {
			byte b =  bb[i];
			if (b == '=') padc++;
			else if (b != '\r' && b != '\n') break;
		}
		int nl = 0;
		for (int i = 0, n = bb.length; i < n; ++i) {
			byte b = bb[i];
			if (b == '\r' || b == '\n') ++nl;
		}
		System.out.println("padc = " + padc + ", ret size = " + ((bb.length - nl) / 4 * 3 - padc));

		byte[] ret = new byte[(bb.length - nl) / 4 * 3 - padc];
		
		int pos = -1, bpos = -1, cc = -1;
		int[] ints = new int[4];
		for(int n = bb.length - 1, m = ret.length - 1; bpos < n; ) {
			byte b = bb[++bpos];
			if (b == '\r' || b == '\n') continue;
			ints[++cc] = INV[b];
			if (cc < 3) continue;
			ret[++pos] = (byte)((ints[0] << 2) | ((ints[1] >> 4) & 0x3));
			if (ints[2] != 0 && pos < m)
				ret[++pos] = (byte)((ints[1] << 4) | (ints[2] >> 2 & 0xF));
			if (ints[3] != 0 && pos < m)
				ret[++pos] = (byte)((ints[2] << 6) | ints[3]);
			cc = -1;
		}
		
		return ret;
	}
	
	public static boolean isInt(String text) {
		return Pattern.matches("[\\+\\-]?[0-9]+", text);
	}

	public static boolean isFloat(String text) {
		return Pattern.matches("[\\+\\-]?[0-9]+(\\.[0-9]+)?", text);
	}
	
	public static boolean isMoney(String text) {
		return Pattern.matches("[\\+\\-]?[0-9]+(\\.[0-9][0-9]?)?", text);
	}
	
	private static DateFormat dfDate = new SimpleDateFormat("yyyy-MM-dd");
	private static DateFormat dfDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static DateFormat dfTime = new SimpleDateFormat("HH:mm:ss");
	private static DateFormat dfGmtDateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

	public static String formatDateTime(java.util.Date date) {
		synchronized (dfDateTime) {
			return dfDateTime.format(date);
		}
	}
	
	public static String formatDate(java.util.Date date) {
		synchronized (dfDate) {
			return dfDate.format(date);
		}
	}
	
	public static String formatTime(java.util.Date date) {
		synchronized (dfTime) {
			return dfTime.format(date);
		}
	}
	
	public static String formatGmtDate(java.util.Date date) {
		synchronized (dfGmtDateTime) {
			return dfGmtDateTime.format(date);
		}
	}
	
	public static Date parseDateTime(String text) {
		try {
			synchronized (dfDateTime) {
				return dfDateTime.parse(text);
			}
		} catch (ParseException e) {
			return null;
		}
	}
	
	public static Date parseDate(String text) {
		try {
			synchronized (dfDate) {
				return dfDate.parse(text);
			}
		} catch (ParseException e) {
			return null;
		}
	}
	
	public static Date parseTime(String text) {
		try {
			synchronized (dfTime) {
				return dfTime.parse(text);
			}
		} catch (ParseException e) {
			return null;
		}
	}
	
	public static void main(String[] args) throws Exception {
		if(args.length == 0)
			System.out.println("Usage: Crypt <string>");
		else if(args.length == 1){
			System.out.println("hex    " + toHex(args[0].getBytes("UTF-8")));
			System.out.println("base64 " + toBase64(args[0].getBytes("UTF-8")));
		}
	}

}
