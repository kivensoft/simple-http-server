package cn.kivensoft.http;

/** web api 通用返回对象
 * @author kiven
 *
 */
public class ApiResult {
	private int code;
	private String msg;
	private Object data;
	
	public ApiResult() {
		super();
	}

	public ApiResult(int code, String msg, Object data) {
		super();
		this.code = code;
		this.msg = msg;
		this.data = data;
	}
	
	public static ApiResult success() {
		return new ApiResult(0, null, null);
	}
	
	public static ApiResult success(Object data) {
		return new ApiResult(0, null, data);
	}
	
	public static ApiResult success(String msg, Object data) {
		return new ApiResult(0, msg, data);
	}
	
	public static ApiResult error(int code, String msg, Object data) {
		return new ApiResult(code, msg, data);
	}
	
	public static ApiResult error(int code, String msg) {
		return new ApiResult(code, msg, null);
	}
	
	public static ApiResult error(String msg) {
		return new ApiResult(-1, msg, null);
	}

	public int getCode() {
		return code;
	}
	
	public void setCode(int code) {
		this.code = code;
	}
	
	public String getMsg() {
		return msg;
	}
	
	public void setMsg(String msg) {
		this.msg = msg;
	}
	
	public Object getData() {
		return data;
	}
	
	public void setData(Object data) {
		this.data = data;
	}
}
