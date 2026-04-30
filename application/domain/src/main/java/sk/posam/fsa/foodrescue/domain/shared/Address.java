package sk.posam.fsa.foodrescue.domain.shared;

import sk.posam.fsa.foodrescue.domain.shared.ValidationException;

import java.util.Objects;

public class Address {

    private String street;
    private String city;
    private String postalCode;
    private String country;
    private Double latitude;
    private Double longitude;

    public Address() {
    }

    public Address(String street, String city, String postalCode, String country) {
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
    }

    public Address(String street, String city, String postalCode, String country, Double latitude, Double longitude) {
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
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

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
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
        normalizeCoordinates();
    }

    public Address copy() {
        return new Address(street, city, postalCode, country, latitude, longitude);
    }

    private void normalizeCoordinates() {
        if (latitude == null && longitude == null) {
            return;
        }

        require(latitude != null && longitude != null, "Latitude and longitude must be provided together");
        require(!Double.isNaN(latitude) && !Double.isInfinite(latitude), "Latitude must be a finite number");
        require(!Double.isNaN(longitude) && !Double.isInfinite(longitude), "Longitude must be a finite number");
        require(latitude >= -90.0 && latitude <= 90.0, "Latitude must be between -90 and 90");
        require(longitude >= -180.0 && longitude <= 180.0, "Longitude must be between -180 and 180");
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
                && Objects.equals(country, address.country)
                && Objects.equals(latitude, address.latitude)
                && Objects.equals(longitude, address.longitude);
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, city, postalCode, country, latitude, longitude);
    }
}


