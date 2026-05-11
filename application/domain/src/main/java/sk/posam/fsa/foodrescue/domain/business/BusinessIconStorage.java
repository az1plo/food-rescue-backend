package sk.posam.fsa.foodrescue.domain.business;

public interface BusinessIconStorage {

    StoredBusinessIcon store(BusinessIconUpload upload);

    StoredBusinessIconContent read(String imageId);

    void delete(String imageId);
}
