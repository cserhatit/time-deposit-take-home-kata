package org.ikigaidigital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot entry point of the time deposit service.
 *
 * <p>The domain classes stay free of framework annotations; this class wires them as beans.
 */
@SpringBootApplication
public class TimeDepositApplication {

    /**
     * Starts the application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(TimeDepositApplication.class, args);
    }

    /**
     * Provides the legacy interest calculator as a bean, unchanged.
     *
     * @return the stateless interest calculator
     */
    @Bean
    public TimeDepositCalculator timeDepositCalculator() {
        return new TimeDepositCalculator();
    }
}
