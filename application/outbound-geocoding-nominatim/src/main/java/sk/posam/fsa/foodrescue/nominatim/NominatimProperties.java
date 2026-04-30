package sk.posam.fsa.foodrescue.nominatim;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "food-rescue.geocoding.nominatim")
public class NominatimProperties {

    private String baseUrl = "https://nominatim.openstreetmap.org/search";
    private String userAgent = "food-rescue-platform/1.0";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
