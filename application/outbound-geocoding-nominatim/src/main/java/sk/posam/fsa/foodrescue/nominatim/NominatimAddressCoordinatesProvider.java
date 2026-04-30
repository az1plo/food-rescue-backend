package sk.posam.fsa.foodrescue.nominatim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.shared.Address;
import sk.posam.fsa.foodrescue.domain.shared.AddressCoordinatesProvider;

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
public class NominatimAddressCoordinatesProvider implements AddressCoordinatesProvider {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String userAgent;

    public NominatimAddressCoordinatesProvider(NominatimProperties properties) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = trimTrailingSlash(properties.getBaseUrl());
        this.userAgent = properties.getUserAgent();
    }

    @Override
    public Address populateCoordinates(Address address) {
        if (address == null) {
            return null;
        }

        if (address.getLatitude() != null && address.getLongitude() != null) {
            return address.copy();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(buildSearchUri(address))
                    .header("Accept", "application/json")
                    .header("User-Agent", userAgent)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return address.copy();
            }

            List<Map<String, Object>> payload = readJsonArray(response.body());
            if (payload.isEmpty()) {
                return address.copy();
            }

            Map<String, Object> firstMatch = payload.getFirst();
            Double latitude = parseCoordinate(firstMatch.get("lat"));
            Double longitude = parseCoordinate(firstMatch.get("lon"));

            return new Address(
                    address.getStreet(),
                    address.getCity(),
                    address.getPostalCode(),
                    address.getCountry(),
                    latitude,
                    longitude
            );
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return address.copy();
        } catch (RuntimeException ex) {
            return address.copy();
        }
    }

    private URI buildSearchUri(Address address) {
        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("format", "jsonv2");
        queryParameters.put("limit", "1");
        putIfNotBlank(queryParameters, "street", address.getStreet());
        putIfNotBlank(queryParameters, "city", address.getCity());
        putIfNotBlank(queryParameters, "postalcode", address.getPostalCode());
        putIfNotBlank(queryParameters, "country", address.getCountry());

        String queryString = queryParameters.entrySet().stream()
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .collect(java.util.stream.Collectors.joining("&"));

        return URI.create(baseUrl + (queryString.isBlank() ? "" : "?" + queryString));
    }

    private void putIfNotBlank(Map<String, String> queryParameters, String key, String value) {
        if (value != null && !value.isBlank()) {
            queryParameters.put(key, value);
        }
    }

    private List<Map<String, Object>> readJsonArray(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to parse Nominatim response", ex);
        }
    }

    private Double parseCoordinate(Object value) {
        if (!(value instanceof String coordinate) || coordinate.isBlank()) {
            return null;
        }

        try {
            return Double.parseDouble(coordinate);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String trimTrailingSlash(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }
}

