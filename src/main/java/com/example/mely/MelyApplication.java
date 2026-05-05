package com.example.mely;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MelyApplication {

	public static void main(String[] args) {
		SpringApplication.run(MelyApplication.class, args);
		System.out.println("MelyApplication started");
	}

}
