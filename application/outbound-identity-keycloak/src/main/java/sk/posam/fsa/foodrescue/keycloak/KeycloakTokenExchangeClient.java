package sk.posam.fsa.foodrescue.keycloak;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;
import sk.posam.fsa.foodrescue.domain.user.AuthTokenExchangeProvider;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class KeycloakTokenExchangeClient implements AuthTokenExchangeProvider {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String keycloakBaseUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public KeycloakTokenExchangeClient(KeycloakProperties keycloakProperties) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.keycloakBaseUrl = trimTrailingSlash(keycloakProperties.getBaseUrl());
        this.realm = keycloakProperties.getRealm();
        this.clientId = keycloakProperties.getClientId();
        this.clientSecret = keycloakProperties.getClientSecret();
    }

    @Override
    public Map<String, Object> exchange(Map<String, String> requestParameters) {
        String grantType = readRequired(requestParameters, "grant_type");
        if (!"password".equals(grantType)
                && !"refresh_token".equals(grantType)
                && !"authorization_code".equals(grantType)) {
            throw new FoodRescueException(
                    FoodRescueException.Type.VALIDATION,
                    "Unsupported grant_type",
                    List.of("Only password, refresh_token and authorization_code grants are supported")
            );
        }

        Map<String, String> keycloakParameters = new LinkedHashMap<>();
        keycloakParameters.put("grant_type", grantType);
        keycloakParameters.put("client_id", clientId);
        keycloakParameters.put("client_secret", clientSecret);

        String scope = requestParameters.getOrDefault("scope", "openid profile email offline_access");
        if (scope != null && !scope.isBlank()) {
            keycloakParameters.put("scope", scope.trim());
        }

        if ("password".equals(grantType)) {
            keycloakParameters.put("username", readRequired(requestParameters, "username"));
            keycloakParameters.put("password", readRequired(requestParameters, "password"));
        } else if ("refresh_token".equals(grantType)) {
            keycloakParameters.put("refresh_token", readRequired(requestParameters, "refresh_token"));
        } else {
            keycloakParameters.put("code", readRequired(requestParameters, "code"));
            keycloakParameters.put("redirect_uri", readRequired(requestParameters, "redirect_uri"));

            String codeVerifier = requestParameters.get("code_verifier");
            if (codeVerifier != null && !codeVerifier.isBlank()) {
                keycloakParameters.put("code_verifier", codeVerifier.trim());
            }
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(keycloakBaseUrl + "/realms/" + realm + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(toFormBody(keycloakParameters)))
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() == 200) {
            return readJsonObject(response.body());
        }

        Map<String, Object> errorBody = readJsonObjectSafe(response.body());
        String error = stringValue(errorBody.get("error"));
        String errorDescription = stringValue(errorBody.get("error_description"));

        if ("invalid_client".equals(error)) {
            throw new IllegalStateException("Keycloak client authentication failed");
        }

        if (response.statusCode() == 400 || response.statusCode() == 401) {
            String message = "password".equals(grantType)
                    ? "Invalid email or password"
                    : "refresh_token".equals(grantType)
                    ? "Session refresh failed"
                    : "Google sign in could not be completed";
            throw new FoodRescueException(
                    FoodRescueException.Type.UNAUTHORIZED,
                    message,
                    List.of(errorDescription != null ? errorDescription : (error != null ? error : "Token exchange failed"))
            );
        }

        throw new IllegalStateException("Keycloak token exchange failed with status " + response.statusCode());
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Keycloak communication failed", ex);
        }
    }

    private Map<String, Object> readJsonObject(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to parse Keycloak JSON object response", ex);
        }
    }

    private Map<String, Object> readJsonObjectSafe(String value) {
        try {
            return readJsonObject(value);
        } catch (RuntimeException ex) {
            return Map.of();
        }
    }

    private String toFormBody(Map<String, String> parameters) {
        return parameters.entrySet().stream()
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String readRequired(Map<String, String> parameters, String key) {
        String value = parameters.get(key);
        if (value == null || value.isBlank()) {
            throw new FoodRescueException(
                    FoodRescueException.Type.VALIDATION,
                    "Missing required authentication parameter",
                    List.of(key + " is required")
            );
        }
        return value.trim();
    }

    private String stringValue(Object value) {
        return value instanceof String text && !text.isBlank() ? text.trim() : null;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String trimTrailingSlash(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }
}

