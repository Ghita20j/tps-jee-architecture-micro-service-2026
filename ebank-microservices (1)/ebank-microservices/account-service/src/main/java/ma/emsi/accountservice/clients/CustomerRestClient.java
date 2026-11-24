package ma.emsi.accountservice.clients;

import ma.emsi.accountservice.model.Customer;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Client declaratif OpenFeign vers customer-service.
 * "customer-service" est resolu via Eureka (load balancing).
 * Si le service est indisponible, le Circuit Breaker Resilience4j bascule sur le fallback.
 */
@FeignClient(name = "customer-service", fallback = CustomerRestClientFallback.class)
public interface CustomerRestClient {

    @GetMapping("/api/customers/{id}")
    Customer findCustomerById(@PathVariable("id") Long id);

    @GetMapping("/api/customers")
    List<Customer> allCustomers();
}
