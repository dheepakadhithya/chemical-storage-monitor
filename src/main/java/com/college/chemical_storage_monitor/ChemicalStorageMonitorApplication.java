package com.college.chemical_storage_monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@SpringBootApplication
public class ChemicalStorageMonitorApplication {

	public static void main(String[] args) {
		loadEnvFileIfPresent();
		SpringApplication.run(ChemicalStorageMonitorApplication.class, args);
	}

	private static void loadEnvFileIfPresent() {
		File envFile = new File(".env");
		if (envFile.exists()) {
			try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
				String line;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (!line.isEmpty() && !line.startsWith("#") && line.contains("=")) {
						int eqIdx = line.indexOf('=');
						String key = line.substring(0, eqIdx).trim();
						String value = line.substring(eqIdx + 1).trim();
						if (System.getProperty(key) == null && System.getenv(key) == null) {
							System.setProperty(key, value);
						}
						if ("SPRING_PROFILES_ACTIVE".equalsIgnoreCase(key) && System.getProperty("spring.profiles.active") == null && System.getenv("SPRING_PROFILES_ACTIVE") == null) {
							System.setProperty("spring.profiles.active", value);
						}
					}
				}
			} catch (IOException ignored) {
			}
		}
	}

}

