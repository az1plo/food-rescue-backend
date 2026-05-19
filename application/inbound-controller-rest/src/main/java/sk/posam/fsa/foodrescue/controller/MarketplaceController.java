package sk.posam.fsa.foodrescue.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sk.posam.fsa.foodrescue.domain.marketplace.MarketplaceFacade;
import sk.posam.fsa.foodrescue.domain.marketplace.MarketplaceOfferCriteria;
import sk.posam.fsa.foodrescue.domain.marketplace.MarketplaceOfferView;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.mapper.MarketplaceOfferMapper;
import sk.posam.fsa.foodrescue.rest.api.MarketplaceApi;
import sk.posam.fsa.foodrescue.rest.dto.MarketplaceOfferResponseDto;
import sk.posam.fsa.foodrescue.rest.dto.MarketplaceOfferSortDto;
import sk.posam.fsa.foodrescue.security.CurrentUserDetailService;

import java.util.List;

@RestController
public class MarketplaceController implements MarketplaceApi {

    private final MarketplaceFacade marketplaceFacade;
    private final MarketplaceOfferMapper marketplaceOfferMapper;
    private final CurrentUserDetailService currentUserDetailService;

    public MarketplaceController(MarketplaceFacade marketplaceFacade,
                                 MarketplaceOfferMapper marketplaceOfferMapper,
                                 CurrentUserDetailService currentUserDetailService) {
        this.marketplaceFacade = marketplaceFacade;
        this.marketplaceOfferMapper = marketplaceOfferMapper;
        this.currentUserDetailService = currentUserDetailService;
    }

    @Override
    public ResponseEntity<List<MarketplaceOfferResponseDto>> getMarketplaceOffers(String q,
                                                                                  Double viewerLat,
                                                                                  Double viewerLng,
                                                                                  Integer radiusKm,
                                                                                  MarketplaceOfferSortDto sort,
                                                                                  Boolean includeUnavailable) {
        MarketplaceOfferCriteria criteria = marketplaceOfferMapper.toCriteria(
                q,
                viewerLat,
                viewerLng,
                radiusKm,
                sort,
                includeUnavailable
        );
        User currentUser = currentUserDetailService.getOptionalCurrentUser();
        List<MarketplaceOfferView> offers = marketplaceFacade.findOffers(currentUser, criteria);
        return ResponseEntity.ok(marketplaceOfferMapper.toDtos(offers));
    }
}
