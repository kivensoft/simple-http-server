package cn.kivensoft.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface RequestMapping {
	public static final String GET = "get";
	public static final String POST = "post";
	
	String value() default "";
	String method() default "";
	String desc() default "";
}
