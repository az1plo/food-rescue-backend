package sk.posam.fsa.foodrescue.offermedia;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "food-rescue.offer-media")
public class OfferMediaProperties {

    private String provider = "filesystem";
    private String baseDir = System.getProperty("java.io.tmpdir") + "/food-rescue-offer-media";
    private final Azure azure = new Azure();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public Azure getAzure() {
        return azure;
    }

    public static class Azure {

        private String connectionString = "";
        private String businessIconsContainer = "business-icons";
        private String offerImagesContainer = "offer-images";

        public String getConnectionString() {
            return connectionString;
        }

        public void setConnectionString(String connectionString) {
            this.connectionString = connectionString;
        }

        public String getBusinessIconsContainer() {
            return businessIconsContainer;
        }

        public void setBusinessIconsContainer(String businessIconsContainer) {
            this.businessIconsContainer = businessIconsContainer;
        }

        public String getOfferImagesContainer() {
            return offerImagesContainer;
        }

        public void setOfferImagesContainer(String offerImagesContainer) {
            this.offerImagesContainer = offerImagesContainer;
        }
    }
}
