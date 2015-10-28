package br.com.rotlite.rotlite;

/**
 * Created by claudio on 12/08/15.
 */
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE) //on class level
public @interface Table {

    String name() default "";
    String endpointPost() default "";
    String endpointPut() default "";
    String endpointDelete() default "";
    String endpoint() default "";
    String perPageQuery() default "";
    String pageQuery() default "";
    int perPageCount() default 0;
    boolean autosync() default false;

}