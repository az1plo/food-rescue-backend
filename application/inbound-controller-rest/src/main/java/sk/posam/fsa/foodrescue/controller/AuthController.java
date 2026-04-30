package sk.posam.fsa.foodrescue.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.MultiValueMap;
import sk.posam.fsa.foodrescue.domain.user.AuthFacade;

import java.util.Map;

@RestController
public class AuthController {

    private final AuthFacade authFacade;

    public AuthController(AuthFacade authFacade) {
        this.authFacade = authFacade;
    }

    @PostMapping(
            value = "/auth/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> exchangeToken(@RequestParam MultiValueMap<String, String> formData) {
        Map<String, Object> response = authFacade.exchangeToken(formData.toSingleValueMap());
        return ResponseEntity.ok(response);
    }
}

