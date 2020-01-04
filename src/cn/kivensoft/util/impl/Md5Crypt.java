package cn.kivensoft.util.impl;

import java.security.MessageDigest;

import cn.kivensoft.util.Crypt;
import cn.kivensoft.util.Fmt;
import cn.kivensoft.util.Strings;

public class Md5Crypt extends BaseCrypt implements Crypt {
	private static final int DEFAULT_SALT_LEN = 6;
	private static final String MAGIC = "$1$";


	public Md5Crypt() {
		super();
	}

	@Override
	public String encrypt(String passwd) {
		String salt = encodeBase64(gensalt(DEFAULT_SALT_LEN));
		try {
			return doencrypt(passwd, salt, MAGIC);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean verification(String passwd, String encryptString) {
		if (encryptString == null || encryptString.length() < 34
				|| encryptString.charAt(0) != MAGIC.charAt(0)
				|| encryptString.charAt(1) != MAGIC.charAt(1)
				|| encryptString.charAt(2) != MAGIC.charAt(2)
				|| encryptString.charAt(11) != MAGIC.charAt(0))
			return false;

		try {
			String enc = doencrypt(passwd, encryptString.substring(3, 11), MAGIC);
			return encryptString.equals(enc);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String doencrypt(String passwd, String salt, String magic) throws Exception {
		byte[] pwd_bs = Strings.getBytes(passwd);
		byte[] salt_bs = Strings.getBytes(salt);
		byte[] magic_bs = Strings.getBytes(MAGIC);

		MessageDigest ctx = MessageDigest.getInstance("md5");
		ctx.update(pwd_bs);
		ctx.update(magic_bs);
		ctx.update(salt_bs);

		MessageDigest ctx1 = MessageDigest.getInstance("md5");
		ctx1.update(pwd_bs);
		ctx1.update(salt_bs);
		ctx1.update(pwd_bs);
		byte[] finalState = ctx1.digest();

		for (int pl = pwd_bs.length; pl > 0; pl -= 16)
			ctx.update(finalState, 0, pl > 16 ? 16 : pl);

		for (int i = finalState.length - 1; i >= 0; --i)
			finalState[i] = 0;

		for (int i = pwd_bs.length; i != 0; i >>>= 1) {
			if ((i & 1) != 0) ctx.update(finalState, 0, 1);
			else ctx.update(pwd_bs, 0, 1);
		}

		finalState = ctx.digest();

		for (int i = 0; i < 1000; i++) {
			MessageDigest ctx2 = MessageDigest.getInstance("md5");

			if ((i & 1) != 0) ctx2.update(pwd_bs);
			else ctx2.update(finalState, 0, 16);

			if ((i % 3) != 0) ctx2.update(salt_bs);
			if ((i % 7) != 0) ctx2.update(pwd_bs);
			if ((i & 1) != 0) ctx2.update(finalState, 0, 16);
			else ctx2.update(pwd_bs);

			finalState = ctx2.digest();
		}

		String enc = encodeBase64(finalState);
		for (int i = finalState.length - 1; i >= 0; --i)
			finalState[i] = 0;
		return Fmt.concat(MAGIC, salt, MAGIC.substring(0, 1), enc);
	}

}
