package cn.kivensoft.http;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.FIELD })
@Repeatable(Descs.class)
public @interface Desc {
	String path();
	String type();
	boolean required() default false;
	String desc();
	Class<?> ref() default void.class;
}
