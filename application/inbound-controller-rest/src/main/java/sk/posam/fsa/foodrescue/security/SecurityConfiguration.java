package sk.posam.fsa.foodrescue.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private final RestSecurityExceptionHandler restSecurityExceptionHandler;

    public SecurityConfiguration(RestSecurityExceptionHandler restSecurityExceptionHandler) {
        this.restSecurityExceptionHandler = restSecurityExceptionHandler;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                // Configuration of rules for authorizing HTTP requests
                .authorizeHttpRequests(this::configureAuthorizationRules)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(restSecurityExceptionHandler)
                        .accessDeniedHandler(restSecurityExceptionHandler))
                // Configuration of the OAuth2 resource server
                .oauth2ResourceServer(oauth2 -> configureOauth2ResourceServer(oauth2, jwtDecoder))
                // Building the security filter chain
                .build();
    }

    // Method for configuring authorization rules
    private void configureAuthorizationRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                // Allowing access to actuator endpoints without authorization
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/token").permitAll()
                .requestMatchers(HttpMethod.POST, "/users/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/marketplace/offers").permitAll()
                .requestMatchers(HttpMethod.POST, "/support/chat/messages").permitAll()

                .requestMatchers(HttpMethod.POST, "/businesses").authenticated()
                .requestMatchers(HttpMethod.GET, "/businesses").authenticated()
                .requestMatchers(HttpMethod.GET, "/businesses/*").authenticated()
                .requestMatchers(HttpMethod.GET, "/businesses/*/analytics").authenticated()
                .requestMatchers(HttpMethod.PUT, "/businesses/*").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/businesses/*").authenticated()
                .requestMatchers(HttpMethod.GET, "/admin/businesses/pending").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/admin/businesses/*/approve").hasRole("ADMIN")

                .requestMatchers(HttpMethod.POST, "/offers").authenticated()
                .requestMatchers(HttpMethod.GET, "/offers").permitAll()
                .requestMatchers(HttpMethod.GET, "/offers/*").authenticated()
                .requestMatchers(HttpMethod.PUT, "/offers/*").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/offers/*").authenticated()

                .requestMatchers(HttpMethod.POST, "/reservations").authenticated()
                .requestMatchers(HttpMethod.GET, "/reservations").authenticated()
                .requestMatchers(HttpMethod.GET, "/reservations/*").authenticated()
                .requestMatchers(HttpMethod.POST, "/reservations/*/cancel").authenticated()
                .requestMatchers(HttpMethod.POST, "/reservations/*/pickup").authenticated()

                .requestMatchers(HttpMethod.GET, "/notifications").authenticated()
                .requestMatchers(HttpMethod.GET, "/notifications/*").authenticated()
                .requestMatchers(HttpMethod.POST, "/notifications/*/read").authenticated()
                // All other requests require authorization
                .anyRequest().authenticated();
    }

    // Method for configuring the OAuth2 resource server
    private void configureOauth2ResourceServer(OAuth2ResourceServerConfigurer<HttpSecurity> oauth2, JwtDecoder jwtDecoder) {
        oauth2
                .jwt(jwt -> {
                    jwt.decoder(jwtDecoder);
                    jwt.jwtAuthenticationConverter(JwtConverter::new);
                });
    }
}
