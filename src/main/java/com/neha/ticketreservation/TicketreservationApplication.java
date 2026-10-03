package com.neha.ticketreservation;

import org.springframework.boot.SpringApplication;
import java.util.TimeZone;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TicketreservationApplication {

	public static void main(String[] args) {
		// Windows reports the legacy zone name "Asia/Calcutta", which PostgreSQL rejects
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
		SpringApplication.run(TicketreservationApplication.class, args);
	}

}
