package sk.posam.fsa.foodrescue.domain.offerassistant;

public record StoredOfferImage(
        String imageId,
        String imageUrl,
        String contentType,
        boolean illustrativeImage
) {
}
