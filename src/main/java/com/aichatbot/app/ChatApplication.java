package com.aichatbot.app;

import com.aichatbot.app.entity.User;
import com.aichatbot.app.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
    }

    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                User defaultUser = new User();
                defaultUser.setUsername("DefaultUser");
                defaultUser.setEmail("user@example.com");
                userRepository.save(defaultUser);
                System.out.println("Seeded database with default user profile: " + defaultUser.getUsername());
            }
        };
    }
}
