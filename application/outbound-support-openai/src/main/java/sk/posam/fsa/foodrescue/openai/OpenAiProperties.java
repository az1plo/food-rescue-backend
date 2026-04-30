package sk.posam.fsa.foodrescue.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "food-rescue.support.openai")
public class OpenAiProperties {

    private String baseUrl = "https://api.openai.com/v1/responses";
    private String apiKey;
    private String projectId;
    private String organizationId;
    private String model = "gpt-4.1-mini";
    private Integer timeoutSeconds = 30;
    private Integer maxOutputTokens = 500;
    private Integer maxToolRounds = 3;
    private Double temperature = 0.2;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Integer getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(Integer maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }

    public Integer getMaxToolRounds() {
        return maxToolRounds;
    }

    public void setMaxToolRounds(Integer maxToolRounds) {
        this.maxToolRounds = maxToolRounds;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
}
