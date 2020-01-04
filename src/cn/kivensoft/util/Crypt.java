package cn.kivensoft.util;

import cn.kivensoft.util.impl.BCrypt;
import cn.kivensoft.util.impl.Md5Crypt;

public interface Crypt {
	enum CryptType { MD5, BCrypt }

	static Crypt create(CryptType cryptType) {
		switch (cryptType) {
			case MD5: return new Md5Crypt();
			case BCrypt: return new BCrypt();
			default: return new Md5Crypt();
		}
	}

	String encrypt(String passwd);
	boolean verification(String passwd, String encryptString);
}
