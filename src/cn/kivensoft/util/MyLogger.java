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

	private final static String FQCN = MyLogger.class.getName();
	private final static Logger logger = Logger.getLogger(MyLogger.class);
	
	private static Priority toLevel(LogLevel level) {
		Level v;
		switch (level) {
			case trace: v = Level.TRACE; break;
			case debug: v = Level.DEBUG; break;
			case info: v = Level.INFO; break;
			case warn: v = Level.WARN; break;
			case error: v = Level.ERROR; break;
			default: v = Level.OFF;
		}
		return v;
	}
	
	public static String caller() {
		StackTraceElement[] stacks = new Throwable().getStackTrace();
		StackTraceElement s = stacks[0];
		if (s.getClassName().equals(MyLogger.class.getName())) s = stacks[1];
		int idx = s.getClassName().lastIndexOf('.');
		return Fmt.concat(s.getClassName().substring(idx + 1), ".", s.getMethodName());
	}
	
	public static boolean isEnabledFor(LogLevel level) {
		return logger.isEnabledFor(toLevel(level));
	}
	
	public static boolean isTraceEnabled() {
		return logger.isEnabledFor(Level.TRACE);
	}

	public static boolean isDebugEnabled() {
		return logger.isEnabledFor(Level.DEBUG);
	}

	public static boolean isInfoEnabled() {
		return logger.isEnabledFor(Level.INFO);
	}

	public static boolean isWarnEnabled() {
		return logger.isEnabledFor(Level.WARN);
	}

	public static boolean isErrorEnabled() {
		return logger.isEnabledFor(Level.ERROR);
	}

	public static void log(LogLevel level, String msg) {
		Priority v = toLevel(level);
		if (logger.isEnabledFor(v))
			logger.log(FQCN, v, msg, null);
	}
	
	public static void log(LogLevel level, String msg, Object arg1) {
		Priority v = toLevel(level);
		if (logger.isEnabledFor(v))
			logger.log(FQCN, v, Fmt.fmt(msg, arg1), null);
	}
	
	public static void log(LogLevel level, String msg, Object arg1, Object arg2) {
		Priority v = toLevel(level);
		if (logger.isEnabledFor(v))
			logger.log(FQCN, v, Fmt.fmt(msg, arg1, arg2), null);
	}
	
	public static void log(LogLevel level, String msg, Object arg1,
			Object arg2, Object arg3) {
		Priority v = toLevel(level);
		if (logger.isEnabledFor(v))
			logger.log(FQCN, v, Fmt.fmt(msg, arg1, arg2, arg3), null);
	}
	
	public static void log(LogLevel level, String msg, Object... args) {
		Priority v = toLevel(level);
		if (logger.isEnabledFor(v))
			logger.log(FQCN, v, Fmt.fmt(msg, args), null);
	}
	
	public static void log(LogLevel level, Throwable e) {
		Priority v = toLevel(level);
		if (logger.isEnabledFor(v))
			logger.log(FQCN, v, null, e);
	}

	public static void log(LogLevel level, Throwable e, String msg) {
		Priority v = toLevel(level);
		if (logger.isEnabledFor(v))
			logger.log(FQCN, v, msg, e);
	}
	
	public static void log(LogLevel level, Throwable e, String msg, Object arg1) {
		Priority v = toLevel(level);
		if (logger.isEnabledFor(v))
			logger.log(FQCN, v, Fmt.fmt(msg, arg1), e);
	}
	
	public static void log(LogLevel level, Throwable e, String msg,
			Object arg1, Object arg2) {
		Priority v = toLevel(level);
		if (logger.isEnabledFor(v))
			logger.log(FQCN, v, Fmt.fmt(msg, arg1, arg2), e);
	}
	
	public static void log(LogLevel level, Throwable e, String msg,
			Object arg1, Object arg2, Object arg3) {
		Priority v = toLevel(level);
		if (logger.isEnabledFor(v))
			logger.log(FQCN, v, Fmt.fmt(msg, arg1, arg2, arg3), e);
	}
	
	public static void log(LogLevel level, Throwable e, String msg, Object... args) {
		Priority v = toLevel(level);
		if (logger.isEnabledFor(v))
			logger.log(FQCN, v, Fmt.fmt(msg, args), e);
	}

	public static void logJson(LogLevel level, String msg, Object...args) {
		Priority v = toLevel(level);
		if (logger.isEnabledFor(v))
			logger.log(FQCN, v, Fmt.fmtJson(msg, args), null);
	}
	
	public static void logJson(LogLevel level, Throwable e, String msg, Object...args) {
		Priority v = toLevel(level);
		if (logger.isEnabledFor(v))
			logger.log(FQCN, v, Fmt.fmtJson(msg, args), e);
	}

	public static void trace(String msg) {
		if (logger.isEnabledFor(Level.TRACE))
			logger.log(FQCN, Level.TRACE, msg, null);
	}
	
	public static void trace(String msg, Object arg1) {
		if (logger.isEnabledFor(Level.TRACE))
			logger.log(FQCN, Level.TRACE, Fmt.fmt(msg, arg1), null);
	}
	
	public static void trace(String msg, Object arg1, Object arg2) {
		if (logger.isEnabledFor(Level.TRACE))
			logger.log(FQCN, Level.TRACE, Fmt.fmt(msg, arg1, arg2), null);
	}
	
	public static void trace(String msg, Object arg1, Object arg2, Object arg3) {
		if (logger.isEnabledFor(Level.TRACE))
			logger.log(FQCN, Level.TRACE, Fmt.fmt(msg, arg1, arg2, arg3), null);
	}
	
	public static void trace(String msg, Object... args) {
		if (logger.isEnabledFor(Level.TRACE))
			logger.log(FQCN, Level.TRACE, Fmt.fmt(msg, args), null);
	}
	
	public static void trace(Throwable e) {
		if (logger.isEnabledFor(Level.TRACE))
			logger.log(FQCN, Level.TRACE, null, e);
	}

	public static void trace(Throwable e, String msg) {
		if (logger.isEnabledFor(Level.TRACE))
			logger.log(FQCN, Level.TRACE, msg, e);
	}
	
	public static void trace(Throwable e, String msg, Object arg1) {
		if (logger.isEnabledFor(Level.TRACE))
			logger.log(FQCN, Level.TRACE, Fmt.fmt(msg, arg1), e);
	}
	
	public static void trace(Throwable e, String msg, Object arg1, Object arg2) {
		if (logger.isEnabledFor(Level.TRACE))
			logger.log(FQCN, Level.TRACE, Fmt.fmt(msg, arg1, arg2), e);
	}
	
	public static void trace(Throwable e, String msg, Object arg1,
			Object arg2, Object arg3) {
		if (logger.isEnabledFor(Level.TRACE))
			logger.log(FQCN, Level.TRACE, Fmt.fmt(msg, arg1, arg2, arg3), e);
	}
	
	public static void trace(Throwable e, String msg, Object... args) {
		if (logger.isEnabledFor(Level.TRACE))
			logger.log(FQCN, Level.TRACE, Fmt.fmt(msg, args), e);
	}

	public static void traceJson(String msg, Object...args) {
		if (logger.isEnabledFor(Level.TRACE))
			logger.log(FQCN, Level.TRACE, Fmt.fmtJson(msg, args), null);
	}
	
	public static void traceJson(Throwable e, String msg, Object...args) {
		if (logger.isEnabledFor(Level.TRACE))
			logger.log(FQCN, Level.TRACE, Fmt.fmtJson(msg, args), e);
	}

	public static void debug(String msg) {
		if (logger.isEnabledFor(Level.DEBUG))
			logger.log(FQCN, Level.DEBUG, msg, null);
	}
	
	public static void debug(String msg, Object arg1) {
		if (logger.isEnabledFor(Level.DEBUG))
			logger.log(FQCN, Level.DEBUG, Fmt.fmt(msg, arg1), null);
	}
	
	public static void debug(String msg, Object arg1, Object arg2) {
		if (logger.isEnabledFor(Level.DEBUG))
			logger.log(FQCN, Level.DEBUG, Fmt.fmt(msg, arg1, arg2), null);
	}
	
	public static void debug(String msg, Object arg1, Object arg2, Object arg3) {
		if (logger.isEnabledFor(Level.DEBUG))
			logger.log(FQCN, Level.DEBUG, Fmt.fmt(msg, arg1, arg2, arg3), null);
	}
	
	public static void debug(String msg, Object... args) {
		if (logger.isEnabledFor(Level.DEBUG))
			logger.log(FQCN, Level.DEBUG, Fmt.fmt(msg, args), null);
	}
	
	public static void debug(Throwable e) {
		if (logger.isEnabledFor(Level.DEBUG))
			logger.log(FQCN, Level.DEBUG, null, e);
	}
	
	public static void debug(Throwable e, String msg) {
		if (logger.isEnabledFor(Level.DEBUG))
			logger.log(FQCN, Level.DEBUG, msg, e);
	}
	
	public static void debug(Throwable e, String msg, Object arg1) {
		if (logger.isEnabledFor(Level.DEBUG))
			logger.log(FQCN, Level.DEBUG, Fmt.fmt(msg, arg1), e);
	}
	
	public static void debug(Throwable e, String msg, Object arg1, Object arg2) {
		if (logger.isEnabledFor(Level.DEBUG))
			logger.log(FQCN, Level.DEBUG, Fmt.fmt(msg, arg1, arg2), e);
	}
	
	public static void debug(Throwable e, String msg, Object arg1,
			Object arg2, Object arg3) {
		if (logger.isEnabledFor(Level.DEBUG))
			logger.log(FQCN, Level.DEBUG, Fmt.fmt(msg, arg1, arg2, arg3), e);
	}
	
	public static void debug(Throwable e, String msg, Object... args) {
		if (logger.isEnabledFor(Level.DEBUG))
			logger.log(FQCN, Level.DEBUG, Fmt.fmt(msg, args), e);
	}

	public static void debugJson(String msg, Object...args) {
		if (logger.isEnabledFor(Level.DEBUG))
			logger.log(FQCN, Level.DEBUG, Fmt.fmtJson(msg, args), null);
	}
	
	public static void debugJson(Throwable e, String msg, Object...args) {
		if (logger.isEnabledFor(Level.DEBUG))
			logger.log(FQCN, Level.DEBUG, Fmt.fmtJson(msg, args), e);
	}

	public static void info(String msg) {
		if (logger.isEnabledFor(Level.INFO))
			logger.log(FQCN, Level.INFO, msg, null);
	}

	public static void info(String msg, Object arg1) {
		if (logger.isEnabledFor(Level.INFO))
			logger.log(FQCN, Level.INFO, Fmt.fmt(msg, arg1), null);
	}
	
	public static void info(String msg, Object arg1, Object arg2) {
		if (logger.isEnabledFor(Level.INFO))
			logger.log(FQCN, Level.INFO, Fmt.fmt(msg, arg1, arg2), null);
	}

	public static void info(String msg, Object arg1, Object arg2, Object arg3) {
		if (logger.isEnabledFor(Level.INFO))
			logger.log(FQCN, Level.INFO, Fmt.fmt(msg, arg1, arg2, arg3), null);
	}
	
	public static void info(String msg, Object... args) {
		if (logger.isEnabledFor(Level.INFO))
			logger.log(FQCN, Level.INFO, Fmt.fmt(msg, args), null);
	}
	
	public static void info(Throwable e) {
		if (logger.isEnabledFor(Level.INFO))
			logger.log(FQCN, Level.INFO, null, e);
	}
	
	public static void info(Throwable e, String msg) {
		if (logger.isEnabledFor(Level.INFO))
			logger.log(FQCN, Level.INFO, msg, e);
	}
	
	public static void info(Throwable e, String msg, Object arg1) {
		if (logger.isEnabledFor(Level.INFO))
			logger.log(FQCN, Level.INFO, Fmt.fmt(msg, arg1), e);
	}
	
	public static void info(Throwable e, String msg, Object arg1, Object arg2) {
		if (logger.isEnabledFor(Level.INFO))
			logger.log(FQCN, Level.INFO, Fmt.fmt(msg, arg1, arg2), e);
	}
	
	public static void info(Throwable e, String msg, Object arg1,
			Object arg2, Object arg3) {
		if (logger.isEnabledFor(Level.INFO))
			logger.log(FQCN, Level.INFO, Fmt.fmt(msg, arg1, arg2, arg3), e);
	}
	
	public static void info(Throwable e, String msg, Object... args) {
		if (logger.isEnabledFor(Level.INFO))
			logger.log(FQCN, Level.INFO, Fmt.fmt(msg, args), e);
	}

	public static void infoJson(String msg, Object...args) {
		if (logger.isEnabledFor(Level.INFO))
			logger.log(FQCN, Level.INFO, Fmt.fmtJson(msg, args), null);
	}
	
	public static void infoJson(Throwable e, String msg, Object...args) {
		if (logger.isEnabledFor(Level.INFO))
			logger.log(FQCN, Level.INFO, Fmt.fmtJson(msg, args), e);
	}

	public static void warn(String msg) {
		if (logger.isEnabledFor(Level.WARN))
			logger.log(FQCN, Level.WARN, msg, null);
	}

	public static void warn(String msg, Object arg1) {
		if (logger.isEnabledFor(Level.WARN))
			logger.log(FQCN, Level.WARN, Fmt.fmt(msg, arg1), null);
	}
	
	public static void warn(String msg, Object arg1, Object arg2) {
		if (logger.isEnabledFor(Level.WARN))
			logger.log(FQCN, Level.WARN, Fmt.fmt(msg, arg1, arg2), null);
	}
	
	public static void warn(String msg, Object arg1, Object arg2, Object arg3) {
		if (logger.isEnabledFor(Level.WARN))
			logger.log(FQCN, Level.WARN, Fmt.fmt(msg, arg1, arg2, arg3), null);
	}
	
	public static void warn(String msg, Object... args) {
		if (logger.isEnabledFor(Level.WARN))
			logger.log(FQCN, Level.WARN, Fmt.fmt(msg, args), null);
	}
	
	public static void warn(Throwable e) {
		if (logger.isEnabledFor(Level.WARN))
			logger.log(FQCN, Level.WARN, null, e);
	}
	
	public static void warn(Throwable e, String msg) {
		if (logger.isEnabledFor(Level.WARN))
			logger.log(FQCN, Level.WARN, msg, e);
	}
	
	public static void warn(Throwable e, String msg, Object arg1) {
		if (logger.isEnabledFor(Level.WARN))
			logger.log(FQCN, Level.WARN, Fmt.fmt(msg, arg1), e);
	}
	
	public static void warn(Throwable e, String msg, Object arg1, Object arg2) {
		if (logger.isEnabledFor(Level.WARN))
			logger.log(FQCN, Level.WARN, Fmt.fmt(msg, arg1, arg2), e);
	}
	
	public static void warn(Throwable e, String msg, Object arg1,
			Object arg2, Object arg3) {
		if (logger.isEnabledFor(Level.WARN))
			logger.log(FQCN, Level.WARN, Fmt.fmt(msg, arg1, arg2, arg3), e);
	}
	
	public static void warn(Throwable e, String msg, Object... args) {
		if (logger.isEnabledFor(Level.WARN))
			logger.log(FQCN, Level.WARN, Fmt.fmt(msg, args), e);
	}

	public static void warnJson(String msg, Object...args) {
		if (logger.isEnabledFor(Level.WARN))
			logger.log(FQCN, Level.WARN, Fmt.fmtJson(msg, args), null);
	}
	
	public static void warnJson(Throwable e, String msg, Object...args) {
		if (logger.isEnabledFor(Level.WARN))
			logger.log(FQCN, Level.WARN, Fmt.fmtJson(msg, args), e);
	}

	public static void error(String msg) {
		if (logger.isEnabledFor(Level.ERROR))
			logger.log(FQCN, Level.ERROR, msg, null);
	}

	public static void error(String msg, Object arg1) {
		if (logger.isEnabledFor(Level.ERROR))
			logger.log(FQCN, Level.ERROR, Fmt.fmt(msg, arg1), null);
	}
	
	public static void error(String msg, Object arg1, Object arg2) {
		if (logger.isEnabledFor(Level.ERROR))
			logger.log(FQCN, Level.ERROR, Fmt.fmt(msg, arg1, arg2), null);
	}
	
	public static void error(String msg, Object arg1, Object arg2, Object arg3) {
		if (logger.isEnabledFor(Level.ERROR))
			logger.log(FQCN, Level.ERROR, Fmt.fmt(msg, arg1, arg2, arg3), null);
	}
	
	public static void error(String msg, Object... args) {
		if (logger.isEnabledFor(Level.ERROR))
			logger.log(FQCN, Level.ERROR, Fmt.fmt(msg, args), null);
	}
	
	public static void error(Throwable e) {
		if (logger.isEnabledFor(Level.ERROR))
			logger.log(FQCN, Level.ERROR, null, e);
	}
	
	public static void error(Throwable e, String msg) {
		if (logger.isEnabledFor(Level.ERROR))
			logger.log(FQCN, Level.ERROR, msg, e);
	}
	
	public static void error(Throwable e, String msg, Object arg1) {
		if (logger.isEnabledFor(Level.ERROR))
			logger.log(FQCN, Level.ERROR, Fmt.fmt(msg, arg1), e);
	}
	
	public static void error(Throwable e, String msg, Object arg1, Object arg2) {
		if (logger.isEnabledFor(Level.ERROR))
			logger.log(FQCN, Level.ERROR, Fmt.fmt(msg, arg1, arg2), e);
	}
	
	public static void error(Throwable e, String msg, Object arg1,
			Object arg2, Object arg3) {
		if (logger.isEnabledFor(Level.ERROR))
			logger.log(FQCN, Level.ERROR, Fmt.fmt(msg, arg1, arg2, arg3), e);
	}
	
	public static void error(Throwable e, String msg, Object... args) {
		if (logger.isEnabledFor(Level.ERROR))
			logger.log(FQCN, Level.ERROR, Fmt.fmt(msg, args), e);
	}

	public static void errorJson(String msg, Object...args) {
		if (logger.isEnabledFor(Level.ERROR))
			logger.log(FQCN, Level.ERROR, Fmt.fmtJson(msg, args), null);
	}
	
	public static void errorJson(Throwable e, String msg, Object...args) {
		if (logger.isEnabledFor(Level.ERROR))
			logger.log(FQCN, Level.ERROR, Fmt.fmtJson(msg, args), e);
	}

}
