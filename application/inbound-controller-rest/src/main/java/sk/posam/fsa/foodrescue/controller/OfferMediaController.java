package sk.posam.fsa.foodrescue.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferAssistantFacade;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferImageUpload;
import sk.posam.fsa.foodrescue.domain.offerassistant.StoredOfferImage;
import sk.posam.fsa.foodrescue.domain.offerassistant.StoredOfferImageContent;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.mapper.OfferAssistantMapper;
import sk.posam.fsa.foodrescue.rest.api.OfferImagesApi;
import sk.posam.fsa.foodrescue.rest.dto.OfferImageUploadRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.OfferImageUploadResponseDto;
import sk.posam.fsa.foodrescue.security.CurrentUserDetailService;

@RestController
public class OfferMediaController implements OfferImagesApi {

    private final OfferAssistantFacade offerAssistantFacade;
    private final OfferAssistantMapper offerAssistantMapper;
    private final CurrentUserDetailService currentUserDetailService;

    public OfferMediaController(OfferAssistantFacade offerAssistantFacade,
                                OfferAssistantMapper offerAssistantMapper,
                                CurrentUserDetailService currentUserDetailService) {
        this.offerAssistantFacade = offerAssistantFacade;
        this.offerAssistantMapper = offerAssistantMapper;
        this.currentUserDetailService = currentUserDetailService;
    }

    @Override
    public ResponseEntity<OfferImageUploadResponseDto> uploadOfferImage(OfferImageUploadRequestDto offerImageUploadRequestDto) {
        User user = currentUserDetailService.getFullCurrentUser();
        OfferImageUpload upload = offerAssistantMapper.toImageUpload(offerImageUploadRequestDto);
        StoredOfferImage storedOfferImage = offerAssistantFacade.uploadOfferImage(
                user,
                offerImageUploadRequestDto.getBusinessId(),
                upload,
                Boolean.TRUE.equals(offerImageUploadRequestDto.getIllustrativeImage())
        );
        return ResponseEntity.ok(offerAssistantMapper.toDto(storedOfferImage));
    }

    @Override
    public ResponseEntity<Resource> getOfferImage(String id) {
        StoredOfferImageContent image = offerAssistantFacade.getOfferImage(id);
        MediaType mediaType = MediaType.parseMediaType(image.contentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(new ByteArrayResource(image.bytes()));
    }
}
