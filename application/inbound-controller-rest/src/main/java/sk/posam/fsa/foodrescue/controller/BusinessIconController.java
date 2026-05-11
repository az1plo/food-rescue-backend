package sk.posam.fsa.foodrescue.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import sk.posam.fsa.foodrescue.domain.business.Business;
import sk.posam.fsa.foodrescue.domain.business.BusinessFacade;
import sk.posam.fsa.foodrescue.domain.business.BusinessIconUpload;
import sk.posam.fsa.foodrescue.domain.business.StoredBusinessIconContent;
import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.mapper.BusinessMapper;
import sk.posam.fsa.foodrescue.rest.dto.BusinessResponseDto;
import sk.posam.fsa.foodrescue.security.CurrentUserDetailService;

import java.util.Base64;

@RestController
public class BusinessIconController {

    private final BusinessFacade businessFacade;
    private final BusinessMapper businessMapper;
    private final CurrentUserDetailService currentUserDetailService;

    public BusinessIconController(BusinessFacade businessFacade,
                                  BusinessMapper businessMapper,
                                  CurrentUserDetailService currentUserDetailService) {
        this.businessFacade = businessFacade;
        this.businessMapper = businessMapper;
        this.currentUserDetailService = currentUserDetailService;
    }

    @PostMapping("/business-icons")
    public ResponseEntity<BusinessResponseDto> uploadBusinessIcon(@RequestBody BusinessIconUploadRequest request) {
        User user = currentUserDetailService.getFullCurrentUser();
        Business updatedBusiness = businessFacade.uploadIcon(
                user,
                request.businessId(),
                new BusinessIconUpload(
                        request.fileName(),
                        request.contentType(),
                        decodeBase64(request.imageBase64())
                )
        );
        return ResponseEntity.ok(businessMapper.toDto(updatedBusiness));
    }

    @GetMapping("/business-icons/{id}")
    public ResponseEntity<Resource> getBusinessIcon(@PathVariable("id") String id) {
        StoredBusinessIconContent image = businessFacade.getIcon(id);
        MediaType mediaType = MediaType.parseMediaType(image.contentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(new ByteArrayResource(image.bytes()));
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

    record BusinessIconUploadRequest(
            Long businessId,
            String fileName,
            String contentType,
            String imageBase64
    ) {
    }
}
