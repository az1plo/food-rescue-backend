package sk.posam.fsa.foodrescue.keycloak;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.exceptions.FoodRescueException;
import sk.posam.fsa.foodrescue.domain.models.entities.User;
import sk.posam.fsa.foodrescue.domain.ports.UserIdentityProvider;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KeycloakAdminUserIdentityProvider implements UserIdentityProvider {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String keycloakBaseUrl;
    private final String realm;
    private final String adminRealm;
    private final String adminClientId;
    private final String adminUsername;
    private final String adminPassword;

    public KeycloakAdminUserIdentityProvider(KeycloakProperties keycloakProperties) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.keycloakBaseUrl = trimTrailingSlash(keycloakProperties.getBaseUrl());
        this.realm = keycloakProperties.getRealm();
        this.adminRealm = keycloakProperties.getAdminRealm();
        this.adminClientId = keycloakProperties.getAdminClientId();
        this.adminUsername = keycloakProperties.getAdminUsername();
        this.adminPassword = keycloakProperties.getAdminPassword();
    }

    @Override
    public void create(User user) {
        String accessToken = requestAdminAccessToken();
        String userId = createKeycloakUser(accessToken, user);
        try {
            assignRealmRole(accessToken, userId, user.getRole().name());
        } catch (RuntimeException ex) {
            deleteKeycloakUser(accessToken, userId);
            throw ex;
        }
    }

    @Override
    public void deleteByEmail(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        String accessToken = requestAdminAccessToken();
        findUserIdByEmail(accessToken, email).ifPresent(userId -> deleteKeycloakUser(accessToken, userId));
    }

    private String requestAdminAccessToken() {
        String requestBody = "client_id=" + urlEncode(adminClientId)
                + "&username=" + urlEncode(adminUsername)
                + "&password=" + urlEncode(adminPassword)
                + "&grant_type=password";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(keycloakBaseUrl + "/realms/" + adminRealm + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Keycloak admin token request failed with status " + response.statusCode());
        }

        Map<String, Object> body = readJsonObject(response.body());
        Object accessToken = body.get("access_token");
        if (!(accessToken instanceof String token) || token.isBlank()) {
            throw new IllegalStateException("Keycloak admin token response does not contain access_token");
        }

        return token;
    }

    private String createKeycloakUser(String accessToken, User user) {
        Map<String, Object> requestBody = Map.of(
                "username", user.getEmail(),
                "email", user.getEmail(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "enabled", true,
                "emailVerified", true,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", user.getPassword(),
                        "temporary", false
                ))
        );

        HttpRequest request = authorizedJsonRequest(accessToken,
                URI.create(keycloakBaseUrl + "/admin/realms/" + realm + "/users"),
                toJson(requestBody));

        HttpResponse<String> response = send(request);
        if (response.statusCode() == 409) {
            throw new FoodRescueException(
                    FoodRescueException.Type.CONFLICT,
                    "User with given email already exists"
            );
        }

        if (response.statusCode() != 201) {
            throw new IllegalStateException("Keycloak user creation failed with status " + response.statusCode());
        }

        String location = response.headers().firstValue("Location")
                .orElseThrow(() -> new IllegalStateException("Keycloak user creation did not return Location header"));
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private void assignRealmRole(String accessToken, String userId, String roleName) {
        HttpRequest getRoleRequest = HttpRequest.newBuilder()
                .uri(URI.create(keycloakBaseUrl + "/admin/realms/" + realm + "/roles/" + urlEncodePath(roleName)))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> getRoleResponse = send(getRoleRequest);
        if (getRoleResponse.statusCode() != 200) {
            throw new IllegalStateException("Keycloak role lookup failed with status " + getRoleResponse.statusCode());
        }

        Map<String, Object> roleRepresentation = readJsonObject(getRoleResponse.body());

        HttpRequest assignRoleRequest = authorizedJsonRequest(accessToken,
                URI.create(keycloakBaseUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm"),
                toJson(List.of(roleRepresentation)));

        HttpResponse<String> assignRoleResponse = send(assignRoleRequest);
        if (assignRoleResponse.statusCode() != 204) {
            throw new IllegalStateException("Keycloak role assignment failed with status " + assignRoleResponse.statusCode());
        }
    }

    private Optional<String> findUserIdByEmail(String accessToken, String email) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(keycloakBaseUrl + "/admin/realms/" + realm + "/users?email=" + urlEncode(email) + "&exact=true"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Keycloak user lookup failed with status " + response.statusCode());
        }

        List<Map<String, Object>> users = readJsonArray(response.body());
        return users.stream()
                .map(user -> user.get("id"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst();
    }

    private void deleteKeycloakUser(String accessToken, String userId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(keycloakBaseUrl + "/admin/realms/" + realm + "/users/" + userId))
                .header("Authorization", "Bearer " + accessToken)
                .DELETE()
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() != 204) {
            throw new IllegalStateException("Keycloak user deletion failed with status " + response.statusCode());
        }
    }

    private HttpRequest authorizedJsonRequest(String accessToken, URI uri, String body) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
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

    private List<Map<String, Object>> readJsonArray(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to parse Keycloak JSON array response", ex);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize Keycloak request body", ex);
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String urlEncodePath(String value) {
        return value.replace(" ", "%20");
    }

    private String trimTrailingSlash(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }
}
