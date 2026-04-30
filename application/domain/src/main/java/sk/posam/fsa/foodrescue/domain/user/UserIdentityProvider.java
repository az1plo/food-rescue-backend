package sk.posam.fsa.foodrescue.domain.user;

public interface UserIdentityProvider {

    void create(User user);

    void deleteByEmail(String email);
}


