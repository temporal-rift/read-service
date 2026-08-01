package io.github.temporalrift.read;

import org.springframework.boot.SpringApplication;

public class TestReadServiceApplication {

    static void main(String[] args) {
        SpringApplication.from(ReadServiceApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
