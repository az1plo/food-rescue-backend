package sk.posam.fsa.foodrescue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.notification.NotificationRepository;
import sk.posam.fsa.foodrescue.domain.offer.OfferRepository;
import sk.posam.fsa.foodrescue.domain.order.OrderFacade;
import sk.posam.fsa.foodrescue.domain.order.OrderRepository;
import sk.posam.fsa.foodrescue.domain.order.OrderService;

@Configuration
public class OrderBeanConfiguration {

    @Bean
    public OrderFacade orderFacade(OrderRepository orderRepository,
                                   OfferRepository offerRepository,
                                   BusinessRepository businessRepository,
                                   NotificationRepository notificationRepository) {
        return new OrderService(orderRepository, offerRepository, businessRepository, notificationRepository);
    }
}
