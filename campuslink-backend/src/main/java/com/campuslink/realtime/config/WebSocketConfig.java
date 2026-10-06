package com.campuslink.realtime.config;

import com.campuslink.realtime.security.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration WebSocket / STOMP pour la messagerie et les notifications temps reel.
 *
 * Architecture :
 *  - Endpoint public de handshake : /ws (avec fallback SockJS pour les navigateurs/proxies
 *    qui bloquent les WebSockets natifs).
 *  - Broker simple en memoire (suffisant pour un MVP mono-instance). Pour scaler
 *    horizontalement (plusieurs instances Render), remplacer par un broker externe
 *    (RabbitMQ / Redis STOMP relay).
 *  - Prefixe /app : messages envoyes par le client vers le serveur (@MessageMapping).
 *  - Prefixe /topic : diffusion publique/broadcast (ex: indicateur de saisie dans une conversation).
 *  - Prefixe /user : messages prives cibles vers un utilisateur precis (notifications, messages 1-1),
 *    achemines via convertAndSendToUser().
 *  - L'authentification est geree par StompAuthChannelInterceptor (voir package security),
 *    qui valide le JWT sur la trame CONNECT.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    // Mêmes origines que la config CORS REST (application.cors.allowed-origins).
    @Value("${application.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();

        // Endpoint natif (sans SockJS) pour les clients qui supportent le WebSocket pur
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
