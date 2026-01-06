package com.example.WebDichVu_SpringBoot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class WebDichVu_SpringBootApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebDichVu_SpringBootApplication.class, args);
	}

}
