package sk.posam.fsa.foodrescue.domain.business;

public record BusinessIconUpload(
        String fileName,
        String contentType,
        byte[] bytes
) {

    public byte[] copyBytes() {
        return bytes == null ? new byte[0] : bytes.clone();
    }
}
