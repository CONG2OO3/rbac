package com.company.rbac;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan({
        "com.company.rbac.system.mapper",
        "com.company.rbac.log.mapper",
        "com.company.rbac.file.mapper",
        "com.company.rbac.business.mapper"
})
@SpringBootApplication
public class RbacApplication {
    public static void main(String[] args) { SpringApplication.run(RbacApplication.class, args); }
}
