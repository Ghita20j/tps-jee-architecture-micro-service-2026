package ma.emsi.accountservice.mcp;

import ma.emsi.accountservice.entities.BankAccount;
import ma.emsi.accountservice.enums.AccountType;
import ma.emsi.accountservice.services.BankAccountService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/** Fonctionnalites de l'EBank-Service exposees via MCP. */
@Component
public class AccountMcpTools {

    private final BankAccountService service;

    public AccountMcpTools(BankAccountService service) {
        this.service = service;
    }

    @Tool(name = "getAllAccounts", description = "Retourne tous les comptes bancaires avec leurs proprietaires")
    public List<BankAccount> getAllAccounts() {
        return service.findAll();
    }

    @Tool(name = "getAccountById", description = "Retourne un compte bancaire a partir de son identifiant")
    public BankAccount getAccountById(@ToolParam(description = "Identifiant du compte (UUID)") String accountId) {
        return service.findById(accountId);
    }

    @Tool(name = "getAccountsByCustomerId", description = "Retourne les comptes bancaires d'un client")
    public List<BankAccount> getAccountsByCustomerId(@ToolParam(description = "Identifiant du client") Long customerId) {
        return service.findByCustomerId(customerId);
    }

    @Tool(name = "createAccount", description = "Cree un nouveau compte bancaire pour un client")
    public BankAccount createAccount(@ToolParam(description = "Identifiant du client") Long customerId,
                                     @ToolParam(description = "Solde initial") double initialBalance,
                                     @ToolParam(description = "Devise, ex: MAD, EUR, USD") String currency,
                                     @ToolParam(description = "Type : CURRENT_ACCOUNT ou SAVING_ACCOUNT") AccountType type) {
        return service.create(customerId, initialBalance, currency, type);
    }

    @Tool(name = "deposit", description = "Effectue un versement (credit) sur un compte")
    public BankAccount deposit(@ToolParam(description = "Identifiant du compte") String accountId,
                               @ToolParam(description = "Montant a verser") double amount) {
        return service.deposit(accountId, amount);
    }

    @Tool(name = "withdraw", description = "Effectue un retrait (debit) sur un compte")
    public BankAccount withdraw(@ToolParam(description = "Identifiant du compte") String accountId,
                                @ToolParam(description = "Montant a retirer") double amount) {
        return service.withdraw(accountId, amount);
    }

    @Tool(name = "transfer", description = "Effectue un virement d'un compte vers un autre")
    public String transfer(@ToolParam(description = "Compte source") String fromAccountId,
                           @ToolParam(description = "Compte destination") String toAccountId,
                           @ToolParam(description = "Montant") double amount) {
        service.transfer(fromAccountId, toAccountId, amount);
        return "Virement de " + amount + " effectue de " + fromAccountId + " vers " + toAccountId;
    }
}
