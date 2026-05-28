package sk.posam.fsa.foodrescue.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration
public class MarketplaceHttpCacheConfiguration {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> marketplaceOffersEtagFilter() {
        ShallowEtagHeaderFilter filter = new ShallowEtagHeaderFilter();
        filter.setWriteWeakETag(true);

        FilterRegistrationBean<ShallowEtagHeaderFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.setName("marketplaceOffersEtagFilter");
        registration.addUrlPatterns("/marketplace/offers");
        registration.setOrder(Integer.MAX_VALUE - 20);
        return registration;
    }
}
