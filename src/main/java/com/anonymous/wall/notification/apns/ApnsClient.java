package com.anonymous.wall.notification.apns;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.micronaut.context.annotation.Value;

@Singleton
public class ApnsClient {

    private static final Logger log = LoggerFactory.getLogger(ApnsClient.class);

    @Value("${apns.team-id:}")
    private String teamId;

    @Value("${apns.key-id:}")
    private String keyId;

    @Value("${apns.bundle-id:}")
    private String bundleId;

    @Value("${apns.private-key:}")
    private String privateKeyContent;

    @Value("${apns.environment:sandbox}")
    private String environment;

    @Client("apns")
    @Inject
    private HttpClient httpClient;

    @Inject
    private ObjectMapper objectMapper;

    private String cachedJwt;
    private Instant jwtGeneratedAt;

    public int send(String deviceToken, String title, String body, Map<String, Object> data) {
        String url = getApnsUrl() + "/3/device/" + deviceToken;
        String jwt = getOrRefreshJwt();
        String payload = buildPayload(title, body, data);

        HttpRequest<String> request = HttpRequest.POST(url, payload)
                .header("apns-topic", bundleId)
                .header("apns-push-type", "alert")
                .header("authorization", "bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON);

        try {
            HttpResponse<String> response = httpClient.toBlocking().exchange(request, String.class);
            return response.getStatus().getCode();
        } catch (io.micronaut.http.client.exceptions.HttpClientResponseException e) {
            return e.getStatus().getCode();
        } catch (Exception e) {
            log.error("APNs HTTP request failed for token {}: {}", deviceToken, e.getMessage());
            return 503;
        }
    }

    private String getApnsUrl() {
        return "sandbox".equals(environment)
                ? "https://api.sandbox.push.apple.com"
                : "https://api.push.apple.com";
    }

    public synchronized String getOrRefreshJwt() {
        if (cachedJwt == null ||
                Duration.between(jwtGeneratedAt, Instant.now()).toMinutes() >= 50) {
            cachedJwt = generateJwt();
            jwtGeneratedAt = Instant.now();
        }
        return cachedJwt;
    }

    public String generateJwt() {
        ECPrivateKey privateKey = loadPrivateKey(privateKeyContent);
        return Jwts.builder()
                .setHeaderParam("kid", keyId)
                .setIssuer(teamId)
                .setIssuedAt(new Date())
                .signWith(privateKey, SignatureAlgorithm.ES256)
                .compact();
    }

    private ECPrivateKey loadPrivateKey(String p8Contents) {
        try {
            String stripped = p8Contents
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("\\n", "")      // ← strip literal \n from env var
                    .replaceAll("\\s+", ""); // strip actual whitespace
            byte[] keyBytes = Base64.getDecoder().decode(stripped);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("EC");
            return (ECPrivateKey) kf.generatePrivate(spec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load APNs private key", e);
        }
    }

    private String buildPayload(String title, String body, Map<String, Object> data) {
        try {
            Map<String, Object> alert = new HashMap<>();
            alert.put("title", title);
            alert.put("body", body);

            Map<String, Object> aps = new HashMap<>();
            aps.put("alert", alert);
            aps.put("sound", "default");

            Map<String, Object> payload = new HashMap<>();
            payload.put("aps", aps);
            if (data != null) {
                payload.putAll(data);
            }

            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build APNs payload", e);
        }
    }
}
