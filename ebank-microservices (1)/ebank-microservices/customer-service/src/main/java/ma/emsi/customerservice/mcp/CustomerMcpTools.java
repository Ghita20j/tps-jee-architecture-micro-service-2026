package ma.emsi.customerservice.mcp;

import ma.emsi.customerservice.entities.Customer;
import ma.emsi.customerservice.repositories.CustomerRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fonctionnalites du Customer-Service exposees via le protocole MCP.
 * L'agent IA les decouvre automatiquement grace au client MCP.
 */
@Component
public class CustomerMcpTools {

    private final CustomerRepository customerRepository;

    public CustomerMcpTools(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Tool(name = "getAllCustomers", description = "Retourne la liste de tous les clients de la banque")
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Tool(name = "getCustomerById", description = "Retourne un client a partir de son identifiant")
    public Customer getCustomerById(@ToolParam(description = "Identifiant du client") Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client introuvable : " + id));
    }

    @Tool(name = "searchCustomers", description = "Recherche des clients par nom ou prenom (mot cle)")
    public List<Customer> searchCustomers(@ToolParam(description = "Mot cle (nom ou prenom)") String keyword) {
        return customerRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(keyword, keyword);
    }

    @Tool(name = "createCustomer", description = "Cree un nouveau client")
    public Customer createCustomer(@ToolParam(description = "Prenom") String firstName,
                                   @ToolParam(description = "Nom") String lastName,
                                   @ToolParam(description = "Email") String email) {
        return customerRepository.save(new Customer(null, firstName, lastName, email));
    }
}
