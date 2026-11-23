package com.banking.accountservice.controller;


import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("api/v1/accounts")
@Slf4j
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request){

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(request));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable String accountNumber){

        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<BigDecimal> getBalance(
            @PathVariable String accountNumber){

        return ResponseEntity.ok(accountService.getBalance(accountNumber));
    }

    @PutMapping("/{accountNumber}")
    public ResponseEntity<String> blockAccount(
            @PathVariable String accountNumber){
        accountService.blockAccount(accountNumber);
        return ResponseEntity.ok("Account blocked Sucessfully");
    }

     /**
      * SAGA STEP 1 DEDUCT BALANCE
      * CALLED BY TRANSACTION SERVICE WHEN TRANSFER IS INITITED
      * */
     @PutMapping("/{accountNumber}/deduct")
     public ResponseEntity<String> deductBalance(
             @PathVariable String accountNumber,
             @RequestParam BigDecimal amount){
         accountService.deductBalance(accountNumber,amount);
         return ResponseEntity.ok("Account deducted Successfully");
     }
    /**
     * SAGA STEP 4 - COMPENSATING TRANSACTION ENDPOINT
     * CALLED BY TRANSACTION SERVICE IN TWO SCENARIOS:
     * 1. Fraud detected -> refund sender(undo step1)
     * 2. Transaction completed -> credit receiver
     */

    @PutMapping("/{accountNumber}/credit")
    public ResponseEntity<String> creditBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount){
        accountService.creditBalance(accountNumber,amount);
        return ResponseEntity.ok("BALANCE CREDITED SUCCESSFULLY");
    }



}
