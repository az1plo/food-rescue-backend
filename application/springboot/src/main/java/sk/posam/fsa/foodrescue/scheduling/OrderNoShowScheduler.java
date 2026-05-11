package sk.posam.fsa.foodrescue.scheduling;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sk.posam.fsa.foodrescue.domain.order.OrderService;

@Component
public class OrderNoShowScheduler {

    private final OrderService orderService;

    public OrderNoShowScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(
            initialDelayString = "${foodrescue.orders.no-show-initial-delay-ms:60000}",
            fixedDelayString = "${foodrescue.orders.no-show-delay-ms:60000}"
    )
    public void settleExpiredNoShows() {
        orderService.settleExpiredNoShows();
    }
}
