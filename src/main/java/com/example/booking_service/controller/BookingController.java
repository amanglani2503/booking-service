package com.example.booking_service.controller;

import com.example.booking_service.entity.Booking;
import com.example.booking_service.entity.BookingResponse;
import com.example.booking_service.entity.StripeResponse;
import com.example.booking_service.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    @Autowired
    private BookingService bookingService;

    @PostMapping("/book")
    public ResponseEntity<?> bookFlight(@RequestBody Booking booking) {
        logger.info("Received booking request for user: {}", booking.getEmailId());
        try {
            StripeResponse paymentResponse = bookingService.bookFlight(booking);
            logger.info("Booking and payment successful for user: {}", booking.getPassengerName());
            return ResponseEntity.ok(paymentResponse);
        } catch (RuntimeException e) {
            logger.error("Booking failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<String> cancelBooking(@PathVariable Long bookingId) {
        logger.info("Cancel request received for booking ID: {}", bookingId);
        try {
            bookingService.cancelBooking(bookingId);
            logger.info("Booking cancelled: {}", bookingId);
            return ResponseEntity.ok("Booking canceled successfully.");
        } catch (RuntimeException e) {
            logger.error("Cancellation failed for booking ID {}: {}", bookingId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getBookingDetails(@PathVariable Long bookingId) {
        logger.info("Fetching booking details for ID: {}", bookingId);
        try {
            BookingResponse booking = bookingService.getBookingDetails(bookingId);
            logger.info("Booking details retrieved for ID: {}", bookingId);
            return ResponseEntity.ok(booking);
        } catch (RuntimeException e) {
            logger.error("Failed to get booking details for ID {}: {}", bookingId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getBookingsByUser(@PathVariable String userId) {
        logger.info("Fetching bookings for user ID: {}", userId);
        List<BookingResponse> bookings = bookingService.getBookingsByUser(userId);
        if (bookings.isEmpty()) {
            logger.warn("No bookings found for user ID: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No bookings found for this user.");
        }
        logger.info("Bookings retrieved for user ID: {}", userId);
        return ResponseEntity.ok(bookings);
    }
}
