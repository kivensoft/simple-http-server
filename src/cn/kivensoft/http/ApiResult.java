package cn.kivensoft.http;

/** web api 通用返回对象
 * @author kiven
 *
 */
public class ApiResult {
	public static final int STATUS_OK = 0;
	public static final int STATUS_FAIL = -1;
	
	public int status;
	public String message;
	public Object data;
	
	public ApiResult() {
		super();
	}

	public ApiResult(int status, String message, Object data) {
		super();
		this.status = status;
		this.message = message;
		this.data = data;
	}
	
	public static ApiResult success() {
		return new ApiResult(0, null, null);
	}
	
	public static ApiResult success(Object data) {
		return new ApiResult(0, null, data);
	}
	
	public static ApiResult success(String message, Object data) {
		return new ApiResult(0, message, data);
	}
	
	public static ApiResult error(int status, String message, Object data) {
		return new ApiResult(status, message, data);
	}
	
	public static ApiResult error(int status, String message) {
		return new ApiResult(status, message, null);
	}
	
	public static ApiResult error(String message) {
		return new ApiResult(-1, message, null);
	}

}
