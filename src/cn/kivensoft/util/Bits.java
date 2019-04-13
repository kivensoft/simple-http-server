package cn.kivensoft.util;

import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

final public class Bits {
	private static final int BYTE_BIT_COUNT = 8;
	private static final int BYTE_OFFSET = 3;
	
	private static final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
			'9', 'a', 'b', 'c', 'd', 'e', 'f'};
	private static final byte[] BYTE_BITS = { (byte)0x80, 0x40, 0x20, 0x10, 0x8, 0x4, 0x2, 0x1 };
	
	public static boolean getBit(String permits, int index) {
		if (permits == null || index >= (permits.length() << 2)) return false;
		byte b = c2b(permits.charAt(index >> 2));
		return (b & BYTE_BITS[4 + (index & 3)] & 0xf) != 0;
	}
	
	public static String toBytesString(boolean[] booleans) {
		return booleans == null ? "" : toBytesString(booleans.length, i -> booleans[i]);
	}
	
	public static String toBytesString(int count, IntPredicate predicate) {
		int blen = (count + BYTE_BIT_COUNT - 1) >> BYTE_OFFSET;
		byte[] bytes = new byte[blen];
		// 8倍数的位判断, 写入数组
		for (int i = 0, imax = blen - 1; i < imax; ++i) {
			byte b = 0;
			int index = i << BYTE_OFFSET;
			for (int j = 0; j < 8; ++j)
				if (predicate.test(index + j)) b |= BYTE_BITS[j];
			bytes[i] = b;
		}
		
		// 剩余位数, 如果有, 则写入最后一个字节
		int surplus = count & (BYTE_BIT_COUNT - 1);
		if (surplus > 0) {
			byte b = 0;
			int index = (blen - 1) << BYTE_OFFSET;
			for (int j = 0; j < surplus; ++j) 
				if (predicate.test(index + j)) b |= BYTE_BITS[j];
			bytes[blen - 1] = b;
		}
		
		// 从尾部开始查找为0的数据, 尾部为0的截断
		int endPos = blen - 1;
		while(endPos >= 0 && bytes[endPos] == 0) --endPos;

		char[] chars = new char[(endPos + 1) << 1];
		for (int i = 0; i <= endPos; ++i) {
			byte b = bytes[i];
			chars[i << 1] = HEX[b >>> 4 & 0xF];
			chars[(i << 1) + 1] = HEX[b & 0xF];
		}

		return new String(chars);
	}
	
	public static boolean[] toBooleans(String permits) {
		int len = permits == null ? 0 : permits.length();
		boolean[] ret = new boolean[len << 2];
		transString(permits, index -> ret[index] = true);
		return ret;
	}
	
	public static void transString(String permits, IntConsumer consumer) {
		if (permits == null || permits.isEmpty()) return;
		for (int i = 0, imax = permits.length(); i < imax; ++i) {
			int index = i << 2;
			byte b = c2b(permits.charAt(i));
			if ((b & BYTE_BITS[4] &0xF) != 0) consumer.accept(index);
			if ((b & BYTE_BITS[5] &0xF) != 0) consumer.accept(index + 1);
			if ((b & BYTE_BITS[6] &0xF) != 0) consumer.accept(index + 2);
			if ((b & BYTE_BITS[7] &0xF) != 0) consumer.accept(index + 3);
		}
	}
	
	private static final byte c2b(char c) {
		byte b;
		if (c >= '0' && c <= '9') b = (byte)(c - 48);
		else if (c >= 'A' && c <= 'F') b = (byte)(c - 55);
		else if (c >= 'a' && c <= 'f') b = (byte)(c - 87);
		else b = 0;
		return b;
	}

}
