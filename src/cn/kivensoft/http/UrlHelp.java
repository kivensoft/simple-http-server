package cn.kivensoft.http;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.kivensoft.http.ApiResult;
import cn.kivensoft.http.RequestMapping;
import cn.kivensoft.http.SimpleHttpServer;

final public class UrlHelp {
	/** 显示系统的所有api列表或查询指定的api详情
	 * @param req url=xxx，xxx表示要查询的参数
	 * @return
	 */
	public static ApiResult help(HelpRequest req, SimpleHttpServer server) {
		if (req.getUrl() == null)
			return list(server);

		SimpleHttpServer.MethodInfo methodInfo = server.findMethodInfo(req.getUrl());
		if (methodInfo == null)
			return ApiResult.error("url无效");

		ApiDesc data = new ApiDesc(req.getUrl(), methodInfo.desc);
		if (methodInfo.argType != null) {
			// 计算api的相对路径path
			StringBuilder sb = new StringBuilder();
			RequestMapping cls_rm = methodInfo.obj.getClass()
					.getAnnotation(RequestMapping.class);
			if (cls_rm != null && cls_rm.value() != null)
				SimpleHttpServer.pathAppend(sb, cls_rm.value());
			RequestMapping method_rm = methodInfo.method
					.getAnnotation(RequestMapping.class);
			if (method_rm != null && method_rm.value() != null)
				SimpleHttpServer.pathAppend(sb, method_rm.value());
			String path = sb.toString();
			data.apiParams = getApiParamDescs(path, methodInfo.argType);
		}
		
		return ApiResult.success(data);
	}
	
	private static ApiResult list(SimpleHttpServer server) {
		Map<String, String> apis = server.getAllMappingPath();
		List<ApiDesc> data = apis.size() == 0 ? null : new ArrayList<>();
		for (Map.Entry<String, String> entry : apis.entrySet())
			data.add(new ApiDesc(entry.getKey(), entry.getValue()));
		return ApiResult.success(data);
	}

	private static List<ApiParamDesc> getApiParamDescs(String path, Class<?> cls) {
		List<ApiParamDesc> result = new ArrayList<>();

		for (Field field : getAllFields(cls)) {
			for (Desc desc : field.getAnnotationsByType(Desc.class)) {
				if (!path.matches(desc.path()))
					continue;
				String type = desc.type();
				if (type.isEmpty())
					type = field.getType().getSimpleName();
				ApiParamDesc apd = new ApiParamDesc(field.getName(),
						type, desc.required(), desc.desc());
				result.add(apd);
				if (desc.ref() != void.class)
					apd.params = getApiParamDescs(path, desc.ref());
			}
		}

		return result;
	}
	
	private static List<Field> getAllFields(Class<?> cls) {
		ArrayList<Field> list = new ArrayList<>();
		while (cls != null) {
			Field[] fs = cls.getDeclaredFields();
			for (int i = 0, n = fs.length; i < n; ++i) list.add(fs[i]);
			cls = cls.getSuperclass();
		}
		return list;
	}

	public static class HelpRequest {
		@Desc(path = "/help", type = "String", required = false, desc = "请求地址")
		private String url;

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}
	}

	public static class ApiDesc {
		public String apiName;
		public String apiDesc;
		public List<ApiParamDesc> apiParams;
		public ApiDesc(String apiName, String apiDesc) {
			this.apiName = apiName;
			this.apiDesc = apiDesc;
		}
	}
	
	public static class ApiParamDesc {
		public String paramName;
		public String paramType;
		public boolean paramRequired;
		public String paramDesc;
		public List<ApiParamDesc> params;
		public ApiParamDesc(String paramName, String paramType,
				boolean paramRequired, String paramDesc) {
			this.paramName = paramName;
			this.paramType = paramType;
			this.paramRequired = paramRequired;
			this.paramDesc = paramDesc;
		}
	}
}
