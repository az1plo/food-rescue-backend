package sk.posam.fsa.foodrescue.domain.services;

import sk.posam.fsa.foodrescue.domain.models.entities.Offer;
import sk.posam.fsa.foodrescue.domain.models.entities.User;

import java.util.List;

public interface OfferFacade {

    List<Offer> browseAvailableOffers(User currentUser);

    List<Offer> getBusinessOffers(User currentUser, Long businessId);

    Offer get(User currentUser, Long id);

    Offer create(User currentUser, Offer offer);

    Offer update(User currentUser, Long id, Offer offer);

    void delete(User currentUser, Long id);
}
