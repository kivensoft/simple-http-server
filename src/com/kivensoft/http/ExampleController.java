package com.kivensoft.http;

import java.util.Date;
import java.util.HashMap;

@RequestMapping
public class ExampleController {
	@RequestMapping(value = "test", desc = "测试函数")
	public ApiResult test() {
		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put("now", new Date());
		data.put("action", "测试函数");
		data.put("serverName", "SimpleHttpServer");
		data.put("describe", "微型http服务器，利用java自带的http服务实现");
		return ApiResult.success(data);
	}

}
