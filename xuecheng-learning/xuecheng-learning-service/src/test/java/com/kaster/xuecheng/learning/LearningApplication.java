package com.kaster.xuecheng.learning;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages={"com.kaster.xuecheng.*.feignclient"})
@SpringBootApplication
public class LearningApplication {
	public static void main(String[] args) {
		SpringApplication.run(LearningApplication.class, args);
	}
}