package ma.emsi.accountservice.services;

import ma.emsi.accountservice.clients.CustomerRestClient;
import ma.emsi.accountservice.entities.BankAccount;
import ma.emsi.accountservice.enums.AccountType;
import ma.emsi.accountservice.repositories.BankAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BankAccountService {

    private final BankAccountRepository repository;
    private final CustomerRestClient customerRestClient;

    public BankAccountService(BankAccountRepository repository, CustomerRestClient customerRestClient) {
        this.repository = repository;
        this.customerRestClient = customerRestClient;
    }

    private BankAccount withCustomer(BankAccount account) {
        account.setCustomer(customerRestClient.findCustomerById(account.getCustomerId()));
        return account;
    }

    public List<BankAccount> findAll() {
        return repository.findAll().stream().map(this::withCustomer).toList();
    }

    public BankAccount findById(String accountId) {
        BankAccount account = repository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable : " + accountId));
        return withCustomer(account);
    }

    public List<BankAccount> findByCustomerId(Long customerId) {
        return repository.findByCustomerId(customerId).stream().map(this::withCustomer).toList();
    }

    public BankAccount create(Long customerId, double initialBalance, String currency, AccountType type) {
        BankAccount account = new BankAccount(UUID.randomUUID().toString(), initialBalance, LocalDate.now(),
                currency == null ? "MAD" : currency,
                type == null ? AccountType.CURRENT_ACCOUNT : type, customerId);
        return withCustomer(repository.save(account));
    }

    public BankAccount deposit(String accountId, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Le montant doit etre positif");
        BankAccount account = findById(accountId);
        account.setBalance(account.getBalance() + amount);
        return repository.save(account);
    }

    public BankAccount withdraw(String accountId, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Le montant doit etre positif");
        BankAccount account = findById(accountId);
        if (account.getBalance() < amount) throw new IllegalArgumentException("Solde insuffisant");
        account.setBalance(account.getBalance() - amount);
        return repository.save(account);
    }

    public void transfer(String fromAccountId, String toAccountId, double amount) {
        withdraw(fromAccountId, amount);
        deposit(toAccountId, amount);
    }
}
