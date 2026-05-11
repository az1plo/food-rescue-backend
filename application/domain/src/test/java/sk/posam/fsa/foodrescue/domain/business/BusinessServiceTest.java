package sk.posam.fsa.foodrescue.domain.business;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sk.posam.fsa.foodrescue.domain.shared.AddressCoordinatesProvider;
import sk.posam.fsa.foodrescue.domain.shared.FoodRescueException;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.domain.user.UserRole;
import sk.posam.fsa.foodrescue.domain.user.UserStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessServiceTest {

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private AddressCoordinatesProvider addressCoordinatesProvider;

    @Mock
    private BusinessIconStorage businessIconStorage;

    @Test
    void uploadIconStoresManagedBusinessIconAndUpdatesProfile() {
        Business business = managedBusiness();
        User owner = activeUser(4L);
        BusinessIconUpload upload = new BusinessIconUpload("logo.png", "image/png", new byte[]{1, 2, 3});

        when(businessRepository.findById(business.getId())).thenReturn(Optional.of(business));
        when(businessIconStorage.store(any(BusinessIconUpload.class))).thenReturn(
                new StoredBusinessIcon("new-logo.png", "/business-icons/new-logo.png", "image/png")
        );
        when(businessRepository.save(any(Business.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BusinessService service = new BusinessService(businessRepository, addressCoordinatesProvider, businessIconStorage);

        Business updatedBusiness = service.uploadIcon(owner, business.getId(), upload);

        assertEquals("/business-icons/new-logo.png", updatedBusiness.getIconUrl());
        verify(businessRepository).save(updatedBusiness);
        verify(businessIconStorage).delete("old-logo.png");
    }

    @Test
    void uploadIconRejectsUnsupportedContentType() {
        Business business = managedBusiness();
        User owner = activeUser(4L);
        BusinessIconUpload upload = new BusinessIconUpload("logo.svg", "image/svg+xml", new byte[]{1, 2, 3});

        when(businessRepository.findById(business.getId())).thenReturn(Optional.of(business));

        BusinessService service = new BusinessService(businessRepository, addressCoordinatesProvider, businessIconStorage);

        FoodRescueException exception = assertThrows(
                FoodRescueException.class,
                () -> service.uploadIcon(owner, business.getId(), upload)
        );

        assertEquals(FoodRescueException.Type.VALIDATION, exception.getType());
    }

    private Business managedBusiness() {
        Business business = new Business();
        business.setId(11L);
        business.assignOwner(4L);
        business.setIconUrl("/business-icons/old-logo.png");
        return business;
    }

    private User activeUser(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
