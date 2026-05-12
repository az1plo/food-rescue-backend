package sk.posam.fsa.foodrescue.offermedia;

import sk.posam.fsa.foodrescue.domain.business.BusinessIconStorage;
import sk.posam.fsa.foodrescue.domain.business.BusinessIconUpload;
import sk.posam.fsa.foodrescue.domain.business.StoredBusinessIcon;
import sk.posam.fsa.foodrescue.domain.business.StoredBusinessIconContent;
import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;

public class AzureBlobBusinessIconStorage implements BusinessIconStorage {

    private final AzureBlobStorageSupport storageSupport;

    public AzureBlobBusinessIconStorage(OfferMediaProperties properties) {
        this.storageSupport = new AzureBlobStorageSupport(
                properties.getAzure().getConnectionString(),
                properties.getAzure().getBusinessIconsContainer(),
                "business icon");
    }

    @Override
    public StoredBusinessIcon store(BusinessIconUpload upload) {
        String imageId = OfferMediaFileNaming.nextImageId(upload.contentType(), upload.fileName());
        String contentType = OfferMediaFileNaming.normalizeContentType(upload.contentType(), imageId);
        storageSupport.upload(imageId, upload.copyBytes(), contentType);

        return new StoredBusinessIcon(imageId, "/business-icons/" + imageId, contentType);
    }

    @Override
    public StoredBusinessIconContent read(String imageId) {
        String safeImageId = OfferMediaFileNaming.validateImageId(imageId, "Business icon was not found");
        if (!storageSupport.exists(safeImageId)) {
            throw new FoodRescueException(
                    FoodRescueException.Type.NOT_FOUND,
                    "Business icon with id=" + safeImageId + " was not found");
        }

        return new StoredBusinessIconContent(
                safeImageId,
                OfferMediaFileNaming.normalizeContentType(null, safeImageId),
                storageSupport.download(safeImageId)
        );
    }

    @Override
    public void delete(String imageId) {
        if (imageId == null || imageId.isBlank()) {
            return;
        }

        storageSupport.deleteIfExists(OfferMediaFileNaming.validateImageId(imageId, "Business icon was not found"));
    }
}
