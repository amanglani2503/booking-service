package com.example.booking_service.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
public class BookingResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    private Long userId;
    private Long flightId;
    private String passengerName;
    private LocalDateTime bookingDate;
    private String status;

    private String seatNumber;

    public BookingResponse(Long userId, Long bookingId, String passengerName, Long flightId, LocalDateTime bookingDate, String status, String seatNumber) {
        this.userId = userId;
        this.bookingId = bookingId;
        this.passengerName = passengerName;
        this.flightId = flightId;
        this.bookingDate = bookingDate;
        this.status = status;
        this.seatNumber = seatNumber;
    }

    public String getSeatNumber() {
        return seatNumber;
    }
}
