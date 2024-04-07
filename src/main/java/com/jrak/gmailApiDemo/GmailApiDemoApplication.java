package com.jrak.gmailApiDemo;

import com.jrak.gmailApiDemo.controller.EmailController;
import com.jrak.gmailApiDemo.model.Letter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GmailApiDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(GmailApiDemoApplication.class, args);
	}
}
