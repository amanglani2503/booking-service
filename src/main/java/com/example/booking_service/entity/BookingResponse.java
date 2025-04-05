package com.example.booking_service.entity;

import jakarta.persistence.*;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
public class BookingResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    private String emailId;
    private Long flightId;
    private String passengerName;
    private LocalDateTime bookingDate;
    private String status;

    private String seatNumber;

    public BookingResponse(String emailId, Long bookingId, String passengerName, Long flightId, LocalDateTime bookingDate, String status, String seatNumber) {
        this.emailId = emailId;
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
