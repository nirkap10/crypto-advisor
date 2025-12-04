package com.moveo.crypto_advisor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CryptoAdvisorApplication {

	public static void main(String[] args) {
		SpringApplication.run(CryptoAdvisorApplication.class, args);
	}

}
