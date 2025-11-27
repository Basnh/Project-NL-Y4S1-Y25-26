package com.example.sshcontrol.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAdmin {
    String value() default "Bạn không có quyền truy cập chức năng này";
}
