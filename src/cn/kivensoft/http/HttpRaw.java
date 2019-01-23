package cn.kivensoft.http;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

public interface HttpRaw {
	URI getRequestURI();
	Map<String, List<String>> getRequestHeaders();
	String getRequestHeader(String name);
	InputStream getRequestBody();
}
