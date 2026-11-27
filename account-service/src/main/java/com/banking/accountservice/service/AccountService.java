package com.banking.accountservice.service;


import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.AccountStatus;
import com.banking.accountservice.entity.AccountType;
import com.banking.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private static SecureRandom secureRandom = new SecureRandom();

    public AccountResponse createAccount(CreateAccountRequest request){
        log.info("am creating account for :{}",request.getEmail());

        if(accountRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException("Account already exists for email "+ request.getEmail());
        }
        Account account = new Account();
        account.setAccountHolderName(request.getAccountHolderName());
        account.setEmail(request.getEmail());
        account.setAccountType(request.getAccountType());
        account.setId(generateAccountNumber());
        account.setBalance(request.getInitialDeposit());
        account.setStatus(AccountStatus.ACTIVE);
        account.setPhone(request.getPhone());
        account.setDailyTransactionLimit(
                request.getAccountType() == AccountType.SAVINGS
                ? new BigDecimal("100000")
                        :new BigDecimal("500000")

        );

        Account savedAccount = accountRepository.save(account);
        return mapToResponse(savedAccount);
    }
    private AccountResponse mapToResponse(Account account){
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setAccountHolderName(account.getAccountHolderName());
        response.setAccountType(account.getAccountType());
        response.setAccountNumber(account.getAccountNumber());
        response.setEmail(account.getEmail());
        response.setBalance(account.getBalance());
        response.setStatus(account.getStatus());
        response.setDailyTransactionLimit(account.getDailyTransactionLimit());
        response.setCreatedAt(account.getCreatedAt());
        response.setPhone(account.getPhone());
        return response;
    }
    private String generateAccountNumber(){
        String accountNumber;
        do{

            long number = secureRandom.nextLong(1_000_000_000_000L);
            accountNumber = String.format("%012d",number);

        } while(accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }


    /**
     * Get account by account number
     * @param accountNumber
     * @return
     */

    public AccountResponse getAccount(String accountNumber){
        Account account = accountRepository.findAccountByAccountNumber(accountNumber)
                .orElseThrow(()->new RuntimeException("Account not found"));
        return mapToResponse(account);
    }

    /**
     * Get account balance
     * @param accountNumber
     * @return
     */
    public BigDecimal getBalance(String accountNumber){
        Account account = accountRepository.findAccountByAccountNumber(accountNumber)
                .orElseThrow(()->new RuntimeException("Account not found"));
        return account.getBalance();
    }

    /**
     * Block account - called by Fraud detection service via kafka
     * @param accountNumber
     */
    public void blockAccount(String accountNumber){
        log.info("Blocking account :{}",accountNumber);
        Account account = accountRepository.findAccountByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        account.setStatus(AccountStatus.BLOCKED);
        accountRepository.save(account);
        log.info("account blocked", accountNumber);
    }

    /**
     * Deduct balance from sender account
     * Called by Transaction service
     * @param accountNumber
     * @param amount
     */
    public void deductBalance(String accountNumber,BigDecimal amount){
        Account account = accountRepository.findAccountByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if(account.getStatus() != AccountStatus.ACTIVE){
            throw new RuntimeException("Account is not active"+accountNumber);
        }
        BigDecimal balance = account.getBalance();
        if (balance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }
        BigDecimal remainingAmount = balance.subtract(amount);
        account.setBalance(remainingAmount);
        accountRepository.save(account);
        log.info("Balance is deducted in this accountNumber :{}",accountNumber);
        log.info("Balance is updated:{}",account.getBalance());
    }

    /**
     * Called by transaction service via kafka
     * Fraud detection
     * Transaction completed -> credit receiver
     * @param accountNumber
     * @param amount
     */
    public void creditBalance(String accountNumber,BigDecimal amount){
        Account account = accountRepository.findAccountByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));


        BigDecimal balance = account.getBalance();
        account.setBalance(balance.add(amount));
        accountRepository.save(account);
        log.info("Balance is credited to this account {}",accountNumber);
    }

}
