package sk.posam.fsa.foodrescue.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sk.posam.fsa.foodrescue.domain.user.AuthFacade;
import sk.posam.fsa.foodrescue.mapper.AuthMapper;
import sk.posam.fsa.foodrescue.rest.api.AuthApi;
import sk.posam.fsa.foodrescue.rest.dto.AuthTokenExchangeResponseDto;

@RestController
public class AuthController implements AuthApi {

    private final AuthFacade authFacade;
    private final AuthMapper authMapper;

    public AuthController(AuthFacade authFacade, AuthMapper authMapper) {
        this.authFacade = authFacade;
        this.authMapper = authMapper;
    }

    @Override
    public ResponseEntity<AuthTokenExchangeResponseDto> exchangeToken(String grantType,
                                                                      String username,
                                                                      String password,
                                                                      String refreshToken,
                                                                      String code,
                                                                      String redirectUri,
                                                                      String codeVerifier,
                                                                      String scope) {
        return ResponseEntity.ok(
                authMapper.toDto(
                        authFacade.exchangeToken(
                                authMapper.toRequestParameters(
                                        grantType,
                                        username,
                                        password,
                                        refreshToken,
                                        code,
                                        redirectUri,
                                        codeVerifier,
                                        scope
                                )
                        )
                )
        );
    }
}

