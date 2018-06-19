package cn.kivensoft.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import cn.kivensoft.util.Fmt;
import cn.kivensoft.util.Langs;

public class HttpClient {
	private static final String UTF8 = "UTF-8";

	/** get请求访问http的api函数
	 * @param url 访问地址
	 * @param param 参数，采用post json方式提交
	 * @param ResultClass 返回结果声明类
	 * @return
	 */
	public static <T> T get(String url, Object param,
			Class<T> ResultClass) throws IOException {
		return JSON.parseObject(get(url, param), ResultClass);
	}

	/** get请求访问http的api函数
	 * @param url 访问地址
	 * @param param 参数，采用post json方式提交
	 * @return 请求结果文本内容
	 */
	@SuppressWarnings("unchecked")
	public static String get(String url, Object param) throws IOException {
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

		Fmt f = Fmt.get().append(url);
		if (req != null && !req.isEmpty()) {
			if (req.charAt(0) != '?') f.append('?');
			f.append(req);
		}
		url = f.release();
		
		logger.debug("请求服务地址: {}", url);
		URL _url = new URL(url);
		HttpURLConnection conn = (HttpURLConnection)_url.openConnection();
		conn.setRequestProperty("Accept", "*/*");
		conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

		BufferedReader in = null;
		try {
			// 定义BufferedReader输入流来读取URL的响应
			in = new BufferedReader(new InputStreamReader(
					conn.getInputStream(), UTF8));

			char[] buf = new char[1024];
			f = Fmt.get();
			int count;
			while ((count = in.read(buf)) != -1) f.append(buf, 0, count);
			String response = f.release();
			logger.debug("返回结果: {}", response);
			return response;
		} finally {
			if (in != null) Langs.close(in);
		}
	}

	/** post请求访问http的api函数
	 * @param url 访问地址
	 * @param param 参数，采用post json方式提交
	 * @param ResultClass 返回结果声明类
	 * @return
	 */
	public static <T> T post(String url, Object param, Class<T> ResultClass) throws IOException {
		return JSON.parseObject(post(url, param), ResultClass);
	}
	
	/** post请求访问http的api函数
	 * @param url 访问地址
	 * @param param 参数，采用post json方式提交
	 * @return
	 */
	public static String post(String url, Object param) throws IOException {
		Logger logger = LoggerFactory.getLogger(HttpClient.class);
		// 编码请求参数
		String req = null;
		if (param != null)
			req = param.getClass() == String.class ? (String)param
					: JSON.toJSONString(param, SerializerFeature.WriteDateUseDateFormat,
							SerializerFeature.DisableCircularReferenceDetect);
		logger.debug("请求服务地址: {}", url);
		logger.debug("请求内容: {}", req);

		byte[] req_bytes = req == null ? new byte[0] : req.getBytes(UTF8);
		OutputStream out = null;
		BufferedReader in = null;
		
		URL _url = new URL(url);
		HttpURLConnection conn = (HttpURLConnection)_url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Accept", "*/*");
		conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
		conn.setRequestProperty("Content-Length", String.valueOf(req_bytes.length));
		// 发送POST请求必须设置如下两行
		conn.setDoOutput(true);
		conn.setDoInput(true);
		try {
			// 获取URLConnection对象对应的输出流
			out = conn.getOutputStream();
			out.write(req_bytes);
			out.flush();
			// 定义BufferedReader输入流来读取URL的响应
			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), UTF8));

			char[] buf = new char[1024];
			Fmt f = Fmt.get();
			int count;
			while ((count = in.read(buf)) != -1) f.append(buf, 0, count);
			String response = f.release();
			logger.debug("返回结果: {}", response);
			return response;
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
	public static String postFormData(String url, Object param) throws IOException {
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

		byte[] req_bytes = req == null ? new byte[0] : req.getBytes(UTF8);
		URL _url = new URL(url);
		HttpURLConnection conn = (HttpURLConnection)_url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Accept", "*/*");
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
		conn.setRequestProperty("Content-Length", String.valueOf(req_bytes.length));
		// 发送POST请求必须设置如下两行
		conn.setDoOutput(true);
		conn.setDoInput(true);
		
		OutputStream out = null;
		BufferedReader in = null;
		try {
			// 获取URLConnection对象对应的输出流
			out = conn.getOutputStream();
			out.write(req_bytes);
			out.flush();
			// 定义BufferedReader输入流来读取URL的响应
			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), UTF8));

			char[] buf = new char[1024];
			Fmt f = Fmt.get();
			int count;
			while ((count = in.read(buf)) != -1) f.append(buf, 0, count);
			String response = f.release();
			logger.debug("返回结果: {}", response);
			return response;
		} finally {
			if (out != null) Langs.close(out);
			if (in != null) Langs.close(in);
		}
	}
	
	private static String encodeParam(Object param) {
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
		return f.release();
	}
	
	private static String encodeParam(Map<String, Object> param) {
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
		return f.release();
	}
}
