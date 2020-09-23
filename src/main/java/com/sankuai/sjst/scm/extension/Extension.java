package com.sankuai.sjst.scm.extension;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * <p>扩展点注解,在扩展点实现类上使用该注解</p>
 *
 * @author heyong04@meituan.com
 * @version Extension.class 2020-09-14 上午11:33
 * @since 1.0.0
 **/
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Component
public @interface Extension {
    String name() default "";
}
