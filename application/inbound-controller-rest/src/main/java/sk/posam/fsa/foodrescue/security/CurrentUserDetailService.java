package sk.posam.fsa.foodrescue.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import sk.posam.fsa.foodrescue.domain.models.entities.User;
import sk.posam.fsa.foodrescue.domain.service.UserFacade;

@Service
public class CurrentUserDetailService {

    private final UserFacade userFacade;

    public CurrentUserDetailService(UserFacade userFacade) {
        this.userFacade = userFacade;
    }

    public String getUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CurrentUserNotResolvedException("Authentication required");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("email");
            if (email == null || email.isBlank()) {
                throw new CurrentUserNotResolvedException("Current principal does not contain email");
            }
            return email;
        }

        throw new CurrentUserNotResolvedException("Unsupported authentication principal");
    }

    public User getFullCurrentUser() {
        User user = userFacade.get(getUserEmail());
        if (user == null) {
            throw new CurrentUserNotResolvedException("Authenticated user not available in local user repository");
        }
        return user;
    }
}