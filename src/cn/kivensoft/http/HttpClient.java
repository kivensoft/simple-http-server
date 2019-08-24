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

final public class HttpClient {
	private static final String UTF8 = "UTF-8";

	@FunctionalInterface
	public interface OnRead {
		void accept(InputStream in) throws IOException;
	}
	
	@FunctionalInterface
	public interface OnWrite {
		void accept(OutputStream out) throws IOException;
	}

	private HttpClient() {}

	/** get请求访问http的api函数
	 * @param url 访问地址
	 * @param param URL参数
	 * @param headers 请求头部定制内容
	 * @param ResultClass 返回结果声明类
	 * @return
	 */
	public static <T> T get(String url, Map<String, Object> param,
			Map<String, String> headers, Class<T> ResultClass) throws Exception {
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
			Class<T> ResultClass) throws Exception {
		return JSON.parseObject(get(url, param, headers), ResultClass);
	}

	/** get请求访问http的api函数
	 * @param url 访问地址
	 * @param param URL参数
	 * @param headers 请求头部定制内容
	 * @return 返回的文本
	 * @throws IOException
	 */
	public static String get(String url, Map<String, Object> param,
			Map<String, String> headers) throws Exception {
		return get(makeUrl(url, param), headers);
	}

	/** get请求访问http的api函数
	 * @param url 访问地址
	 * @param param URL参数
	 * @param headers 请求头部定制内容
	 * @return 返回的文本
	 * @throws IOException
	 */
	public static String get(String url, Object param,
			Map<String, String> headers) throws Exception {
		return get(makeUrl(url, param), headers);
	}

	/** get请求访问http的api函数
	 * @param url 访问地址
	 * @param headers 请求头部内容
	 * @return 请求结果文本内容
	 */
	public static String get(String url, Map<String, String> headers) throws Exception {
		Logger logger = LoggerFactory.getLogger(HttpClient.class);
		logger.debug("HttpClient.get url = {}", url);
		String[] res = new String[1];
		get(url, headers, in -> res[0] = readFrom(in));
		logger.debug("HttpClient.get response = {}", res[0]);
		return res[0];
	}
	
	/** get请求访问http的api函数
	 * @param url 访问地址
	 * @param headers 请求头部内容
	 * @param onRead 返回内容的回调函数
	 * @return 请求结果文本内容
	 */
	private static void get(String url, Map<String, String> headers, OnRead onRead) throws Exception {
		URL urlObj = new URL(url);
		HttpURLConnection conn = (HttpURLConnection)urlObj.openConnection();
		conn.setRequestProperty("Accept", "*/*");
		if (headers != null)
			headers.forEach((k, v) -> conn.setRequestProperty(k, v));

		conn.connect();
	
		try {
			onRead.accept(conn.getInputStream());
			int code = conn.getResponseCode();
			if (code != 200) throw new HttpClientException(code, conn.getResponseMessage());
		} catch (IOException e) {
			LoggerFactory.getLogger(HttpClient.class).info(
					"HttpClient.get exception.", e);
			throw e;
		} catch (HttpClientException e) {
			LoggerFactory.getLogger(HttpClient.class).info(
					"HttpClient.get return status = {}, message = {}",
					e.code, e.getMessage());
			throw e;
		} finally {
			conn.disconnect();
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
			Map<String, String> headers, Class<T> ResultClass) throws Exception {
		return JSON.parseObject(post(url, param, headers), ResultClass);
	}
	
	/** post请求访问http的api函数
	 * @param url 访问地址
	 * @param param 提交的内容对象
	 * @param headers 请求头部定制内容
	 * @return
	 */
	public static String post(String url, Object param,
			Map<String, String> headers) throws Exception {
		Logger logger = LoggerFactory.getLogger(HttpClient.class);
		// 编码请求参数
		String req = null;
		if (param != null)
			req = param.getClass() == String.class ? (String)param
					: JSON.toJSONString(param, SerializerFeature.WriteDateUseDateFormat,
							SerializerFeature.DisableCircularReferenceDetect);
		logger.debug("HttpClient.post url = {}", url);
		if (req != null && req.length() > 0)
			logger.debug("HttpClient.post data = {}", req);

		byte[] reqBytes = (req == null || req.isEmpty()) ? null : req.getBytes(UTF8);
		
		String[] res = new String[1];
		post(url, out -> out.write(reqBytes) , reqBytes == null ? 0 :reqBytes.length,
				headers, in -> res[0] = readFrom(in));
		logger.debug("HttpClient.post response = {}", res[0]);
		return res[0];
	}

	/** post请求访问http的api函数
	 * @param url 访问地址
	 * @param onWrite 要提交的内容写入回调函数
	 * @param writeLen 提交内容的字节长度
	 * @param headers 请求头部定制内容
	 * @param onRead 返回内容的读取回调函数
	 */
	private static void post(String url, OnWrite onWrite, int writeLen,
			Map<String, String> headers, OnRead onRead) throws Exception {

		URL urlObj = new URL(url);
		HttpURLConnection conn = (HttpURLConnection)urlObj.openConnection();
		conn.setRequestMethod("POST");
		// conn.setRequestProperty("Accept", "*/*");
		if (writeLen > 0) {
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Content-Length", String.valueOf(writeLen));
			conn.setDoOutput(true);
		}
		if (headers != null)
			headers.forEach((k, v) -> conn.setRequestProperty(k, v));

		conn.connect();

		// 发送POST请求必须设置如下两行
		try {
			// 获取URLConnection对象对应的输出流
			if (writeLen > 0)
				onWrite.accept(conn.getOutputStream());
			// 定义BufferedReader输入流来读取URL的响应
			onRead.accept(conn.getInputStream());
			int code = conn.getResponseCode();
			if (code != 200) throw new HttpClientException(code, conn.getResponseMessage());
		} catch (IOException e) {
			LoggerFactory.getLogger(HttpClient.class).info(
					"HttpClient.post exception.", e);
			throw e;
		} catch (HttpClientException e) {
			LoggerFactory.getLogger(HttpClient.class).info(
					"HttpClient.post return status = {}, message = {}",
					e.code, e.getMessage());
			throw e;
		} finally {
			conn.disconnect();
		}
	}

	/** post请求访问http的api函数
	 * @param url 访问地址
	 * @param param 参数，采用post form-data方式提交
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String postFormData(String url, Object param,
			Map<String, String> headers) throws Exception {
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

		logger.debug("HttpClient.postFormData url = {}", url);
		if (req != null && req.length() > 0)
			logger.debug("HttpClient.postFormData data = {}", req);

		HashMap<String, String> newHeaders = new HashMap<>(headers);
		newHeaders.put("Content-Type", "application/x-www-form-urlencoded");
		byte[] reqBytes = req == null || req.isEmpty() ? null : req.getBytes(UTF8);
	
		String[] res = new String[1];
		post(url, out -> out.write(reqBytes) , reqBytes == null ? 0 : reqBytes.length,
				newHeaders, in -> res[0] = readFrom(in));

		logger.debug("HttpClient.postFormData response = {}", res[0]);
		return res[0];
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

	private static String readFrom(InputStream src) throws IOException {
		Fmt f = Fmt.get();
		char[] buf = new char[1024];
		BufferedReader r = new BufferedReader(new InputStreamReader(src, UTF8));
		int count = r.read(buf);
		while (count > 0) {
			f.append(buf, 0, count);
			count = r.read(buf);
		}
		String ret = f.length() == 0 ? null : f.toString();
		f.recycle();
		return ret;
	}
	
	public static class HttpClientException extends Exception {
		private static final long serialVersionUID = 1L;
		public int code;
		public HttpClientException(int code, String message) {
			super(message);
			this.code = code;
		}
	}
}
