package com.dronetrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DroneTrackApplication {
    public static void main(String[] args) {
        SpringApplication.run(DroneTrackApplication.class, args);
    }
}
