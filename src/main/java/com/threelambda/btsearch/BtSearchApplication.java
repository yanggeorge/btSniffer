package com.threelambda.btsearch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author ym
 */
@Slf4j
@EnableScheduling
@SpringBootApplication
public class BtSearchApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(BtSearchApplication.class, args).start();
        log.info("Succeed to run the Application.");
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("-----------------------");
        log.info("|      BtSearch       |");
        log.info("-----------------------");
    }
}
