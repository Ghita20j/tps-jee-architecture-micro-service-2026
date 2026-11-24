package ma.emsi.customerservice;

import ma.emsi.customerservice.entities.Customer;
import ma.emsi.customerservice.mcp.CustomerMcpTools;
import ma.emsi.customerservice.repositories.CustomerRepository;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }

    /** Expose les methodes annotees @Tool comme outils MCP. */
    @Bean
    public ToolCallbackProvider customerTools(CustomerMcpTools customerMcpTools) {
        return MethodToolCallbackProvider.builder().toolObjects(customerMcpTools).build();
    }

    @Bean
    CommandLineRunner initCustomers(CustomerRepository customerRepository) {
        return args -> {
            List.of(
                    new Customer(null, "Mohamed", "Alaoui", "mohamed@gmail.com"),
                    new Customer(null, "Imane", "Bennani", "imane@gmail.com"),
                    new Customer(null, "Yassine", "El Idrissi", "yassine@gmail.com"),
                    new Customer(null, "Salma", "Tazi", "salma@gmail.com")
            ).forEach(customerRepository::save);
            customerRepository.findAll().forEach(c ->
                    System.out.println("Customer : " + c.getId() + " - " + c.getFirstName() + " " + c.getLastName()));
        };
    }
}
