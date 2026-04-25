package sk.posam.fsa.foodrescue.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sk.posam.fsa.foodrescue.domain.models.entities.Offer;
import sk.posam.fsa.foodrescue.domain.models.entities.User;
import sk.posam.fsa.foodrescue.domain.services.OfferFacade;
import sk.posam.fsa.foodrescue.mapper.OfferMapper;
import sk.posam.fsa.foodrescue.rest.api.OffersApi;
import sk.posam.fsa.foodrescue.rest.dto.CreateOfferRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.OfferResponseDto;
import sk.posam.fsa.foodrescue.rest.dto.UpdateOfferRequestDto;
import sk.posam.fsa.foodrescue.security.CurrentUserDetailService;

import java.net.URI;
import java.util.List;

@RestController
public class OfferController implements OffersApi {

    private final OfferFacade offerFacade;
    private final OfferMapper offerMapper;
    private final CurrentUserDetailService currentUserDetailService;

    public OfferController(OfferFacade offerFacade,
                           OfferMapper offerMapper,
                           CurrentUserDetailService currentUserDetailService) {
        this.offerFacade = offerFacade;
        this.offerMapper = offerMapper;
        this.currentUserDetailService = currentUserDetailService;
    }

    @Override
    public ResponseEntity<List<OfferResponseDto>> getOffers(Long businessId) {
        User user = currentUserDetailService.getFullCurrentUser();
        List<Offer> offers = businessId == null
                ? offerFacade.browseAvailableOffers(user)
                : offerFacade.getBusinessOffers(user, businessId);
        return ResponseEntity.ok(offerMapper.toDtos(offers));
    }

    @Override
    public ResponseEntity<Void> createOffer(CreateOfferRequestDto request) {
        User user = currentUserDetailService.getFullCurrentUser();
        Offer offer = offerMapper.toEntity(request);
        Offer createdOffer = offerFacade.create(user, offer);
        return ResponseEntity.created(URI.create("/offers/" + createdOffer.getId())).build();
    }

    @Override
    public ResponseEntity<OfferResponseDto> getOffer(Long id) {
        User user = currentUserDetailService.getFullCurrentUser();
        Offer offer = offerFacade.get(user, id);
        return ResponseEntity.ok(offerMapper.toDto(offer));
    }

    @Override
    public ResponseEntity<OfferResponseDto> updateOffer(Long id, UpdateOfferRequestDto request) {
        User user = currentUserDetailService.getFullCurrentUser();
        Offer offer = offerMapper.toEntity(request);
        Offer updatedOffer = offerFacade.update(user, id, offer);
        return ResponseEntity.ok(offerMapper.toDto(updatedOffer));
    }

    @Override
    public ResponseEntity<Void> deleteOffer(Long id) {
        User user = currentUserDetailService.getFullCurrentUser();
        offerFacade.delete(user, id);
        return ResponseEntity.noContent().build();
    }
}
