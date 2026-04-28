package sk.posam.fsa.foodrescue.domain.services;

import sk.posam.fsa.foodrescue.domain.ports.AuthTokenExchangeProvider;

import java.util.Map;

public class AuthService implements AuthFacade {

    private final AuthTokenExchangeProvider authTokenExchangeProvider;

    public AuthService(AuthTokenExchangeProvider authTokenExchangeProvider) {
        this.authTokenExchangeProvider = authTokenExchangeProvider;
    }

    @Override
    public Map<String, Object> exchangeToken(Map<String, String> requestParameters) {
        return authTokenExchangeProvider.exchange(requestParameters);
    }
}
