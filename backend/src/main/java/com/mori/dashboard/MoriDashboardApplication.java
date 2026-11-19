package com.mori.dashboard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MoriDashboardApplication {

    public static void main(String[] args) throws IOException {
        String configuredPath = System.getenv().getOrDefault("DB_PATH", "./data/mori-coffee.db");
        Path databasePath = Path.of(configuredPath).toAbsolutePath();
        Path parent = databasePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        SpringApplication.run(MoriDashboardApplication.class, args);
    }
}
