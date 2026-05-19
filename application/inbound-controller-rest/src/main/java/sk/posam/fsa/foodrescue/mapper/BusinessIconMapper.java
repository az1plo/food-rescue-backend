package sk.posam.fsa.foodrescue.mapper;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.business.BusinessIconUpload;
import sk.posam.fsa.foodrescue.domain.business.StoredBusinessIconContent;
import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;
import sk.posam.fsa.foodrescue.rest.dto.BusinessIconUploadRequestDto;

import java.util.Base64;

@Component
public class BusinessIconMapper {

    public BusinessIconUpload toUpload(BusinessIconUploadRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return new BusinessIconUpload(
                dto.getFileName(),
                dto.getContentType(),
                decodeBase64(dto.getImageBase64())
        );
    }

    public MediaType toMediaType(StoredBusinessIconContent image) {
        return image == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(image.contentType());
    }

    public Resource toResource(StoredBusinessIconContent image) {
        return image == null ? null : new ByteArrayResource(image.bytes());
    }

    private byte[] decodeBase64(String value) {
        if (value == null || value.isBlank()) {
            return new byte[0];
        }

        try {
            return Base64.getDecoder().decode(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new FoodRescueException(FoodRescueException.Type.VALIDATION, "Image payload is not valid base64");
        }
    }
}
