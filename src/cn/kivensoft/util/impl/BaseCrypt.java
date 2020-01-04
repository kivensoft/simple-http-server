package cn.kivensoft.util.impl;

import java.security.SecureRandom;

public class BaseCrypt {

	// base64加密算法映射表
	private static final char base64_code[] = {
		'.', '/', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
		'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
		'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
		'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
		'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5',
		'6', '7', '8', '9'
	};

	// base64解密算法映射表
	private static final byte index_64[] = {
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, 0, 1, 54, 55,
		56, 57, 58, 59, 60, 61, 62, 63, -1, -1,
		-1, -1, -1, -1, -1, 2, 3, 4, 5, 6,
		7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
		17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,
		-1, -1, -1, -1, -1, -1, 28, 29, 30,
		31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
		41, 42, 43, 44, 45, 46, 47, 48, 49, 50,
		51, 52, 53, -1, -1, -1, -1, -1
	};

	protected SecureRandom random;

	public BaseCrypt() {
		random = new SecureRandom();
	}

	/**
	 * base64加密算法
	 * @param data	需要加密的字节数组
	 * @param len	字节数组长度
	 * @return	加密后的base64字符串
	 */
	protected static String encodeBase64(byte data[]) {
		int data_len = data.length, align_count = data_len / 3, mod_count = data_len % 3;
		int align_len = data_len - mod_count, str_len = align_count * 4;
		if (mod_count == 1) str_len += 2;
		else if (mod_count == 2) str_len += 3;

		char[] str = new char[str_len];
		int str_off = -1;

		for (int i = 0; i < align_len; i += 3) {
			int c1 = data[i] & 0xff;
			str[++str_off] = base64_code[(c1 >> 2) & 0x3f];

			c1 = (c1 & 0x03) << 4;

			int c2 = data[i + 1] & 0xff;
			c1 |= (c2 >> 4) & 0x0f;
			str[++str_off] = base64_code[c1 & 0x3f];

			c1 = (c2 & 0x0f) << 2;

			c2 = data[i + 2] & 0xff;
			c1 |= (c2 >> 6) & 0x03;
			str[++str_off] = base64_code[c1 & 0x3f];
			str[++str_off] = base64_code[c2 & 0x3f];
		}
		
		// 处理不对齐的字节数
		if (mod_count != 0) {
			int mod_off = align_len;
			int c1 = data[mod_off++] & 0xff;
			str[++str_off] = base64_code[(c1 >> 2) & 0x3f];
			c1 = (c1 & 0x03) << 4;
			if (mod_off >= data_len)
				str[++str_off] = base64_code[c1 & 0x3f];
			else {
				int c2 = data[mod_off] & 0xff;
				c1 |= (c2 >> 4) & 0x0f;
				str[++str_off] = base64_code[c1 & 0x3f];
				c1 = (c2 & 0x0f) << 2;
				str[++str_off] = base64_code[c1 & 0x3f];
			}
				
		}

		return new String(str);
	}

	/**
	 * 解码单个base64字符
	 * @param x	base64字符
	 * @return	解码后的值
	 */
	protected static byte char64(int x) {
		return (x < 0 || x > index_64.length) ? -1 : index_64[x];
	}

	/**
	 * 解码base64字符串
	 * @param text	base64字符串
	 * @return	解码后的字节数组
	 */
	protected static byte[] decodeBase64(String text) {
		int off = 0, slen = text.length(), olen = 0;
		int align_count = slen / 4, mod_count = slen % 4;
		int ret_len = align_count * 3;
		if (mod_count == 2) ret_len += 1;
		else if (mod_count == 3) ret_len += 2;
		byte[] ret = new byte[ret_len];
		byte c1, c2, c3, c4, o, ret_off = -1;

		while (off < slen - 1 && olen < slen) {
			c1 = char64(text.charAt(off++));
			c2 = char64(text.charAt(off++));
			if (c1 == -1 || c2 == -1) break;

			o = (byte)(c1 << 2);
			o |= (c2 & 0x30) >> 4;
			ret[++ret_off] = o;
			if (++olen >= slen || off >= slen) break;

			c3 = char64(text.charAt(off++));
			if (c3 == -1) break;
			o = (byte)((c2 & 0x0f) << 4);
			o |= (c3 & 0x3c) >> 2;
			ret[++ret_off] = o;
			if (++olen >= slen || off >= slen) break;

			c4 = char64(text.charAt(off++));
			o = (byte)((c3 & 0x03) << 6);
			o |= c4;
			ret[++ret_off] = o;
			++olen;
		}

		return ret;
	}

	/** 创建随机盐值
	 * @param keyLen
	 * @return
	 */
	protected byte[] gensalt(int keyLen) {
		byte[] ret = new byte[keyLen];
		random.nextBytes(ret);
		return ret;
	}
}
