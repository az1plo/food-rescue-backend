package sk.posam.fsa.foodrescue.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sk.posam.fsa.foodrescue.domain.offerassistant.GeneratedOfferImage;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferAssistantFacade;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferDraftRequest;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferDraftSuggestion;
import sk.posam.fsa.foodrescue.domain.offerassistant.OfferIllustrativeCoverRequest;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.mapper.OfferAssistantMapper;
import sk.posam.fsa.foodrescue.rest.api.OfferAssistantApi;
import sk.posam.fsa.foodrescue.rest.dto.GeneratedOfferImageDto;
import sk.posam.fsa.foodrescue.rest.dto.OfferDraftFromImageRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.OfferDraftSuggestionDto;
import sk.posam.fsa.foodrescue.rest.dto.OfferIllustrativeCoverRequestDto;
import sk.posam.fsa.foodrescue.security.CurrentUserDetailService;

@RestController
public class OfferAssistantController implements OfferAssistantApi {

    private final OfferAssistantFacade offerAssistantFacade;
    private final OfferAssistantMapper offerAssistantMapper;
    private final CurrentUserDetailService currentUserDetailService;

    public OfferAssistantController(OfferAssistantFacade offerAssistantFacade,
                                    OfferAssistantMapper offerAssistantMapper,
                                    CurrentUserDetailService currentUserDetailService) {
        this.offerAssistantFacade = offerAssistantFacade;
        this.offerAssistantMapper = offerAssistantMapper;
        this.currentUserDetailService = currentUserDetailService;
    }

    @Override
    public ResponseEntity<OfferDraftSuggestionDto> createOfferDraftFromImage(OfferDraftFromImageRequestDto offerDraftFromImageRequestDto) {
        User user = currentUserDetailService.getFullCurrentUser();
        OfferDraftRequest request = offerAssistantMapper.toDraftRequest(offerDraftFromImageRequestDto);
        OfferDraftSuggestion suggestion = offerAssistantFacade.createDraftFromImage(user, request);
        return ResponseEntity.ok(offerAssistantMapper.toDto(suggestion));
    }

    @Override
    public ResponseEntity<GeneratedOfferImageDto> generateOfferCoverImage(OfferIllustrativeCoverRequestDto offerIllustrativeCoverRequestDto) {
        User user = currentUserDetailService.getFullCurrentUser();
        OfferIllustrativeCoverRequest request = offerAssistantMapper.toCoverRequest(offerIllustrativeCoverRequestDto);
        GeneratedOfferImage generatedOfferImage = offerAssistantFacade.generateIllustrativeCover(user, request);
        return ResponseEntity.ok(offerAssistantMapper.toDto(generatedOfferImage));
    }
}
