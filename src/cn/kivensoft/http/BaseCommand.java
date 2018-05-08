package cn.kivensoft.http;

import java.util.function.Function;

import cn.kivensoft.util.Fmt;
import cn.kivensoft.util.MyLogger;

abstract public class BaseCommand<T, R> implements Function<T, R> {
	
	/** 输入参数校验，校验参数必填项，输入范围等
	 * @param request 输入参数
	 * @return null: 没有错误, 其他: 错误信息
	 */
	abstract protected String onVerify(T request);

	/** 执行命令处理
	 * @param req 输入参数
	 * @return 返回值
	 * @throws Exception
	 */
	abstract protected R onExecute(T req) throws Exception;
	
	/** 错误回调函数，生成错误返回的实例
	 * @param code 错误码
	 * @param msg 错误信息
	 * @return 返回R对象实例
	 */
	abstract protected R onError(int code, String msg);
	
	/** 判断返回值是否错误类型
	 * @param value 返回值
	 * @return true表示错误，false无错
	 */
	abstract protected boolean isError(R value);
	
	/** 发生异常时调用 */
	abstract protected void onException(Exception ex);
	
	/** 不管是否发生都调用 */
	abstract protected void onFinally();
	
	/** 执行调用失败时调用 */
	abstract protected void onFail(R value) throws Exception;
	
	/** 执行调用成功时调用 */
	abstract protected void onSuccess(R value) throws Exception;
	
	/** 应用程序调用，处理调用请求函数 */
	@Override
	final public R apply(T req) {
		//校验参数有效性
		String err = onVerify(req);
		if (err != null) {
			onFinally();
			return error("请求参数无效");
		}

		try {
			R ret = onExecute(req);
			if (ret != null && isError(ret)) onFail(ret);
			else onSuccess(ret);
			return ret;
		} catch (Exception e) {
			onException(e);
			MyLogger.error(e, "未知错误");
			return error("未知错误");
		} finally {
			onFinally();
		}
	}
	
	final protected R error(String fmt, Object... args) {
		String msg = args.length == 0 ? fmt : Fmt.fmt(fmt, args);
		MyLogger.error(msg);
		return onError(-1, msg);
	}

	final protected R error(Throwable e, String fmt, Object... args) {
		String msg = args.length == 0 ? fmt : Fmt.fmt(fmt, args);
		MyLogger.error(e, msg);
		return onError(-1, msg);
	}
	
}
