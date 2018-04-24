package cn.kivensoft.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.kivensoft.util.MyLogger;

public class Example {

	public static void main(String[] args) throws Exception {
		ExecutorService cachedExecutor = Executors.newCachedThreadPool();

		//加载日志配置文件
		Properties props = new Properties();
		props.put("log4j.rootLogger", "DEBUG, console");
		props.put("log4j.appender.console", "org.apache.log4j.ConsoleAppender");
		props.put("log4j.appender.console.layout", "org.apache.log4j.PatternLayout");
		props.put("log4j.appender.console.layout.ConversionPattern",
				"[%-d{MM-dd HH:mm:ss.SSS}] [%-5p] [%C{1}:%L] %m%n");
		try {
			Class.forName("org.apache.log4j.PropertyConfigurator")
				.getMethod("configure", Properties.class)
				.invoke(null, props);
		} catch (Exception e) {
			MyLogger.error(e, e.getMessage());
		}
		
		// 初始化web api服务器
		SimpleHttpServer httpServer = new SimpleHttpServer();
		httpServer.scanPackage(null, "cn.kivensoft.http", true);
		httpServer.start("SimpleHttpServer", 3000, cachedExecutor);
		MyLogger.info("{} start at {}", "SimpleHttpServer", 3000);
		
		shell("SimpleHttpServer> ", System.in, System.out);
		
		httpServer.stop();
		cachedExecutor.shutdown();
		MyLogger.info("SimpleHttpServer stop!");
	}

	public static void shell(String prompt, InputStream is, OutputStream os) throws IOException {
		PrintStream out = new PrintStream(os);
		out.print(prompt);
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    while(true) {
		    String cmd = reader.readLine();
		    if ("quit".equals(cmd)) break;
		    out.print(prompt);
	    }
	}
}
