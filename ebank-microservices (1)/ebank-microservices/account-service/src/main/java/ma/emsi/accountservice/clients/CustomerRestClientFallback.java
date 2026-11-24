package ma.emsi.accountservice.clients;

import ma.emsi.accountservice.model.Customer;
import org.springframework.stereotype.Component;

import java.util.List;

/** Reponses par defaut quand customer-service ne repond pas (Circuit Breaker ouvert / erreur). */
@Component
public class CustomerRestClientFallback implements CustomerRestClient {

    @Override
    public Customer findCustomerById(Long id) {
        return new Customer(id, "Not Available", "Not Available", "Not Available");
    }

    @Override
    public List<Customer> allCustomers() {
        return List.of();
    }
}
