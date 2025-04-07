package com.example.booking_service.controller;

import com.example.booking_service.entity.Booking;
import com.example.booking_service.entity.BookingResponse;
import com.example.booking_service.entity.StripeResponse;
import com.example.booking_service.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping("/book")
    public ResponseEntity<?> bookFlight(@RequestBody Booking booking) {
        try {
            StripeResponse paymentResponse = bookingService.bookFlight(booking);
            return ResponseEntity.ok(paymentResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<String> cancelBooking(@PathVariable Long bookingId) {
        try {
            bookingService.cancelBooking(bookingId);
            return ResponseEntity.ok("Booking canceled successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getBookingDetails(@PathVariable Long bookingId) {
        try {
            BookingResponse booking = bookingService.getBookingDetails(bookingId);
            return ResponseEntity.ok(booking);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getBookingsByUser(@PathVariable String emailId) {
        List<BookingResponse> bookings = bookingService.getBookingsByUser(emailId);
        if (bookings.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No bookings found for this user.");
        }
        return ResponseEntity.ok(bookings);
    }
}
