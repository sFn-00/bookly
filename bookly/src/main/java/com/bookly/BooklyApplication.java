package com.bookly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BooklyApplication {

	public static void main(String[] args) {
		SpringApplication.run(BooklyApplication.class, args);
	}
}
