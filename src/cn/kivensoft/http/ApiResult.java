package cn.kivensoft.http;

/** web api 通用返回对象
 * @author kiven
 *
 */
public class ApiResult<T> {
	public static final int STATUS_OK = 0;
	public static final int STATUS_FAIL = -1;
	
	public int status;
	public String message;
	public T data;
	
	public ApiResult() {
		super();
	}

	public ApiResult(int status, String message, T data) {
		super();
		this.status = status;
		this.message = message;
		this.data = data;
	}
	
	public final static <T> ApiResult<T> success() {
		return new ApiResult<T>(0, null, null);
	}
	
	public final static <T> ApiResult<T> success(T data) {
		return new ApiResult<T>(0, null, data);
	}
	
	public final static <T> ApiResult<T> success(String message, T data) {
		return new ApiResult<T>(0, message, data);
	}
	
	public final static <T> ApiResult<T> error(int status, String message, T data) {
		return new ApiResult<T>(status, message, data);
	}
	
	public final static <T> ApiResult<T> error(int status, String message) {
		return new ApiResult<T>(status, message, null);
	}
	
	public final static <T> ApiResult<T> error(String message) {
		return new ApiResult<T>(-1, message, null);
	}

}
