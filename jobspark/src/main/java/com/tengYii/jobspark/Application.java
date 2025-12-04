package com.tengYii.jobspark;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@MapperScan("com.tengYii.jobspark.infrastructure.mapper")
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        SpringApplication.run(Application.class, args);
        log.info("程序启动成功，耗费时间" + (System.currentTimeMillis() - start));
    }
}
