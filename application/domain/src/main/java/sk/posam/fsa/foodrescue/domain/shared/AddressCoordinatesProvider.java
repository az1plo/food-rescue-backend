package sk.posam.fsa.foodrescue.domain.shared;

import sk.posam.fsa.foodrescue.domain.shared.Address;

public interface AddressCoordinatesProvider {

    Address populateCoordinates(Address address);
}


