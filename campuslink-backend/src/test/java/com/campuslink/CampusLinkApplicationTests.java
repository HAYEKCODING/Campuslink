package com.campuslink;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test de fumée vérifiant que le contexte Spring se charge correctement.
 */
@SpringBootTest
@ActiveProfiles("test")
class CampusLinkApplicationTests {

    @Test
    void contextLoads() {
        // Vérifie uniquement que le contexte Spring démarre sans erreur.
    }

}
