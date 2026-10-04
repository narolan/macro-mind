package com.macromind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MacroMindApplication {

	static void main(String[] args) {
		SpringApplication.run(MacroMindApplication.class, args);
	}

}
