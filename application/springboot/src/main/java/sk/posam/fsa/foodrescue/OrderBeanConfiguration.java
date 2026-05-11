package sk.posam.fsa.foodrescue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.posam.fsa.foodrescue.domain.business.BusinessRepository;
import sk.posam.fsa.foodrescue.domain.notification.NotificationRepository;
import sk.posam.fsa.foodrescue.domain.offer.OfferRepository;
import sk.posam.fsa.foodrescue.domain.order.OrderFacade;
import sk.posam.fsa.foodrescue.domain.order.OrderRepository;
import sk.posam.fsa.foodrescue.domain.order.OrderService;
import sk.posam.fsa.foodrescue.domain.review.ReviewRepository;

@Configuration
public class OrderBeanConfiguration {

    @Bean
    public OrderService orderService(OrderRepository orderRepository,
                                     OfferRepository offerRepository,
                                     BusinessRepository businessRepository,
                                     NotificationRepository notificationRepository,
                                     ReviewRepository reviewRepository) {
        return new OrderService(orderRepository, offerRepository, businessRepository, notificationRepository, reviewRepository);
    }

    @Bean
    public OrderFacade orderFacade(OrderService orderService) {
        return orderService;
    }
}
