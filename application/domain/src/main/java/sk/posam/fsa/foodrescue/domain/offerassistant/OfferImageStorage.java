package sk.posam.fsa.foodrescue.domain.offerassistant;

public interface OfferImageStorage {

    StoredOfferImage store(OfferImageUpload upload, boolean illustrativeImage);

    StoredOfferImageContent read(String imageId);
}
