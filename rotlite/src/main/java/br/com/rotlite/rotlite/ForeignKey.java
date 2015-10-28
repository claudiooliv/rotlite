package br.com.rotlite.rotlite;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by danillosantos on 20/10/15.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD) //can use in method only.
public @interface ForeignKey {

    String column() default "";
    String references() default "";

}
