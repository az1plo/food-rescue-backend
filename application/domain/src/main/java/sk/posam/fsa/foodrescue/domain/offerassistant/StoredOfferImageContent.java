package sk.posam.fsa.foodrescue.domain.offerassistant;

public record StoredOfferImageContent(
        String imageId,
        String contentType,
        byte[] bytes
) {
}
