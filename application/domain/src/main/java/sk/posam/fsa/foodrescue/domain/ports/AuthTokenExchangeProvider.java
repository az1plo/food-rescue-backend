package sk.posam.fsa.foodrescue.domain.ports;

import java.util.Map;

public interface AuthTokenExchangeProvider {

    Map<String, Object> exchange(Map<String, String> requestParameters);
}
