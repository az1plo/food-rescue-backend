package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.rest.dto.AuthTokenExchangeResponseDto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class AuthMapper {

    private static final Set<String> KNOWN_RESPONSE_KEYS = Set.of(
            "access_token",
            "expires_in",
            "refresh_expires_in",
            "refresh_token",
            "token_type",
            "id_token",
            "not-before-policy",
            "session_state",
            "scope"
    );

    public Map<String, String> toRequestParameters(String grantType,
                                                   String username,
                                                   String password,
                                                   String refreshToken,
                                                   String code,
                                                   String redirectUri,
                                                   String codeVerifier,
                                                   String scope) {
        Map<String, String> parameters = new LinkedHashMap<>();
        putIfHasText(parameters, "grant_type", grantType);
        putIfHasText(parameters, "username", username);
        putIfHasText(parameters, "password", password);
        putIfHasText(parameters, "refresh_token", refreshToken);
        putIfHasText(parameters, "code", code);
        putIfHasText(parameters, "redirect_uri", redirectUri);
        putIfHasText(parameters, "code_verifier", codeVerifier);
        putIfHasText(parameters, "scope", scope);
        return parameters;
    }

    public AuthTokenExchangeResponseDto toDto(Map<String, Object> values) {
        AuthTokenExchangeResponseDto dto = new AuthTokenExchangeResponseDto();
        dto.setAccessToken(stringValue(values.get("access_token")));
        dto.setExpiresIn(integerValue(values.get("expires_in")));
        dto.setRefreshExpiresIn(integerValue(values.get("refresh_expires_in")));
        dto.setRefreshToken(stringValue(values.get("refresh_token")));
        dto.setTokenType(stringValue(values.get("token_type")));
        dto.setIdToken(stringValue(values.get("id_token")));
        dto.setNotBeforePolicy(integerValue(values.get("not-before-policy")));
        dto.setSessionState(stringValue(values.get("session_state")));
        dto.setScope(stringValue(values.get("scope")));

        values.forEach((key, value) -> {
            if (!KNOWN_RESPONSE_KEYS.contains(key)) {
                dto.putAdditionalProperty(key, value);
            }
        });
        return dto;
    }

    private void putIfHasText(Map<String, String> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value.trim());
        }
    }

    private String stringValue(Object value) {
        return value instanceof String text ? text : null;
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.valueOf(text);
        }
        return null;
    }
}
