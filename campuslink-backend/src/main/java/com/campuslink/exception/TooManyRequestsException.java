package com.campuslink.exception;

/**
 * Exception levée lorsqu'une limite de fréquence est dépassée : délai
 * anti-spam entre deux envois OTP, ou nombre maximal de tentatives de
 * vérification atteint (HTTP 429).
 */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }

}
