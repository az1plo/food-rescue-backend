package sk.posam.fsa.foodrescue.offermedia;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.business.BusinessIconStorage;
import sk.posam.fsa.foodrescue.domain.business.BusinessIconUpload;
import sk.posam.fsa.foodrescue.domain.business.StoredBusinessIcon;
import sk.posam.fsa.foodrescue.domain.business.StoredBusinessIconContent;
import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@Component
public class FilesystemBusinessIconStorage implements BusinessIconStorage {

    private final Path baseDirectory;

    public FilesystemBusinessIconStorage(OfferMediaProperties properties) {
        this.baseDirectory = Paths.get(properties.getBaseDir() == null ? "" : properties.getBaseDir())
                .toAbsolutePath()
                .normalize()
                .resolve("business-icons")
                .normalize();
        try {
            Files.createDirectories(baseDirectory);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to initialize business icon directory", ex);
        }
    }

    @Override
    public StoredBusinessIcon store(BusinessIconUpload upload) {
        String extension = resolveExtension(upload.contentType(), upload.fileName());
        String imageId = UUID.randomUUID().toString().replace("-", "") + extension;
        Path targetPath = resolvePath(imageId);

        try {
            Files.write(targetPath, upload.copyBytes());
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store business icon", ex);
        }

        return new StoredBusinessIcon(
                imageId,
                "/business-icons/" + imageId,
                normalizeContentType(upload.contentType(), imageId)
        );
    }

    @Override
    public StoredBusinessIconContent read(String imageId) {
        Path iconPath = resolvePath(imageId);
        if (!Files.exists(iconPath) || !Files.isRegularFile(iconPath)) {
            throw new FoodRescueException(FoodRescueException.Type.NOT_FOUND, "Business icon with id=" + imageId + " was not found");
        }

        try {
            return new StoredBusinessIconContent(
                    imageId,
                    normalizeContentType(null, imageId),
                    Files.readAllBytes(iconPath)
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read business icon", ex);
        }
    }

    @Override
    public void delete(String imageId) {
        if (imageId == null || imageId.isBlank()) {
            return;
        }

        Path iconPath = resolvePath(imageId);
        try {
            Files.deleteIfExists(iconPath);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to delete business icon", ex);
        }
    }

    private Path resolvePath(String imageId) {
        if (imageId == null || imageId.isBlank() || imageId.contains("/") || imageId.contains("\\") || imageId.contains("..")) {
            throw new FoodRescueException(FoodRescueException.Type.NOT_FOUND, "Business icon was not found");
        }

        Path resolvedPath = baseDirectory.resolve(imageId).normalize();
        if (!resolvedPath.startsWith(baseDirectory)) {
            throw new FoodRescueException(FoodRescueException.Type.NOT_FOUND, "Business icon was not found");
        }
        return resolvedPath;
    }

    private String resolveExtension(String contentType, String fileName) {
        String normalizedContentType = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedContentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".jpeg") ? ".jpeg" : ".jpg";
            case "image/webp" -> ".webp";
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
        return "application/octet-stream";
    }
}
