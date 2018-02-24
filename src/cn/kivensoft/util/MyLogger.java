package cn.kivensoft.util;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;


/** 静态公共日志类，基于log4j 1.2版本实现，避免多个类创建日志对象
 * @author kiven lee
 * @version 1.0
 * @date 2017-11-20
 */
final public class MyLogger {
	public static enum LogLevel {trace, debug, info, warn, error};

	final static String FQCN = MyLogger.class.getName();
	final static Logger _logger = Logger.getLogger(MyLogger.class);
	
	protected static void log(Priority level, String msg, Throwable e) {
		_logger.log(FQCN, level, msg, e);
	}
	
	protected static boolean isEnabledFor(Priority level) {
		return _logger.isEnabledFor(level);
	}
	
	protected static Priority toLevel(LogLevel level) {
		Level v;
		switch (level) {
			case trace: v = Level.TRACE; break;
			case debug: v = Level.DEBUG; break;
			case info: v = Level.INFO; break;
			case warn: v = Level.WARN; break;
			case error: v = Level.ERROR; break;
			default:
				v = Level.OFF;
		}
		return v;
	}
	
	public static boolean isEnabledFor(LogLevel level) {
		return _logger.isEnabledFor(toLevel(level));
	}
	
	public static boolean isTraceEnabled() {
		return _logger.isTraceEnabled();
	}

	public static boolean isDebugEnabled() {
		return _logger.isDebugEnabled();
	}

	public static boolean isInfoEnabled() {
		return _logger.isInfoEnabled();
	}

	public static boolean isWarnEnabled() {
		return true;
	}

	public static boolean isErrorEnabled() {
		return true;
	}

	public static void log(LogLevel level, String msg) {
		Priority v = toLevel(level);
		if (isEnabledFor(v)) log(v, msg, null);
	}
	
	public static void log(LogLevel level, String msg, Object arg1) {
		Priority v = toLevel(level);
		if (isEnabledFor(v)) log(v, Fmt.fmt(msg, arg1), null);
	}
	
	public static void log(LogLevel level, String msg, Object arg1, Object arg2) {
		Priority v = toLevel(level);
		if (isEnabledFor(v)) log(v, Fmt.fmt(msg, arg1, arg2), null);
	}
	
	public static void log(LogLevel level, String msg, Object... args) {
		Priority v = toLevel(level);
		if (isEnabledFor(v)) log(v, Fmt.fmt(msg, args), null);
	}
	
	public static void log(LogLevel level, Throwable e) {
		Priority v = toLevel(level);
		if (isEnabledFor(v)) log(v, null, e);
	}

	public static void log(LogLevel level, Throwable e, String msg) {
		Priority v = toLevel(level);
		if (isEnabledFor(v)) log(v, msg, e);
	}
	
	public static void log(LogLevel level, Throwable e, String msg, Object arg1) {
		Priority v = toLevel(level);
		if (isEnabledFor(v)) log(v, Fmt.fmt(msg, arg1), e);
	}
	
	public static void log(LogLevel level, Throwable e, String msg, Object arg1, Object arg2) {
		Priority v = toLevel(level);
		if (isEnabledFor(v)) log(v, Fmt.fmt(msg, arg1, arg2), e);
	}
	
	public static void log(LogLevel level, Throwable e, String msg, Object... args) {
		Priority v = toLevel(level);
		if (isEnabledFor(v)) log(v, Fmt.fmt(msg, args), e);
	}

	public static void logJson(LogLevel level, String msg, Object...args) {
		Priority v = toLevel(level);
		if (isEnabledFor(v)) log(v, Fmt.fmtJson(msg, args), null);
	}
	
	public static void logJson(LogLevel level, Throwable e, String msg, Object...args) {
		Priority v = toLevel(level);
		if (isEnabledFor(v)) log(v, Fmt.fmtJson(msg, args), e);
	}

	public static void trace(String msg) {
		if (isTraceEnabled()) log(Level.TRACE, msg, null);
	}
	
	public static void trace(String msg, Object arg1) {
		if (isTraceEnabled()) log(Level.TRACE, Fmt.fmt(msg, arg1), null);
	}
	
	public static void trace(String msg, Object arg1, Object arg2) {
		if (isTraceEnabled()) log(Level.TRACE, Fmt.fmt(msg, arg1, arg2), null);
	}
	
	public static void trace(String msg, Object... args) {
		if (isTraceEnabled()) log(Level.TRACE, Fmt.fmt(msg, args), null);
	}
	
	public static void trace(Throwable e) {
		if (isTraceEnabled()) log(Level.TRACE, null, e);
	}

	public static void trace(Throwable e, String msg) {
		if (isTraceEnabled()) log(Level.TRACE, msg, e);
	}
	
	public static void trace(Throwable e, String msg, Object arg1) {
		if (isTraceEnabled()) log(Level.TRACE, Fmt.fmt(msg, arg1), e);
	}
	
	public static void trace(Throwable e, String msg, Object arg1, Object arg2) {
		if (isTraceEnabled()) log(Level.TRACE, Fmt.fmt(msg, arg1, arg2), e);
	}
	
	public static void trace(Throwable e, String msg, Object... args) {
		if (isTraceEnabled()) log(Level.TRACE, Fmt.fmt(msg, args), e);
	}

	public static void traceJson(String msg, Object...args) {
		if (isTraceEnabled()) log(Level.TRACE, Fmt.fmtJson(msg, args), null);
	}
	
	public static void traceJson(Throwable e, String msg, Object...args) {
		if (isTraceEnabled()) log(Level.TRACE, Fmt.fmtJson(msg, args), e);
	}

	public static void debug(String msg) {
		if (isDebugEnabled()) log(Level.DEBUG, msg, null);
	}
	
	public static void debug(String msg, Object arg1) {
		if (isDebugEnabled()) log(Level.DEBUG, Fmt.fmt(msg, arg1), null);
	}
	
	public static void debug(String msg, Object arg1, Object arg2) {
		if (isDebugEnabled()) log(Level.DEBUG, Fmt.fmt(msg, arg1, arg2), null);
	}
	
	public static void debug(String msg, Object... args) {
		if (isDebugEnabled()) log(Level.DEBUG, Fmt.fmt(msg, args), null);
	}
	
	public static void debug(Throwable e) {
		if (isDebugEnabled()) log(Level.DEBUG, null, e);
	}
	
	public static void debug(Throwable e, String msg) {
		if (isDebugEnabled()) log(Level.DEBUG, msg, e);
	}
	
	public static void debug(Throwable e, String msg, Object arg1) {
		if (isDebugEnabled()) log(Level.DEBUG, Fmt.fmt(msg, arg1), e);
	}
	
	public static void debug(Throwable e, String msg, Object arg1, Object arg2) {
		if (isDebugEnabled()) log(Level.DEBUG, Fmt.fmt(msg, arg1, arg2), e);
	}
	
	public static void debug(Throwable e, String msg, Object... args) {
		if (isDebugEnabled()) log(Level.DEBUG, Fmt.fmt(msg, args), e);
	}

	public static void debugJson(String msg, Object...args) {
		if (isDebugEnabled()) log(Level.DEBUG, Fmt.fmtJson(msg, args), null);
	}
	
	public static void debugJson(Throwable e, String msg, Object...args) {
		if (isDebugEnabled()) log(Level.DEBUG, Fmt.fmtJson(msg, args), e);
	}

	public static void info(String msg) {
		if (isInfoEnabled()) log(Level.INFO, msg, null);
	}

	public static void info(String msg, Object arg1) {
		if (isInfoEnabled()) log(Level.INFO, Fmt.fmt(msg, arg1), null);
	}
	
	public static void info(String msg, Object arg1, Object arg2) {
		if (isInfoEnabled()) log(Level.INFO, Fmt.fmt(msg, arg1, arg2), null);
	}
	
	public static void info(String msg, Object... args) {
		if (isInfoEnabled()) log(Level.INFO, Fmt.fmt(msg, args), null);
	}
	
	public static void info(Throwable e) {
		if (isInfoEnabled()) log(Level.INFO, null, e);
	}
	
	public static void info(Throwable e, String msg) {
		if (isInfoEnabled()) log(Level.INFO, msg, e);
	}
	
	public static void info(Throwable e, String msg, Object arg1) {
		if (isInfoEnabled()) log(Level.INFO, Fmt.fmt(msg, arg1), e);
	}
	
	public static void info(Throwable e, String msg, Object arg1, Object arg2) {
		if (isInfoEnabled()) log(Level.INFO, Fmt.fmt(msg, arg1, arg2), e);
	}
	
	public static void info(Throwable e, String msg, Object... args) {
		if (isInfoEnabled()) log(Level.INFO, Fmt.fmt(msg, args), e);
	}

	public static void infoJson(String msg, Object...args) {
		if (isInfoEnabled()) log(Level.INFO, Fmt.fmtJson(msg, args), null);
	}
	
	public static void infoJson(Throwable e, String msg, Object...args) {
		if (isInfoEnabled()) log(Level.INFO, Fmt.fmtJson(msg, args), e);
	}

	public static void warn(String msg) {
		if (isWarnEnabled()) log(Level.WARN, msg, null);
	}

	public static void warn(String msg, Object arg1) {
		if (isWarnEnabled()) log(Level.WARN, Fmt.fmt(msg, arg1), null);
	}
	
	public static void warn(String msg, Object arg1, Object arg2) {
		if (isWarnEnabled()) log(Level.WARN, Fmt.fmt(msg, arg1, arg2), null);
	}
	
	public static void warn(String msg, Object... args) {
		if (isWarnEnabled()) log(Level.WARN, Fmt.fmt(msg, args), null);
	}
	
	public static void warn(Throwable e) {
		if (isWarnEnabled()) log(Level.WARN, null, e);
	}
	
	public static void warn(Throwable e, String msg) {
		if (isWarnEnabled()) log(Level.WARN, msg, e);
	}
	
	public static void warn(Throwable e, String msg, Object arg1) {
		if (isWarnEnabled()) log(Level.WARN, Fmt.fmt(msg, arg1), e);
	}
	
	public static void warn(Throwable e, String msg, Object arg1, Object arg2) {
		if (isWarnEnabled()) log(Level.WARN, Fmt.fmt(msg, arg1, arg2), e);
	}
	
	public static void warn(Throwable e, String msg, Object... args) {
		if (isWarnEnabled()) log(Level.WARN, Fmt.fmt(msg, args), e);
	}

	public static void warnJson(String msg, Object...args) {
		if (isWarnEnabled()) log(Level.WARN, Fmt.fmtJson(msg, args), null);
	}
	
	public static void warnJson(Throwable e, String msg, Object...args) {
		if (isWarnEnabled()) log(Level.WARN, Fmt.fmtJson(msg, args), e);
	}

	public static void error(String msg) {
		if (isWarnEnabled()) log(Level.ERROR, msg, null);
	}

	public static void error(String msg, Object arg1) {
		if (isWarnEnabled()) log(Level.ERROR, Fmt.fmt(msg, arg1), null);
	}
	
	public static void error(String msg, Object arg1, Object arg2) {
		if (isWarnEnabled()) log(Level.ERROR, Fmt.fmt(msg, arg1, arg2), null);
	}
	
	public static void error(String msg, Object... args) {
		if (isWarnEnabled()) log(Level.ERROR, Fmt.fmt(msg, args), null);
	}
	
	public static void error(Throwable e) {
		if (isWarnEnabled()) log(Level.ERROR, null, e);
	}
	
	public static void error(Throwable e, String msg) {
		if (isWarnEnabled()) log(Level.ERROR, msg, e);
	}
	
	public static void error(Throwable e, String msg, Object arg1) {
		if (isWarnEnabled()) log(Level.ERROR, Fmt.fmt(msg, arg1), e);
	}
	
	public static void error(Throwable e, String msg, Object arg1, Object arg2) {
		if (isWarnEnabled()) log(Level.ERROR, Fmt.fmt(msg, arg1, arg2), e);
	}
	
	public static void error(Throwable e, String msg, Object... args) {
		if (isWarnEnabled()) log(Level.ERROR, Fmt.fmt(msg, args), e);
	}

	public static void errorJson(String msg, Object...args) {
		if (isErrorEnabled()) log(Level.ERROR, Fmt.fmtJson(msg, args), null);
	}
	
	public static void errorJson(Throwable e, String msg, Object...args) {
		if (isErrorEnabled()) log(Level.ERROR, Fmt.fmtJson(msg, args), e);
	}

}
