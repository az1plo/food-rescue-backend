package sk.posam.fsa.foodrescue.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import sk.posam.fsa.foodrescue.domain.exceptions.FoodRescueException;

import java.io.IOException;
import java.util.List;

@Component
public class RestSecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final HandlerExceptionResolver handlerExceptionResolver;

    public RestSecurityExceptionHandler(HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        handlerExceptionResolver.resolveException(
                request,
                response,
                null,
                new FoodRescueException(
                        FoodRescueException.Type.UNAUTHORIZED,
                        "Authentication required",
                        List.of("Missing or invalid bearer token")));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        handlerExceptionResolver.resolveException(
                request,
                response,
                null,
                new FoodRescueException(
                        FoodRescueException.Type.FORBIDDEN,
                        "Access denied",
                        List.of("User does not have required role")));
    }
}