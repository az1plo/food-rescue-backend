package sk.posam.fsa.foodrescue.offermedia;

import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;

import java.util.Locale;
import java.util.UUID;

final class OfferMediaFileNaming {

    private OfferMediaFileNaming() {
    }

    static String nextImageId(String contentType, String fileName) {
        return UUID.randomUUID().toString().replace("-", "") + resolveExtension(contentType, fileName);
    }

    static String validateImageId(String imageId, String notFoundMessage) {
        if (imageId == null || imageId.isBlank() || imageId.contains("/") || imageId.contains("\\") || imageId.contains("..")) {
            throw notFound(notFoundMessage);
        }
        return imageId;
    }

    static FoodRescueException notFound(String message) {
        return new FoodRescueException(FoodRescueException.Type.NOT_FOUND, message);
    }

    static String normalizeContentType(String contentType, String imageId) {
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

    private static String resolveExtension(String contentType, String fileName) {
        String normalizedContentType = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedContentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".jpeg") ? ".jpeg" : ".jpg";
            case "image/webp" -> ".webp";
            case "image/svg+xml" -> ".svg";
            default -> ".img";
        };
    }
}
