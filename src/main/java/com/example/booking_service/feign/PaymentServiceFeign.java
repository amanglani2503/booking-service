package com.example.booking_service.feign;

import com.example.booking_service.entity.StripeResponse;
import com.example.booking_service.entity.PaymentRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;


// Interface to communicate with payment service
@FeignClient(name = "PAYMENT-SERVICE")
public interface PaymentServiceFeign {

    // Initiates payment via checkout endpoint
    @PostMapping("/pay/checkout")
    StripeResponse makePayment(@RequestBody PaymentRequest request,
                               @RequestHeader("Authorization") String token);
}
