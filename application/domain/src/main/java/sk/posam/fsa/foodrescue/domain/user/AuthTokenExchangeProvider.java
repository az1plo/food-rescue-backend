package sk.posam.fsa.foodrescue.domain.user;

import java.util.Map;

public interface AuthTokenExchangeProvider {

    Map<String, Object> exchange(Map<String, String> requestParameters);
}

