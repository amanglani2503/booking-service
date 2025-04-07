package com.example.booking_service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
    private double amount;
    private Long flightId;
    private String currency;

    private String bookingId;
    private String userId;
}
