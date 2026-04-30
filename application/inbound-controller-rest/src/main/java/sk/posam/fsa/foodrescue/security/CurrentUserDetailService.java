package sk.posam.fsa.foodrescue.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.domain.user.UserRole;
import sk.posam.fsa.foodrescue.domain.user.UserFacade;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class CurrentUserDetailService {

    private final UserFacade userFacade;

    public CurrentUserDetailService(UserFacade userFacade) {
        this.userFacade = userFacade;
    }

    public String getUserEmail() {
        Jwt jwt = getOptionalCurrentJwt();
        String email = jwt == null ? null : readRequiredEmail(jwt);
        if (email == null) {
            throw new CurrentUserNotResolvedException("Authentication required");
        }
        return email;
    }

    public User getOptionalCurrentUser() {
        Jwt jwt = getOptionalCurrentJwt();
        if (jwt == null) {
            return null;
        }

        String email = readRequiredEmail(jwt);
        User user = userFacade.get(email);
        if (user != null) {
            return user;
        }

        return userFacade.provisionExternalIdentity(createTrustedUser(jwt, email));
    }

    public User getFullCurrentUser() {
        User user = getOptionalCurrentUser();
        if (user == null) {
            throw new CurrentUserNotResolvedException("Authentication required");
        }
        return user;
    }

    private Jwt getOptionalCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt;
        }

        return null;
    }

    private String readRequiredEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new CurrentUserNotResolvedException("Current principal does not contain email");
        }
        return email.trim();
    }

    private User createTrustedUser(Jwt jwt, String email) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(resolveFirstName(jwt, email));
        user.setLastName(resolveLastName(jwt));
        user.setRole(resolveRole(jwt));
        return user;
    }

    private String resolveFirstName(Jwt jwt, String email) {
        String givenName = readStringClaim(jwt, "given_name");
        if (givenName != null) {
            return givenName;
        }

        String fullName = readStringClaim(jwt, "name");
        if (fullName != null) {
            String[] parts = fullName.trim().split("\\s+", 2);
            if (parts.length > 0 && !parts[0].isBlank()) {
                return parts[0].trim();
            }
        }

        String emailPrefix = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        return emailPrefix.isBlank() ? "Food" : emailPrefix.trim();
    }

    private String resolveLastName(Jwt jwt) {
        String familyName = readStringClaim(jwt, "family_name");
        if (familyName != null) {
            return familyName;
        }

        String fullName = readStringClaim(jwt, "name");
        if (fullName != null) {
            String[] parts = fullName.trim().split("\\s+", 2);
            if (parts.length == 2 && !parts[1].isBlank()) {
                return parts[1].trim();
            }
        }

        return "User";
    }

    private UserRole resolveRole(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        Collection<?> roles = realmAccess == null ? List.of() : asCollection(realmAccess.get("roles"));

        for (Object role : roles) {
            if (role != null && "ADMIN".equalsIgnoreCase(String.valueOf(role).trim())) {
                return UserRole.ADMIN;
            }
        }

        return UserRole.USER;
    }

    private Collection<?> asCollection(Object value) {
        return value instanceof Collection<?> collection ? collection : List.of();
    }

    private String readStringClaim(Jwt jwt, String claimName) {
        String value = jwt.getClaimAsString(claimName);
        return value == null || value.isBlank() ? null : value.trim();
    }
}

