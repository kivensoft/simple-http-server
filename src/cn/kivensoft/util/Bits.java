package cn.kivensoft.util;

import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

final public class Bits {
	
	public static boolean getBit(String permits, int index) {
		if (permits == null || index >= (permits.length() << 2)) return false;
		byte b = c2b(permits.charAt(index >> 2));
		return (b & (8 >> (index & 3))) != 0;
	}
	
	public static String toHexString(boolean[] booleans) {
		return booleans == null ? "" : toHexString(booleans.length, i -> booleans[i]);
	}
	
	public static String toHexString(int count, IntPredicate pred) {
		if (count == 0) return "";

		int blen = (count + 7) >> 3;
		char[] chars = new char[blen << 1];
		// 8倍数的位判断, 写入数组
		for (int i = 0, imax = blen - 1; i < imax; ++i) {
			byte b = 0;
			int index = i << 3;
			for (int j = 0; j < 8; ++j)
				if (pred.test(index + j)) b |= 0x80 >> j;
			int b1 = b >> 4 & 0xF, b2 = b & 0xF;
			chars[i << 1] = (char) (b1 < 10 ?  48 + b1 : 87 + b1);
			chars[(i << 1) + 1] = (char) (b2 < 10 ? 48 + b2 : 87 + b2);
		}
		
		// 剩余位数, 如果有, 则写入最后一个字节
		int surplus = count & 7;
		if (surplus > 0) {
			byte b = 0;
			int index = (blen - 1) << 3;
			for (int j = 0; j < surplus; ++j) 
				if (pred.test(index + j)) b |= 0x80 >> j;
			int b1 = b >> 4 & 0xF, b2 = b & 0xF;
			chars[(blen - 1) << 1] = (char) (b1 < 10 ?  48 + b1 : 87 + b1);
			chars[((blen - 1) << 1) + 1] = (char) (b2 < 10 ? 48 + b2 : 87 + b2);
		}
		
		// 从尾部开始查找为0的数据, 尾部为0的截断
		for (int i = chars.length - 1; i > 0; i -= 2) {
			if (chars[i] != '0' || chars[i - 1] != '0') {
				return new String(chars, 0, i + 1);
			}
		}

		return "";
	}
	
	public static boolean[] parseHexString(String permits) {
		int len = permits == null ? 0 : permits.length();
		boolean[] ret = new boolean[len << 2];
		parseHexString(permits, index -> ret[index] = true);
		return ret;
	}
	
	public static void parseHexString(String permits, IntConsumer consumer) {
		if (permits == null || permits.isEmpty()) return;
		for (int i = 0, imax = permits.length(); i < imax; ++i) {
			int index = i << 2;
			byte b = c2b(permits.charAt(i));
			if ((b & 8) != 0) consumer.accept(index);
			if ((b & 4) != 0) consumer.accept(index + 1);
			if ((b & 2) != 0) consumer.accept(index + 2);
			if ((b & 1) != 0) consumer.accept(index + 3);
		}
	}
	
	private static final byte c2b(char c) {
		return c >= '0' && c <= '9'
			? (byte)(c - 48)
				: (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f')
				? (byte)((c & 7) + 9) : 0;
	}

}
