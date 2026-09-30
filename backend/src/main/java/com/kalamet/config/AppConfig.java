package com.kalamet.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(KalametProperties.class)
public class AppConfig {

    /** All business time checks go through this clock so tests can control "now". */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
