package com.campuslink.realtime.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter par defaut : ne fait rien tant que l'integration Firebase (SDK Admin, tokens
 * d'appareils mobiles) n'est pas branchee. A remplacer par une vraie implementation
 * (FirebaseMessaging.getInstance().send(...)) lors du developpement de l'app mobile V2.
 */
@Component
@Slf4j
public class NoOpFcmPushAdapter implements FcmPushPort {

    @Override
    public void envoyerPush(Long userId, String titre, String corps, Long referenceId) {
        log.debug("[FCM placeholder] push ignore (mobile non branche) -> user={}, titre='{}'", userId, titre);
    }
}
