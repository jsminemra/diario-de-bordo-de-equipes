package com.diariodebordo.diariobordo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DiariobordoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiariobordoApplication.class, args);
	}

}