package com.vetsoftware.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

@SpringBootApplication(nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class VetSoftwareApplication {
    public static void main(String[] args) {
        SpringApplication.run(VetSoftwareApplication.class, args);
    }
}