package sk.posam.fsa.foodrescue.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

class JwtConverter extends AbstractAuthenticationToken {

    private final Jwt source;

    JwtConverter(Jwt source) {
        super(toAuthorities(source));
        this.source = Objects.requireNonNull(source);
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return Collections.emptyList();
    }

    @Override
    public Object getPrincipal() {
        return source;
    }

    private static Collection<? extends GrantedAuthority> toAuthorities(Jwt source) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        addAuthorities(authorities, source.getClaim("roles"), "ROLE_");
        addAuthorities(authorities, source.getClaim("authorities"), "");
        addAuthorities(authorities, source.getClaim("scope"), "SCOPE_");

        var realmAccess = source.getClaimAsMap("realm_access");
        Collection<String> realmRoles = extractStringValues(realmAccess == null ? null : realmAccess.get("roles"));
        for (String role : realmRoles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return authorities;
    }

    private static void addAuthorities(Set<GrantedAuthority> target, Object claim, String prefix) {
        for (String value : extractStringValues(claim)) {
            target.add(new SimpleGrantedAuthority(prefix + value));
        }
    }

    private static Collection<String> extractStringValues(Object claim) {
        if (claim == null) {
            return List.of();
        }

        Collection<String> values = new ArrayList<>();
        if (claim instanceof Collection<?> collection) {
            for (Object value : collection) {
                if (value != null) {
                    values.add(String.valueOf(value));
                }
            }
            return values;
        }

        if (claim instanceof String text && !text.isBlank()) {
            for (String token : text.split("[,\\s]+")) {
                if (!token.isBlank()) {
                    values.add(token.trim());
                }
            }
        }
        return values;
    }
}
