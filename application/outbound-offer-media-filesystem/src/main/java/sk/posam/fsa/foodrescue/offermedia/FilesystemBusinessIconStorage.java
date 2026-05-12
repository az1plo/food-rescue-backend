package sk.posam.fsa.foodrescue.offermedia;

import sk.posam.fsa.foodrescue.domain.business.BusinessIconStorage;
import sk.posam.fsa.foodrescue.domain.business.BusinessIconUpload;
import sk.posam.fsa.foodrescue.domain.business.StoredBusinessIcon;
import sk.posam.fsa.foodrescue.domain.business.StoredBusinessIconContent;
import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        String imageId = OfferMediaFileNaming.nextImageId(upload.contentType(), upload.fileName());
        Path targetPath = resolvePath(imageId);

        try {
            Files.write(targetPath, upload.copyBytes());
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store business icon", ex);
        }

        return new StoredBusinessIcon(
                imageId,
                "/business-icons/" + imageId,
                OfferMediaFileNaming.normalizeContentType(upload.contentType(), imageId)
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
                    OfferMediaFileNaming.normalizeContentType(null, imageId),
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
        Path resolvedPath = baseDirectory.resolve(
                        OfferMediaFileNaming.validateImageId(imageId, "Business icon was not found"))
                .normalize();
        if (!resolvedPath.startsWith(baseDirectory)) {
            throw OfferMediaFileNaming.notFound("Business icon was not found");
        }
        return resolvedPath;
    }
}
