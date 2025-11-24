package com.better.CommuteMate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 스케줄링 기능 활성화 (배치 작업용)
public class CommuteMateApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommuteMateApplication.class, args);
	}

}
