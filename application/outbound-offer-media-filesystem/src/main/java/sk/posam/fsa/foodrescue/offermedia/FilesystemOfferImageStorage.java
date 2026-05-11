package sk.posam.fsa.foodrescue.offermedia;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferImageStorage;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferImageUpload;
import sk.posam.fsa.foodrescue.domain.offerassistant.StoredOfferImage;
import sk.posam.fsa.foodrescue.domain.offerassistant.StoredOfferImageContent;
import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@Component
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
        String extension = resolveExtension(upload.contentType(), upload.fileName());
        String imageId = UUID.randomUUID().toString().replace("-", "") + extension;
        Path targetPath = resolvePath(imageId);

        try {
            Files.write(targetPath, upload.copyBytes());
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store offer image", ex);
        }

        return new StoredOfferImage(
                imageId,
                "/offer-images/" + imageId,
                normalizeContentType(upload.contentType(), imageId),
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
                    normalizeContentType(null, imageId),
                    Files.readAllBytes(imagePath)
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read offer image", ex);
        }
    }

    private Path resolvePath(String imageId) {
        if (imageId == null || imageId.isBlank() || imageId.contains("/") || imageId.contains("\\") || imageId.contains("..")) {
            throw new FoodRescueException(FoodRescueException.Type.NOT_FOUND, "Offer image was not found");
        }

        Path resolvedPath = baseDirectory.resolve(imageId).normalize();
        if (!resolvedPath.startsWith(baseDirectory)) {
            throw new FoodRescueException(FoodRescueException.Type.NOT_FOUND, "Offer image was not found");
        }
        return resolvedPath;
    }

    private String resolveExtension(String contentType, String fileName) {
        String normalizedContentType = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedContentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".jpeg") ? ".jpeg" : ".jpg";
            case "image/webp" -> ".webp";
            case "image/svg+xml" -> ".svg";
            default -> ".img";
        };
    }

    private String normalizeContentType(String contentType, String imageId) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType.trim();
        }

        String normalizedImageId = imageId == null ? "" : imageId.toLowerCase(Locale.ROOT);
        if (normalizedImageId.endsWith(".png")) {
            return "image/png";
        }
        if (normalizedImageId.endsWith(".jpg") || normalizedImageId.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (normalizedImageId.endsWith(".webp")) {
            return "image/webp";
        }
        if (normalizedImageId.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }
}
