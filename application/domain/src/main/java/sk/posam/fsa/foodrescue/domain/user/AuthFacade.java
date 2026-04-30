package sk.posam.fsa.foodrescue.domain.user;

import java.util.Map;

public interface AuthFacade {

    Map<String, Object> exchangeToken(Map<String, String> requestParameters);
}

