package com.campuslink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée principal de l'application CampusLink.
 *
 * <p>CampusLink est la plateforme backend destinée à la gestion des services
 * de campus (utilisateurs, ressources académiques, communications, etc.).</p>
 */
@SpringBootApplication
public class CampusLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusLinkApplication.class, args);
    }

}
