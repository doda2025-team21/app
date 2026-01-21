package frontend;

// Import lib-version and use VersionUtil to print version at startup
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import tudelft.doda2025.team21.VersionUtil;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        System.out.println("lib-version version: " + VersionUtil.getVersion());
        SpringApplication.run(Main.class, args);
    }

}