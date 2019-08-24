package cn.kivensoft.util;

public class FastThrowable extends Throwable {
	private static final long serialVersionUID = 1L;

	public FastThrowable(String message) {
		super(message, null, false, false);
	}

}
