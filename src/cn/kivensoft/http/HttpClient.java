package cn.kivensoft.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import cn.kivensoft.util.Fmt;
import cn.kivensoft.util.Langs;

public class HttpClient {
	private static final String UTF8 = "UTF-8";

	@FunctionalInterface
	public static interface OnRead {
		void accept(InputStream in) throws IOException;
	}
	
	@FunctionalInterface
	public static interface OnWrite {
		void accept(OutputStream out) throws IOException;
	}
	
	/** get请求访问http的api函数
	 * @param url 访问地址
	 * @param param URL参数
	 * @param headers 请求头部定制内容
	 * @param ResultClass 返回结果声明类
	 * @return
	 */
	public static <T> T get(String url, Map<String, Object> param,
			Map<String, String> headers, Class<T> ResultClass) throws IOException {
		return JSON.parseObject(get(url, param, headers), ResultClass);
	}

	/** get请求访问http的api函数
	 * @param url 访问地址
	 * @param param URL参数
	 * @param headers 请求头部定制内容
	 * @param ResultClass 返回结果声明类
	 * @return
	 */
	public static <T> T get(String url, Object param, Map<String, String> headers,
			Class<T> ResultClass) throws IOException {
		return JSON.parseObject(get(makeUrl(url, param), headers), ResultClass);
	}

	/** get请求访问http的api函数
	 * @param url 访问地址
	 * @param param URL参数
	 * @param headers 请求头部定制内容
	 * @return 返回的文本
	 * @throws IOException
	 */
	public static String get(String url, Map<String, Object> param, Map<String, String> headers) throws IOException {
		return get(makeUrl(url, param), headers);
	}

	/** get请求访问http的api函数
	 * @param url 访问地址
	 * @param param URL参数
	 * @param headers 请求头部定制内容
	 * @return 返回的文本
	 * @throws IOException
	 */
	public static String get(String url, Object param, Map<String, String> headers) throws IOException {
		return get(makeUrl(url, param), headers);
	}

	/** get请求访问http的api函数
	 * @param url 访问地址
	 * @param headers 请求头部内容
	 * @return 请求结果文本内容
	 */
	public static String get(String url, Map<String, String> headers) throws IOException {
		Fmt f = Fmt.get();
		get(url, headers, in -> {
			char[] buf = new char[1024];
			int count;
			try (BufferedReader bin = new BufferedReader(new InputStreamReader(in, UTF8))) {
				while ((count = bin.read(buf)) != -1) f.append(buf, 0, count);
			} catch (IOException ex) {
				throw ex;
			}
			
		});
		String response = f.release();
		LoggerFactory.getLogger(HttpClient.class).debug("返回结果: {}", response);
		return response;
	}
	
	/** get请求访问http的api函数
	 * @param url 访问地址
	 * @param headers 请求头部内容
	 * @param onRead 返回内容的回调函数
	 * @return 请求结果文本内容
	 */
	public static void get(String url, Map<String, String> headers, OnRead onRead) throws IOException {
		LoggerFactory.getLogger(HttpClient.class).debug("请求服务地址: {}", url);
		URL urlObj = new URL(url);
		HttpURLConnection conn = (HttpURLConnection)urlObj.openConnection();
		conn.setRequestProperty("Accept", "*/*");
		if (headers != null) {
			headers.forEach((k, v) -> conn.setRequestProperty(k, v));
		}

		try (InputStream in = conn.getInputStream()) {
			onRead.accept(in);
		} catch (IOException ex) {
			throw ex;
		}
	}
	
	/** post请求访问http的api函数
	 * @param url 访问地址
	 * @param param 参数，采用post json方式提交
	 * @param headers 请求头部定制内容
	 * @param ResultClass 返回结果声明类
	 * @return
	 */
	public static <T> T post(String url, Object param,
			Map<String, String> headers, Class<T> ResultClass) throws IOException {
		return JSON.parseObject(post(url, param, headers), ResultClass);
	}
	
	/** post请求访问http的api函数
	 * @param url 访问地址
	 * @param param 提交的内容对象
	 * @param headers 请求头部定制内容
	 * @return
	 */
	public static String post(String url, Object param,
			Map<String, String> headers) throws IOException {
		Logger logger = LoggerFactory.getLogger(HttpClient.class);
		// 编码请求参数
		String req = null;
		if (param != null)
			req = param.getClass() == String.class ? (String)param
					: JSON.toJSONString(param, SerializerFeature.WriteDateUseDateFormat,
							SerializerFeature.DisableCircularReferenceDetect);
		logger.debug("请求服务地址: {}", url);
		logger.debug("请求内容: {}", req);

		byte[] reqBytes = req == null ? new byte[0] : req.getBytes(UTF8);
		Fmt f = Fmt.get();
		
		post(url, out -> {
			out.write(reqBytes);
		}, reqBytes.length, headers, in -> {
			char[] buf = new char[1024];
			int count;
			try (BufferedReader bin = new BufferedReader(new InputStreamReader(in, UTF8))) {
				while ((count = bin.read(buf)) != -1) f.append(buf, 0, count);
			} catch (IOException ex) {
				throw ex;
			}
		});
		String response = f.release();
		logger.debug("返回结果: {}", response);
		return response;
	}

	/** post请求访问http的api函数
	 * @param url 访问地址
	 * @param onWrite 要提交的内容写入回调函数
	 * @param writeLen 提交内容的字节长度
	 * @param headers 请求头部定制内容
	 * @param onRead 返回内容的读取回调函数
	 */
	public static void post(String url, OnWrite onWrite, int writeLen,
			Map<String, String> headers, OnRead onRead) throws IOException {
		Logger logger = LoggerFactory.getLogger(HttpClient.class);
		logger.debug("请求服务地址: {}", url);

		OutputStream out = null;
		InputStream in = null;
		
		URL urlObj = new URL(url);
		HttpURLConnection conn = (HttpURLConnection)urlObj.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Accept", "*/*");
		conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
		conn.setRequestProperty("Content-Length", String.valueOf(writeLen));
		if (headers != null) {
			headers.forEach((k, v) -> conn.setRequestProperty(k, v));
		}

		// 发送POST请求必须设置如下两行
		conn.setDoOutput(true);
		conn.setDoInput(true);
		try {
			// 获取URLConnection对象对应的输出流
			out = conn.getOutputStream();
			onWrite.accept(out);
			out.flush();
			// 定义BufferedReader输入流来读取URL的响应
			in = conn.getInputStream();
			onRead.accept(in);

		} finally {
			if (out != null) Langs.close(out);
			if (in != null) Langs.close(in);
		}
	}

	/** post请求访问http的api函数
	 * @param url 访问地址
	 * @param param 参数，采用post form-data方式提交
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String postFormData(String url, Object param, Map<String, String> headers) throws IOException {
		Logger logger = LoggerFactory.getLogger(HttpClient.class);
		// 编码请求参数
		String req = null;
		if (param != null) {
			if (param.getClass() == String.class)
				req = (String)param;
			else if (param instanceof Map<?, ?>)
				req = encodeParam((Map<String, Object>)param);
			else
				req = encodeParam(param);
		}
		logger.debug("请求服务地址: {}", url);
		logger.debug("请求内容: {}", req);

		HashMap<String, String> newHeaders = new HashMap<>(headers);
		newHeaders.put("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
		byte[] reqBytes = req == null ? new byte[0] : req.getBytes(UTF8);
		Fmt f = Fmt.get();
		
		post(url, out -> {
			out.write(reqBytes);
		}, reqBytes.length, newHeaders, in -> {
			char[] buf = new char[1024];
			int count;
			try (BufferedReader bin = new BufferedReader(new InputStreamReader(in, UTF8))) {
				while ((count = bin.read(buf)) != -1) f.append(buf, 0, count);
			} catch (IOException ex) {
				throw ex;
			}
		});

		String response = f.release();
		logger.debug("返回结果: {}", response);
		return response;
	}
	
	public static String makeUrl(String url, Object param) {
		// 编码请求参数
		String req = param == null ? null : encodeParam(param);
		return req == null ? url : Fmt.concat(url, "?", req);
	}
	
	public static String makeUrl(String url, Map<String, Object> param) {
		// 编码请求参数
		String req = param == null ? null : encodeParam(param);
		return req == null ? url : Fmt.concat(url, "?", req);
	}
	
	public static String encodeParam(Object param) {
		if (param == null) return null;
		Fmt f = Fmt.get();

		Langs.forEachFields(param, (name, value) -> {
			f.append(name).append('=');
			String v = Fmt.get().append(value).release();
			try {
				f.append(URLEncoder.encode(v, UTF8)).append('&');
			} catch (UnsupportedEncodingException e) {
				LoggerFactory.getLogger(HttpClient.class).error(Fmt.fmt(
						"unsupport {} character encode: {}",
						UTF8, e.getMessage()), e);
			}
		});

		if (f.length() > 0) f.setLength(f.length() - 1);
		return f.length() == 0 ? null : f.release();
	}
	
	public static String encodeParam(Map<String, Object> param) {
		if (param == null) return null;
		Fmt f = Fmt.get();
		try {
			for (Map.Entry<String, Object> entry : param.entrySet()) {
				f.append(entry.getKey()).append('=');
				String value = Fmt.get().append(entry.getValue()).release();
				f.append(URLEncoder.encode(value, UTF8)).append('&');
			}
			if (f.length() > 0) f.setLength(f.length() - 1);
		}
		catch (UnsupportedEncodingException e) {
			LoggerFactory.getLogger(HttpClient.class).error(Fmt.fmt(
					"unsupport {} character encode: {}",
					UTF8, e.getMessage()), e);
		}
		return f.length() == 0 ? null : f.release();
	}

}
