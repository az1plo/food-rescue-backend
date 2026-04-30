package sk.posam.fsa.foodrescue.domain.user;

public interface UserFacade {

    User get(Long id);

    User get(String email);

    void create(User user);

    void register(User user);

    User provisionExternalIdentity(User user);
}


