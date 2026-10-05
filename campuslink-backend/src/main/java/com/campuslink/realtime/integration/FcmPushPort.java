package com.campuslink.realtime.integration;

/**
 * Prepare l'integration Firebase Cloud Messaging pour la future application mobile (V2, cf.
 * section "Evolutions futures" du cahier des charges). En V1 (web uniquement), les notifications
 * transitent exclusivement par WebSocket ; ce port est appele en parallele mais peut rester
 * un no-op tant que le mobile n'existe pas (voir NoOpFcmPushAdapter).
 */
public interface FcmPushPort {
    void envoyerPush(Long userId, String titre, String corps, Long referenceId);
}
