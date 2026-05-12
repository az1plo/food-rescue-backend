package sk.posam.fsa.foodrescue.offermedia;

import sk.posam.fsa.foodrescue.domain.offerassistant.OfferImageStorage;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferImageUpload;
import sk.posam.fsa.foodrescue.domain.offerassistant.StoredOfferImage;
import sk.posam.fsa.foodrescue.domain.offerassistant.StoredOfferImageContent;
import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;

public class AzureBlobOfferImageStorage implements OfferImageStorage {

    private final AzureBlobStorageSupport storageSupport;

    public AzureBlobOfferImageStorage(OfferMediaProperties properties) {
        this.storageSupport = new AzureBlobStorageSupport(
                properties.getAzure().getConnectionString(),
                properties.getAzure().getOfferImagesContainer(),
                "offer image");
    }

    @Override
    public StoredOfferImage store(OfferImageUpload upload, boolean illustrativeImage) {
        String imageId = OfferMediaFileNaming.nextImageId(upload.contentType(), upload.fileName());
        String contentType = OfferMediaFileNaming.normalizeContentType(upload.contentType(), imageId);
        storageSupport.upload(imageId, upload.copyBytes(), contentType);

        return new StoredOfferImage(imageId, "/offer-images/" + imageId, contentType, illustrativeImage);
    }

    @Override
    public StoredOfferImageContent read(String imageId) {
        String safeImageId = OfferMediaFileNaming.validateImageId(imageId, "Offer image was not found");
        if (!storageSupport.exists(safeImageId)) {
            throw new FoodRescueException(
                    FoodRescueException.Type.NOT_FOUND,
                    "Offer image with id=" + safeImageId + " was not found");
        }

        return new StoredOfferImageContent(
                safeImageId,
                OfferMediaFileNaming.normalizeContentType(null, safeImageId),
                storageSupport.download(safeImageId)
        );
    }
}
