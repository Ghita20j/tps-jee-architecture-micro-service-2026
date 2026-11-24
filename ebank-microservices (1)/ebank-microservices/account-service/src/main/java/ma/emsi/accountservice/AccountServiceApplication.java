package ma.emsi.accountservice;

import ma.emsi.accountservice.entities.BankAccount;
import ma.emsi.accountservice.enums.AccountType;
import ma.emsi.accountservice.mcp.AccountMcpTools;
import ma.emsi.accountservice.repositories.BankAccountRepository;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@SpringBootApplication
@EnableFeignClients
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider accountTools(AccountMcpTools accountMcpTools) {
        return MethodToolCallbackProvider.builder().toolObjects(accountMcpTools).build();
    }

    /** Donnees de test : 2 comptes (courant + epargne) pour les clients 1..4 */
    @Bean
    CommandLineRunner initAccounts(BankAccountRepository repository) {
        return args -> {
            List.of(1L, 2L, 3L, 4L).forEach(customerId -> {
                repository.save(new BankAccount(UUID.randomUUID().toString(),
                        Math.round(Math.random() * 90000 + 1000), LocalDate.now(), "MAD",
                        AccountType.CURRENT_ACCOUNT, customerId));
                repository.save(new BankAccount(UUID.randomUUID().toString(),
                        Math.round(Math.random() * 90000 + 1000), LocalDate.now(), "MAD",
                        AccountType.SAVING_ACCOUNT, customerId));
            });
            repository.findAll().forEach(a ->
                    System.out.println(a.getAccountId() + " | " + a.getType() + " | " + a.getBalance() + " | customer=" + a.getCustomerId()));
        };
    }
}
