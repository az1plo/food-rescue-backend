package sk.posam.fsa.foodrescue.offermedia;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;

final class AzureBlobStorageSupport {

    private final BlobContainerClient containerClient;
    private final String resourceName;

    AzureBlobStorageSupport(String connectionString, String containerName, String resourceName) {
        if (connectionString == null || connectionString.isBlank()) {
            throw new IllegalStateException("Azure Blob Storage connection string for " + resourceName + " is not configured");
        }
        if (containerName == null || containerName.isBlank()) {
            throw new IllegalStateException("Azure Blob Storage container for " + resourceName + " is not configured");
        }

        this.resourceName = resourceName;
        this.containerClient = new BlobContainerClientBuilder()
                .connectionString(connectionString.trim())
                .containerName(containerName.trim())
                .buildClient();
        this.containerClient.createIfNotExists();
    }

    void upload(String blobName, byte[] bytes, String contentType) {
        try {
            BlobClient blobClient = blobClient(blobName);
            blobClient.upload(BinaryData.fromBytes(bytes == null ? new byte[0] : bytes), true);
            blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Unable to store " + resourceName, ex);
        }
    }

    boolean exists(String blobName) {
        try {
            return blobClient(blobName).exists();
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Unable to inspect " + resourceName, ex);
        }
    }

    byte[] download(String blobName) {
        try {
            return blobClient(blobName).downloadContent().toBytes();
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Unable to read " + resourceName, ex);
        }
    }

    void deleteIfExists(String blobName) {
        try {
            blobClient(blobName).deleteIfExists();
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Unable to delete " + resourceName, ex);
        }
    }

    private BlobClient blobClient(String blobName) {
        return containerClient.getBlobClient(blobName);
    }
}
