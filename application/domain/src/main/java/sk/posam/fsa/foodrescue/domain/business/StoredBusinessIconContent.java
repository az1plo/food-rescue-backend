package sk.posam.fsa.foodrescue.domain.business;

public record StoredBusinessIconContent(
        String imageId,
        String contentType,
        byte[] bytes
) {
}
