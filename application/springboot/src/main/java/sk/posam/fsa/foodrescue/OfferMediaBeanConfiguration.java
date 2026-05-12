package sk.posam.fsa.foodrescue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.posam.fsa.foodrescue.domain.business.BusinessIconStorage;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferImageStorage;
import sk.posam.fsa.foodrescue.offermedia.AzureBlobBusinessIconStorage;
import sk.posam.fsa.foodrescue.offermedia.AzureBlobOfferImageStorage;
import sk.posam.fsa.foodrescue.offermedia.FilesystemBusinessIconStorage;
import sk.posam.fsa.foodrescue.offermedia.FilesystemOfferImageStorage;
import sk.posam.fsa.foodrescue.offermedia.OfferMediaProperties;

@Configuration
public class OfferMediaBeanConfiguration {

    @Bean
    public BusinessIconStorage businessIconStorage(OfferMediaProperties properties) {
        if (isAzureBlobProvider(properties)) {
            return new AzureBlobBusinessIconStorage(properties);
        }
        return new FilesystemBusinessIconStorage(properties);
    }

    @Bean
    public OfferImageStorage offerImageStorage(OfferMediaProperties properties) {
        if (isAzureBlobProvider(properties)) {
            return new AzureBlobOfferImageStorage(properties);
        }
        return new FilesystemOfferImageStorage(properties);
    }

    private boolean isAzureBlobProvider(OfferMediaProperties properties) {
        return "azure-blob".equalsIgnoreCase(properties.getProvider());
    }
}
