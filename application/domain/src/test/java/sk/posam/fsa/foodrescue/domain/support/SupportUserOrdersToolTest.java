package sk.posam.fsa.foodrescue.domain.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sk.posam.fsa.foodrescue.domain.offer.OfferRepository;
import sk.posam.fsa.foodrescue.domain.order.OrderRepository;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.domain.user.UserRole;
import sk.posam.fsa.foodrescue.domain.user.UserStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SupportUserOrdersToolTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OfferRepository offerRepository;

    @Test
    void executeRejectsInactiveUserContext() {
        User blockedUser = new User();
        blockedUser.setId(7L);
        blockedUser.setRole(UserRole.USER);
        blockedUser.setStatus(UserStatus.BLOCKED);

        SupportUserOrdersTool tool = new SupportUserOrdersTool(orderRepository, offerRepository);

        Object result = tool.execute(
                Map.of(),
                new SupportAssistantToolContext("conv-7", "/support", "en", blockedUser)
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) result;

        assertFalse((Boolean) payload.get("authenticated"));
        assertEquals("The user is not signed in or is not active.", payload.get("message"));
        verifyNoInteractions(orderRepository, offerRepository);
    }
}
