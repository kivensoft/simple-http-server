package cn.kivensoft.http;

import java.util.Date;
import java.util.HashMap;

@RequestMapping
public class ExampleController {
	@RequestMapping(value = "test", desc = "测试函数")
	public ApiResult test(User user) {
		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put("now", new Date());
		data.put("action", "测试函数");
		data.put("serverName", "SimpleHttpServer");
		data.put("describe", "微型http服务器，利用java自带的http服务实现");
		data.put("name", user.name);
		data.put("age", user.age);
		return ApiResult.success(data);
	}

	@RequestMapping(value = "test2", desc = "测试函数2")
	public ApiResult test2(User user, @AuthInfo String token) {
		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put("now", new Date());
		data.put("action", "测试函数2");
		data.put("serverName", "SimpleHttpServer");
		data.put("describe", "微型http服务器，利用java自带的http服务实现");
		data.put("name", user.name);
		data.put("age", user.age);
		data.put("token", token);
		return ApiResult.success(data);
	}
	
	@RequestMapping(value = "test3", desc = "测试函数3")
	public ApiResult test3(HttpRaw hr, @AuthInfo String token) {
		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put("now", new Date());
		data.put("action", "测试函数3");
		data.put("serverName", "SimpleHttpServer");
		data.put("describe", "微型http服务器，利用java自带的http服务实现");
		data.put("name", hr.getRequestHeader("name"));
		data.put("age", hr.getRequestHeader("age"));
		data.put("token", token);
		return ApiResult.success(data);
	}
	
	public static class User {
		public String name;
		private int age;

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}
}
