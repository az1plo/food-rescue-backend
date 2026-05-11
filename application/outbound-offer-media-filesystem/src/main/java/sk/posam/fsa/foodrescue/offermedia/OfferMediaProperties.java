package sk.posam.fsa.foodrescue.offermedia;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "food-rescue.offer-media")
public class OfferMediaProperties {

    private String baseDir = System.getProperty("java.io.tmpdir") + "/food-rescue-offer-media";

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }
}
