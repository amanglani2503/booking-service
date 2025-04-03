package com.example.booking_service.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    private Long userId;
    private Long flightId;
    private String passengerName;
    private LocalDateTime bookingDate;
    private String status;

    public Booking() {
        this.bookingDate = LocalDateTime.now();
        this.status = "CONFIRMED";
    }
}
