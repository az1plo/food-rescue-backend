package sk.posam.fsa.foodrescue.domain.offerassistant;

public record OfferImageUpload(
        String fileName,
        String contentType,
        byte[] bytes
) {

    public byte[] copyBytes() {
        return bytes == null ? new byte[0] : bytes.clone();
    }
}
