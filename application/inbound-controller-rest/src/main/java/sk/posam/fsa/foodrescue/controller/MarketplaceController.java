package sk.posam.fsa.foodrescue.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sk.posam.fsa.foodrescue.domain.marketplace.MarketplaceFacade;
import sk.posam.fsa.foodrescue.domain.marketplace.MarketplaceOfferCriteria;
import sk.posam.fsa.foodrescue.domain.marketplace.MarketplaceOfferView;
import sk.posam.fsa.foodrescue.mapper.MarketplaceOfferMapper;
import sk.posam.fsa.foodrescue.rest.api.MarketplaceApi;
import sk.posam.fsa.foodrescue.rest.dto.MarketplaceOfferResponseDto;
import sk.posam.fsa.foodrescue.rest.dto.MarketplaceOfferSortDto;

import java.util.List;

@RestController
public class MarketplaceController implements MarketplaceApi {

    private final MarketplaceFacade marketplaceFacade;
    private final MarketplaceOfferMapper marketplaceOfferMapper;

    public MarketplaceController(MarketplaceFacade marketplaceFacade,
                                 MarketplaceOfferMapper marketplaceOfferMapper) {
        this.marketplaceFacade = marketplaceFacade;
        this.marketplaceOfferMapper = marketplaceOfferMapper;
    }

    @Override
    public ResponseEntity<List<MarketplaceOfferResponseDto>> getMarketplaceOffers(String q,
                                                                                  Double viewerLat,
                                                                                  Double viewerLng,
                                                                                  MarketplaceOfferSortDto sort,
                                                                                  Boolean includeUnavailable) {
        MarketplaceOfferCriteria criteria = marketplaceOfferMapper.toCriteria(
                q,
                viewerLat,
                viewerLng,
                sort,
                includeUnavailable
        );
        List<MarketplaceOfferView> offers = marketplaceFacade.findOffers(criteria);
        return ResponseEntity.ok(marketplaceOfferMapper.toDtos(offers));
    }
}
