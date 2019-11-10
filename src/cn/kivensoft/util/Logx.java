package cn.kivensoft.util;

import java.util.WeakHashMap;

import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

/** 扩展日志类，基于slf4j/logback实现，增强动态参数, 使用自带的高效格式化函数
 * @author kiven lee
 * @version 1.0
 * @date 2017-11-20
 */
final public class Logx {
	public static enum LogLevel {
		trace(LocationAwareLogger.TRACE_INT),
		debug(LocationAwareLogger.DEBUG_INT),
		info(LocationAwareLogger.INFO_INT),
		warn(LocationAwareLogger.WARN_INT),
		error(LocationAwareLogger.ERROR_INT);

		private int _level;
		private LogLevel(int level) { _level = level; }
		public int level() { return _level; }
	};

	private final static String FQCN = Logx.class.getName();
	private final static WeakHashMap<String, Logx> cache = new WeakHashMap<>();
	private final static Logx global = Logx.get(Logx.class.getName());

	private LocationAwareLogger logger;
	private boolean isDebug;

	private Logx() {}

	public static Logx get(Class<?> clazz) {
		return get(clazz.getName());
	}

	public static Logx get(String name) {
		Logx ret = cache.get(name);
		if (ret == null) {
			ret = new Logx();
			ret.logger = (LocationAwareLogger) LoggerFactory.getLogger(name);
			ret.isDebug = ret.logger.isDebugEnabled();
			synchronized (Logx.class) {
				Logx r = cache.get(name);
				if (r == null) cache.put(new String(name), ret);
			}
		}
		return ret;
	}

	private void _log(LogLevel level, String msg, Throwable e) {
		logger.log(null, FQCN, level.level(), msg, null, e);
	}

	public boolean isEnabledFor(LogLevel level) {
		switch (level) {
			case trace:
				return logger.isTraceEnabled();
			case debug:
				return logger.isDebugEnabled();
			case info:
				return logger.isInfoEnabled();
			case warn:
				return logger.isWarnEnabled();
			case error:
				return logger.isErrorEnabled();
			default:
				return false;
		}
	}

	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	public boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}

	public boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}

	public void log(LogLevel level, String msg) {
		if (isEnabledFor(level)) _log(level, msg, null);
	}

	public void log(LogLevel level, String msg, Object... args) {
		if (isEnabledFor(level)) _log(level, Fmt.fmt(msg, args), null);
	}

	public void log(LogLevel level, Throwable e) {
		if (isEnabledFor(level)) _log(level, e.getMessage(), e);
	}

	public void log(LogLevel level, Throwable e, String msg) {
		if (isEnabledFor(level)) _log(level, msg, e);
	}

	public void log(LogLevel level, Throwable e, String msg, Object... args) {
		if (isEnabledFor(level)) _log(level, Fmt.fmt(msg, args), e);
	}

	public void logJson(LogLevel level, String msg, Object... args) {
		if (isEnabledFor(level)) _log(level, Fmt.fmtJson(msg, args), null);
	}

	public void logJson(LogLevel level, Throwable e, String msg, Object... args) {
		if (isEnabledFor(level)) _log(level, Fmt.fmtJson(msg, args), e);
	}

	public void debug(String msg) {
		if (isDebug) _log(LogLevel.debug, msg, null);
	}

	public void debug(String msg, Object arg1) {
		if (isDebug) _log(LogLevel.debug, Fmt.fmt(msg, arg1), null);
	}

	public void debug(String msg, Object arg1, Object arg2) {
		if (isDebug) _log(LogLevel.debug, Fmt.fmt(msg, arg1, arg2), null);
	}

	public void debug(String msg, Object arg1, Object arg2, Object arg3) {
		if (isDebug) _log(LogLevel.debug, Fmt.fmt(msg, arg1, arg2, arg3), null);
	}

	public void debug(String msg, Object... args) {
		if (isDebug) _log(LogLevel.debug, Fmt.fmt(msg, args), null);
	}

	public void debug(Throwable e) {
		if (isDebug) _log(LogLevel.debug, null, e);
	}

	public void debug(Throwable e, String msg) {
		if (isDebug) _log(LogLevel.debug, msg, e);
	}

	public void debug(Throwable e, String msg, Object arg1) {
		if (isDebug) _log(LogLevel.debug, Fmt.fmt(msg, arg1), e);
	}

	public void debug(Throwable e, String msg, Object... args) {
		if (isDebug) _log(LogLevel.debug, Fmt.fmt(msg, args), e);
	}

	public void debugJson(String msg, Object... args) {
		if (isDebug) _log(LogLevel.debug, Fmt.fmtJson(msg, args), null);
	}

	public void debugJson(Throwable e, String msg, Object... args) {
		if (isDebug) _log(LogLevel.debug, Fmt.fmtJson(msg, args), e);
	}

	public void info(String msg) {
		if (logger.isInfoEnabled()) _log(LogLevel.info, msg, null);
	}

	public void info(String msg, Object... args) {
		if (logger.isInfoEnabled()) _log(LogLevel.info, Fmt.fmt(msg, args), null);
	}

	public void info(Throwable e) {
		if (logger.isInfoEnabled()) _log(LogLevel.info, null, e);
	}

	public void info(Throwable e, String msg) {
		if (logger.isInfoEnabled()) _log(LogLevel.info, msg, e);
	}

	public void info(Throwable e, String msg, Object... args) {
		if (logger.isInfoEnabled()) _log(LogLevel.info, Fmt.fmt(msg, args), e);
	}

	public void infoJson(String msg, Object... args) {
		if (logger.isInfoEnabled()) _log(LogLevel.info, Fmt.fmtJson(msg, args), null);
	}

	public void infoJson(Throwable e, String msg, Object... args) {
		if (logger.isInfoEnabled()) _log(LogLevel.info, Fmt.fmtJson(msg, args), e);
	}

	public void warn(String msg) {
		if (logger.isWarnEnabled()) _log(LogLevel.warn, msg, null);
	}

	public void warn(String msg, Object arg1) {
		if (logger.isWarnEnabled()) _log(LogLevel.warn, Fmt.fmt(msg, arg1), null);
	}

	public void warn(String msg, Object... args) {
		if (logger.isWarnEnabled()) _log(LogLevel.warn, Fmt.fmt(msg, args), null);
	}

	public void warn(Throwable e) {
		if (logger.isWarnEnabled()) _log(LogLevel.warn, null, e);
	}

	public void warn(Throwable e, String msg) {
		if (logger.isWarnEnabled()) _log(LogLevel.warn, msg, e);
	}

	public void warn(Throwable e, String msg, Object... args) {
		if (logger.isWarnEnabled()) _log(LogLevel.warn, Fmt.fmt(msg, args), e);
	}

	public void warnJson(String msg, Object... args) {
		if (logger.isWarnEnabled()) _log(LogLevel.warn, Fmt.fmtJson(msg, args), null);
	}

	public void warnJson(Throwable e, String msg, Object... args) {
		if (logger.isWarnEnabled()) _log(LogLevel.warn, Fmt.fmtJson(msg, args), e);
	}

	public void error(String msg) {
		if (isErrorEnabled()) _log(LogLevel.error, msg, null);
	}

	public void error(String msg, Object... args) {
		if (isErrorEnabled()) _log(LogLevel.error, Fmt.fmt(msg, args), null);
	}

	public void error(Throwable e) {
		if (isErrorEnabled()) _log(LogLevel.error, null, e);
	}

	public void error(Throwable e, String msg) {
		if (isErrorEnabled()) _log(LogLevel.error, msg, e);
	}

	public void error(Throwable e, String msg, Object... args) {
		if (isErrorEnabled()) _log(LogLevel.error, Fmt.fmt(msg, args), e);
	}

	public void errorJson(String msg, Object... args) {
		if (logger.isErrorEnabled()) _log(LogLevel.error, Fmt.fmtJson(msg, args), null);
	}

	public void errorJson(Throwable e, String msg, Object... args) {
		if (logger.isErrorEnabled()) _log(LogLevel.error, Fmt.fmtJson(msg, args), e);
	}

	public static void D(String msg) {
		if (global.isDebug) global._log(LogLevel.debug, msg, null);
	}

	public static void D(String msg, Object arg1) {
		if (global.isDebug) global._log(LogLevel.debug, Fmt.fmt(msg, arg1), null);
	}

	public static void D(String msg, Object arg1, Object arg2) {
		if (global.isDebug) global._log(LogLevel.debug, Fmt.fmt(msg, arg1, arg2), null);
	}

	public static void D(String msg, Object arg1, Object arg2, Object arg3) {
		if (global.isDebug) global._log(LogLevel.debug, Fmt.fmt(msg, arg1, arg2, arg3), null);
	}

	public static void D(String msg, Object... args) {
		if (global.isDebug) global._log(LogLevel.debug, Fmt.fmt(msg, args), null);
	}

	public static void D(Throwable e, String msg) {
		if (global.isDebug) global._log(LogLevel.debug, msg, e);
	}

	public static void D(Throwable e, String msg, Object... args) {
		if (global.isDebug) global._log(LogLevel.debug, Fmt.fmt(msg, args), e);
	}

	public static void DJ(String msg, Object... args) {
		if (global.isDebug) global._log(LogLevel.debug, Fmt.fmtJson(msg, args), null);
	}

	public static void DJ(Throwable e, String msg, Object... args) {
		if (global.isDebug) global._log(LogLevel.debug, Fmt.fmtJson(msg, args), e);
	}

	public static void I(String msg) {
		if (global.isInfoEnabled()) global._log(LogLevel.info, msg, null);
	}

	public static void I(String msg, Object... args) {
		if (global.isInfoEnabled()) global._log(LogLevel.info, Fmt.fmt(msg, args), null);
	}

	public static void I(Throwable e, String msg) {
		if (global.isInfoEnabled()) global._log(LogLevel.info, msg, e);
	}

	public static void I(Throwable e, String msg, Object... args) {
		if (global.isInfoEnabled()) global._log(LogLevel.info, Fmt.fmt(msg, args), e);
	}

	public static void IJ(String msg, Object... args) {
		if (global.isInfoEnabled()) global._log(LogLevel.info, Fmt.fmtJson(msg, args), null);
	}

	public static void IJ(Throwable e, String msg, Object... args) {
		if (global.isInfoEnabled()) global._log(LogLevel.info, Fmt.fmtJson(msg, args), e);
	}

	public static void W(String msg) {
		if (global.isWarnEnabled()) global._log(LogLevel.warn, msg, null);
	}

	public static void W(String msg, Object... args) {
		if (global.isWarnEnabled()) global._log(LogLevel.warn, Fmt.fmt(msg, args), null);
	}

	public static void W(Throwable e, String msg) {
		if (global.isWarnEnabled()) global._log(LogLevel.warn, msg, e);
	}

	public static void W(Throwable e, String msg, Object... args) {
		if (global.isWarnEnabled()) global._log(LogLevel.warn, Fmt.fmt(msg, args), e);
	}

	public static void WJ(String msg, Object... args) {
		if (global.isWarnEnabled()) global._log(LogLevel.warn, Fmt.fmtJson(msg, args), null);
	}

	public static void WJ(Throwable e, String msg, Object... args) {
		if (global.isWarnEnabled()) global._log(LogLevel.warn, Fmt.fmtJson(msg, args), e);
	}

	public static void E(String msg) {
		if (global.isErrorEnabled()) global._log(LogLevel.error, msg, null);
	}

	public static void E(String msg, Object... args) {
		if (global.isErrorEnabled()) global._log(LogLevel.error, Fmt.fmt(msg, args), null);
	}

	public static void E(Throwable e, String msg) {
		if (global.isErrorEnabled()) global._log(LogLevel.error, msg, e);
	}

	public static void E(Throwable e, String msg, Object... args) {
		if (global.isErrorEnabled()) global._log(LogLevel.error, Fmt.fmt(msg, args), e);
	}

	public static void EJ(String msg, Object... args) {
		if (global.isErrorEnabled()) global._log(LogLevel.error, Fmt.fmtJson(msg, args), null);
	}

	public static void EJ(Throwable e, String msg, Object... args) {
		if (global.isErrorEnabled()) global._log(LogLevel.error, Fmt.fmtJson(msg, args), e);
	}
}
