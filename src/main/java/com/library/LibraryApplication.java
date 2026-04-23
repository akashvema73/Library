package com.library;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LibraryApplication {
    public static void main(String[] args) {

        SpringApplication.run(LibraryApplication.class, args);
    }

    @Bean
    CommandLineRunner seedDefaultUsers(UserRepo userRepo) {
        return args -> {
            if (!userRepo.existsByUsernameIgnoreCase("admin")) {
                User admin = new User();
                admin.setName("Admin User");
                admin.setUsername("admin");
                admin.setPassword("admin123");
                admin.setEmail("admin@lib.com");
                admin.setPhone("9999999999");
                admin.setRole("ADMIN");
                userRepo.save(admin);
            }

            if (!userRepo.existsByUsernameIgnoreCase("gopal")) {
                User student = new User();
                student.setName("Gopal Srivastava");
                student.setUsername("gopal");
                student.setPassword("gopal123");
                student.setEmail("gopal@lib.com");
                student.setPhone("9876543210");
                student.setRole("STUDENT");
                userRepo.save(student);
            }
        };
    }
}
