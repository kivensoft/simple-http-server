package cn.kivensoft.http;

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class Json {
	public static String toJson(Object value) {
		try {
			return JSON.toJSONString(value,
					SerializerFeature.WriteDateUseDateFormat,
					SerializerFeature.DisableCircularReferenceDetect);
		} catch (Exception e) {
			getLogger().error("json序列化错误", e);
			return null;
		}
	}
	
	public static byte[] toJsonBytes(Object value) {
		try {
		return JSON.toJSONBytes(value,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect);
		} catch (Exception e) {
			getLogger().error("json序列化错误", e);
			return null;
		}
	}
	
	public static <T> T fromJson(String text, Class<T> cls) {
		return JSON.parseObject(text, cls);
	}
	
	public static <T> T fromJson(byte[] bytes, Class<T> cls) {
		return JSON.parseObject(bytes, cls);
	}
	
	public static <T> T fromJson(byte[] bytes, int off, int len, Class<T> cls) {
		try {
			return JSON.parseObject(bytes, off, len, Charset.forName("UTF-8"), cls);
		} catch (Exception e) {
			getLogger().error("json反序列化错误", e);
			return null;
		}
	}

	private static Logger getLogger() {
		return LoggerFactory.getLogger(Json.class);
	}
}
