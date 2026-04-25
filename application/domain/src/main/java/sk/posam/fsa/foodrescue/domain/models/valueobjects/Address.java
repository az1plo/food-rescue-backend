package sk.posam.fsa.foodrescue.domain.models.valueobjects;

import sk.posam.fsa.foodrescue.domain.exceptions.ValidationException;

import java.util.Objects;

public class Address {

    private String street;
    private String city;
    private String postalCode;
    private String country;

    public Address() {
    }

    public Address(String street, String city, String postalCode, String country) {
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void prepareForCreation() {
        require(street != null && !street.isBlank(), "Street is required");
        require(city != null && !city.isBlank(), "City is required");
        require(postalCode != null && !postalCode.isBlank(), "Postal code is required");
        require(country != null && !country.isBlank(), "Country is required");

        street = street.trim();
        city = city.trim();
        postalCode = postalCode.trim();
        country = country.trim();
    }

    public Address copy() {
        return new Address(street, city, postalCode, country);
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new ValidationException(message);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Address address)) {
            return false;
        }
        return Objects.equals(street, address.street)
                && Objects.equals(city, address.city)
                && Objects.equals(postalCode, address.postalCode)
                && Objects.equals(country, address.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, city, postalCode, country);
    }
}
