package sk.posam.fsa.foodrescue.offermedia;

import sk.posam.fsa.foodrescue.domain.offerassistant.OfferImageStorage;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferImageUpload;
import sk.posam.fsa.foodrescue.domain.offerassistant.StoredOfferImage;
import sk.posam.fsa.foodrescue.domain.offerassistant.StoredOfferImageContent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FilesystemOfferImageStorage implements OfferImageStorage {

    private final Path baseDirectory;

    public FilesystemOfferImageStorage(OfferMediaProperties properties) {
        this.baseDirectory = Paths.get(properties.getBaseDir() == null ? "" : properties.getBaseDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDirectory);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to initialize offer media directory", ex);
        }
    }

    @Override
    public StoredOfferImage store(OfferImageUpload upload, boolean illustrativeImage) {
        String imageId = OfferMediaFileNaming.nextImageId(upload.contentType(), upload.fileName());
        Path targetPath = resolvePath(imageId);

        try {
            Files.write(targetPath, upload.copyBytes());
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store offer image", ex);
        }

        return new StoredOfferImage(
                imageId,
                "/offer-images/" + imageId,
                OfferMediaFileNaming.normalizeContentType(upload.contentType(), imageId),
                illustrativeImage
        );
    }

    @Override
    public StoredOfferImageContent read(String imageId) {
        Path imagePath = resolvePath(imageId);
        if (!Files.exists(imagePath) || !Files.isRegularFile(imagePath)) {
            throw new FoodRescueException(FoodRescueException.Type.NOT_FOUND, "Offer image with id=" + imageId + " was not found");
        }

        try {
            return new StoredOfferImageContent(
                    imageId,
                    OfferMediaFileNaming.normalizeContentType(null, imageId),
                    Files.readAllBytes(imagePath)
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read offer image", ex);
        }
    }

    private Path resolvePath(String imageId) {
        Path resolvedPath = baseDirectory.resolve(
                        OfferMediaFileNaming.validateImageId(imageId, "Offer image was not found"))
                .normalize();
        if (!resolvedPath.startsWith(baseDirectory)) {
            throw OfferMediaFileNaming.notFound("Offer image was not found");
        }
        return resolvedPath;
    }
}
