package sk.posam.fsa.foodrescue.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "food-rescue.offer-assistant.openai")
public class OfferOpenAiProperties {

    private String responsesBaseUrl = "https://api.openai.com/v1/responses";
    private String imagesBaseUrl = "https://api.openai.com/v1/images/generations";
    private String apiKey;
    private String projectId;
    private String organizationId;
    private String draftModel = "gpt-4.1-mini";
    private String imageModel = "gpt-image-1";
    private Integer timeoutSeconds = 45;
    private Integer maxOutputTokens = 500;
    private Double temperature = 0.2;
    private String imageSize = "1024x1024";
    private String imageQuality = "medium";

    public String getResponsesBaseUrl() {
        return responsesBaseUrl;
    }

    public void setResponsesBaseUrl(String responsesBaseUrl) {
        this.responsesBaseUrl = responsesBaseUrl;
    }

    public String getImagesBaseUrl() {
        return imagesBaseUrl;
    }

    public void setImagesBaseUrl(String imagesBaseUrl) {
        this.imagesBaseUrl = imagesBaseUrl;
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

    public String getDraftModel() {
        return draftModel;
    }

    public void setDraftModel(String draftModel) {
        this.draftModel = draftModel;
    }

    public String getImageModel() {
        return imageModel;
    }

    public void setImageModel(String imageModel) {
        this.imageModel = imageModel;
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

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public String getImageSize() {
        return imageSize;
    }

    public void setImageSize(String imageSize) {
        this.imageSize = imageSize;
    }

    public String getImageQuality() {
        return imageQuality;
    }

    public void setImageQuality(String imageQuality) {
        this.imageQuality = imageQuality;
    }
}
