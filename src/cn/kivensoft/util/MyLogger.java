package cn.kivensoft.util;

import java.util.WeakHashMap;

import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

/** 静态公共日志类，基于slf4j实现，避免多个类创建日志对象
 * @author kiven lee
 * @version 1.0
 * @date 2017-11-20
 */
final public class MyLogger {
	public static enum LogLevel {
		trace(LocationAwareLogger.TRACE_INT),
		debug(LocationAwareLogger.DEBUG_INT),
		info(LocationAwareLogger.INFO_INT),
		warn(LocationAwareLogger.WARN_INT),
		error(LocationAwareLogger.ERROR_INT);

		private int _level;
		private LogLevel(int level) {
			_level = level;
		}
		public int level() {
			return _level;
		}
	};

	private final static String FQCN = MyLogger.class.getName();
	private final static WeakHashMap<String, MyLogger> cache = new WeakHashMap<>();

	private LocationAwareLogger logger;

	private MyLogger() {}

	public static MyLogger get(Class<?> clazz) {
		return get(clazz.getName());
	}

	private void _log(LogLevel level, String msg, Throwable e) {
		logger.log(null, FQCN, level.level(), msg, null, e);
	}

	public static MyLogger get(String name) {
		MyLogger ret = cache.get(name);
		if (ret == null) {
			ret = new MyLogger();
			ret.logger = (LocationAwareLogger) LoggerFactory.getLogger(name);
			cache.put(new String(name), ret);
		}
		return ret;
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

	public boolean isTraceEnabled() {
		return logger.isTraceEnabled();
	}

	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	public boolean isWarnEnabled() {
		return true;
	}

	public boolean isErrorEnabled() {
		return true;
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

	public void logJson(LogLevel level, String msg, Object...args) {
		if (isEnabledFor(level)) _log(level, Fmt.fmtJson(msg, args), null);
	}

	public void logJson(LogLevel level, Throwable e, String msg, Object...args) {
		if (isEnabledFor(level)) _log(level, Fmt.fmtJson(msg, args), e);
	}

	public void trace(String msg) {
		if (logger.isTraceEnabled()) _log(LogLevel.trace, msg, null);
	}

	public void trace(String msg, Object... args) {
		if (logger.isTraceEnabled()) _log(LogLevel.trace, Fmt.fmt(msg, args), null);
	}

	public void trace(Throwable e) {
		if (logger.isTraceEnabled()) _log(LogLevel.trace, null, e);
	}

	public void trace(Throwable e, String msg) {
		if (logger.isTraceEnabled()) _log(LogLevel.trace, msg, e);
	}

	public void trace(Throwable e, String msg, Object... args) {
		if (logger.isTraceEnabled()) _log(LogLevel.trace, Fmt.fmt(msg, args), e);
	}

	public void traceJson(String msg, Object...args) {
		if (logger.isTraceEnabled()) _log(LogLevel.trace, Fmt.fmtJson(msg, args), null);
	}

	public void traceJson(Throwable e, String msg, Object...args) {
		if (logger.isTraceEnabled()) _log(LogLevel.trace, Fmt.fmtJson(msg, args), e);
	}

	public void debug(String msg) {
		if (logger.isDebugEnabled()) _log(LogLevel.debug, msg, null);
	}

	public void debug(String msg, Object arg1) {
		if (logger.isDebugEnabled()) _log(LogLevel.debug, Fmt.fmt(msg, arg1), null);
	}

	public void debug(String msg, Object arg1, Object arg2) {
		if (logger.isDebugEnabled()) _log(LogLevel.debug, Fmt.fmt(msg, arg1, arg2), null);
	}

	public void debug(String msg, Object... args) {
		if (logger.isDebugEnabled()) _log(LogLevel.debug, Fmt.fmt(msg, args), null);
	}

	public void debug(Throwable e) {
		if (logger.isDebugEnabled()) _log(LogLevel.debug, null, e);
	}

	public void debug(Throwable e, String msg) {
		if (logger.isDebugEnabled()) _log(LogLevel.debug, msg, e);
	}

	public void debug(Throwable e, String msg, Object arg1) {
		if (logger.isDebugEnabled()) _log(LogLevel.debug, Fmt.fmt(msg, arg1), e);
	}

	public void debug(Throwable e, String msg, Object arg1, Object arg2) {
		if (logger.isDebugEnabled()) _log(LogLevel.debug, Fmt.fmt(msg, arg1, arg2), e);
	}

	public void debug(Throwable e, String msg, Object... args) {
		if (logger.isDebugEnabled()) _log(LogLevel.debug, Fmt.fmt(msg, args), e);
	}

	public void debugJson(String msg, Object...args) {
		if (logger.isDebugEnabled()) _log(LogLevel.debug, Fmt.fmtJson(msg, args), null);
	}

	public void debugJson(Throwable e, String msg, Object...args) {
		if (logger.isDebugEnabled()) _log(LogLevel.debug, Fmt.fmtJson(msg, args), e);
	}

	public void info(String msg) {
		if (logger.isInfoEnabled()) _log(LogLevel.info, msg, null);
	}

	public void info(String msg, Object arg1) {
		if (logger.isInfoEnabled()) _log(LogLevel.info, Fmt.fmt(msg, arg1), null);
	}

	public void info(String msg, Object arg1, Object arg2) {
		if (logger.isInfoEnabled()) _log(LogLevel.info, Fmt.fmt(msg, arg1, arg2), null);
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

	public void info(Throwable e, String msg, Object arg1) {
		if (logger.isInfoEnabled()) _log(LogLevel.info, Fmt.fmt(msg, arg1), e);
	}

	public void info(Throwable e, String msg, Object arg1, Object arg2) {
		if (logger.isInfoEnabled()) _log(LogLevel.info, Fmt.fmt(msg, arg1, arg2), e);
	}

	public void info(Throwable e, String msg, Object... args) {
		if (logger.isInfoEnabled()) _log(LogLevel.info, Fmt.fmt(msg, args), e);
	}

	public void infoJson(String msg, Object...args) {
		if (logger.isInfoEnabled()) _log(LogLevel.info, Fmt.fmtJson(msg, args), null);
	}

	public void infoJson(Throwable e, String msg, Object...args) {
		if (logger.isInfoEnabled()) _log(LogLevel.info, Fmt.fmtJson(msg, args), e);
	}

	public void warn(String msg) {
		if (logger.isWarnEnabled()) _log(LogLevel.warn, msg, null);
	}

	public void warn(String msg, Object arg1) {
		if (logger.isWarnEnabled()) _log(LogLevel.warn, Fmt.fmt(msg, arg1), null);
	}

	public void warn(String msg, Object arg1, Object arg2) {
		if (logger.isWarnEnabled()) _log(LogLevel.warn, Fmt.fmt(msg, arg1, arg2), null);
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

	public void warn(Throwable e, String msg, Object arg1) {
		if (logger.isWarnEnabled()) _log(LogLevel.warn, Fmt.fmt(msg, arg1), e);
	}

	public void warn(Throwable e, String msg, Object arg1, Object arg2) {
		if (logger.isWarnEnabled()) _log(LogLevel.warn, Fmt.fmt(msg, arg1, arg2), e);
	}

	public void warn(Throwable e, String msg, Object... args) {
		if (logger.isWarnEnabled()) _log(LogLevel.warn, Fmt.fmt(msg, args), e);
	}

	public void warnJson(String msg, Object...args) {
		if (logger.isWarnEnabled()) _log(LogLevel.warn, Fmt.fmtJson(msg, args), null);
	}

	public void warnJson(Throwable e, String msg, Object...args) {
		if (logger.isWarnEnabled()) _log(LogLevel.warn, Fmt.fmtJson(msg, args), e);
	}

	public void error(String msg) {
		if (isWarnEnabled()) _log(LogLevel.error, msg, null);
	}

	public void error(String msg, Object... args) {
		if (isWarnEnabled()) _log(LogLevel.error, Fmt.fmt(msg, args), null);
	}

	public void error(Throwable e) {
		if (isWarnEnabled()) _log(LogLevel.error, null, e);
	}

	public void error(Throwable e, String msg) {
		if (isWarnEnabled()) _log(LogLevel.error, msg, e);
	}

	public void error(Throwable e, String msg, Object... args) {
		if (isWarnEnabled()) _log(LogLevel.error, Fmt.fmt(msg, args), e);
	}

	public void errorJson(String msg, Object...args) {
		if (logger.isErrorEnabled()) _log(LogLevel.error, Fmt.fmtJson(msg, args), null);
	}

	public void errorJson(Throwable e, String msg, Object...args) {
		if (logger.isErrorEnabled()) _log(LogLevel.error, Fmt.fmtJson(msg, args), e);
	}

}
