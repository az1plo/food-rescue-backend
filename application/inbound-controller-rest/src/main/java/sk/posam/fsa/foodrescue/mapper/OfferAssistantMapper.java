package sk.posam.fsa.foodrescue.mapper;

import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.offerassistant.GeneratedOfferImage;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferDraftRequest;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferDraftSuggestion;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferIllustrativeCoverRequest;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferImageUpload;
import sk.posam.fsa.foodrescue.domain.offerassistant.StoredOfferImage;
import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;
import sk.posam.fsa.foodrescue.rest.dto.GeneratedOfferImageDto;
import sk.posam.fsa.foodrescue.rest.dto.OfferDraftFromImageRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.OfferDraftSuggestionDto;
import sk.posam.fsa.foodrescue.rest.dto.OfferIllustrativeCoverRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.OfferImageUploadRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.OfferImageUploadResponseDto;

import java.util.Base64;
import java.util.List;

@Component
public class OfferAssistantMapper {

    private final OfferCategoryMapper offerCategoryMapper;

    public OfferAssistantMapper(OfferCategoryMapper offerCategoryMapper) {
        this.offerCategoryMapper = offerCategoryMapper;
    }

    public OfferDraftRequest toDraftRequest(OfferDraftFromImageRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return new OfferDraftRequest(
                dto.getBusinessId(),
                null,
                new OfferImageUpload(
                        dto.getFileName(),
                        dto.getContentType(),
                        decodeBase64(dto.getImageBase64())
                )
        );
    }

    public OfferDraftSuggestionDto toDto(OfferDraftSuggestion suggestion) {
        if (suggestion == null) {
            return null;
        }

        OfferDraftSuggestionDto dto = new OfferDraftSuggestionDto();
        dto.setDetectedItems(suggestion.detectedItems());
        dto.setSuggestedTitle(suggestion.suggestedTitle());
        dto.setSuggestedDescription(suggestion.suggestedDescription());
        dto.setSuggestedCategory(offerCategoryMapper.toDto(suggestion.suggestedCategory()));
        return dto;
    }

    public OfferIllustrativeCoverRequest toCoverRequest(OfferIllustrativeCoverRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return new OfferIllustrativeCoverRequest(
                dto.getBusinessId(),
                dto.getTitle(),
                dto.getDescription(),
                offerCategoryMapper.toEntity(dto.getCategory()),
                dto.getDetectedItems() == null ? List.of() : dto.getDetectedItems()
        );
    }

    public GeneratedOfferImageDto toDto(GeneratedOfferImage generatedOfferImage) {
        if (generatedOfferImage == null) {
            return null;
        }

        GeneratedOfferImageDto dto = new GeneratedOfferImageDto();
        dto.setFileName(generatedOfferImage.fileName());
        dto.setContentType(generatedOfferImage.contentType());
        dto.setImageBase64(generatedOfferImage.base64Data());
        dto.setIllustrativeOnly(generatedOfferImage.illustrativeOnly());
        return dto;
    }

    public OfferImageUpload toImageUpload(OfferImageUploadRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return new OfferImageUpload(
                dto.getFileName(),
                dto.getContentType(),
                decodeBase64(dto.getImageBase64())
        );
    }

    public OfferImageUploadResponseDto toDto(StoredOfferImage storedOfferImage) {
        if (storedOfferImage == null) {
            return null;
        }

        OfferImageUploadResponseDto dto = new OfferImageUploadResponseDto();
        dto.setImageId(storedOfferImage.imageId());
        dto.setImageUrl(storedOfferImage.imageUrl());
        dto.setContentType(storedOfferImage.contentType());
        dto.setIllustrativeImage(storedOfferImage.illustrativeImage());
        return dto;
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
