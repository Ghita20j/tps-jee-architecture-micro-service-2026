package ma.emsi.accountservice.web;

import ma.emsi.accountservice.entities.BankAccount;
import ma.emsi.accountservice.enums.AccountType;
import ma.emsi.accountservice.services.BankAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountRestController {

    public record CreateAccountRequest(Long customerId, double initialBalance, String currency, AccountType type) {}
    public record TransferRequest(String fromAccountId, String toAccountId, double amount) {}

    private final BankAccountService service;

    public AccountRestController(BankAccountService service) {
        this.service = service;
    }

    @GetMapping
    public List<BankAccount> accounts() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public BankAccount account(@PathVariable String id) {
        return service.findById(id);
    }

    @GetMapping("/customer/{customerId}")
    public List<BankAccount> accountsByCustomer(@PathVariable Long customerId) {
        return service.findByCustomerId(customerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BankAccount create(@RequestBody CreateAccountRequest request) {
        return service.create(request.customerId(), request.initialBalance(), request.currency(), request.type());
    }

    @PostMapping("/{id}/deposit")
    public BankAccount deposit(@PathVariable String id, @RequestParam double amount) {
        return service.deposit(id, amount);
    }

    @PostMapping("/{id}/withdraw")
    public BankAccount withdraw(@PathVariable String id, @RequestParam double amount) {
        return service.withdraw(id, amount);
    }

    @PostMapping("/transfer")
    public Map<String, String> transfer(@RequestBody TransferRequest request) {
        service.transfer(request.fromAccountId(), request.toAccountId(), request.amount());
        return Map.of("status", "OK");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handle(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
