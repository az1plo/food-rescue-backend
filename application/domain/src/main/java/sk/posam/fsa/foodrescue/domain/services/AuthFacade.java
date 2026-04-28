package sk.posam.fsa.foodrescue.domain.services;

import java.util.Map;

public interface AuthFacade {

    Map<String, Object> exchangeToken(Map<String, String> requestParameters);
}
