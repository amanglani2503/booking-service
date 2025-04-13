package com.example.booking_service.service;

import com.example.booking_service.entity.*;
import com.example.booking_service.feign.FlightServiceFeign;
import com.example.booking_service.feign.MessagingServiceFeign;
import com.example.booking_service.feign.PaymentServiceFeign;
import com.example.booking_service.repository.BookingRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private FlightServiceFeign flightServiceFeign;

    @Autowired
    private MessagingServiceFeign messagingServiceFeign;

    @Autowired
    private PaymentServiceFeign paymentServiceFeign;

    // Handles flight booking and payment
    public StripeResponse bookFlight(Booking booking) {
        logger.info("Initiating booking process for user: {}, flight ID: {}", booking.getEmailId(), booking.getFlightId());
        String token = extractTokenFromRequest();
        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

        boolean available = flightServiceFeign.isSeatAvailable(booking.getFlightId(), authHeader);
        if (!available) {
            logger.warn("Seat not available for flight ID: {}", booking.getFlightId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seat is not available!");
        }

        BookingResponse pendingBooking = new BookingResponse(
                booking.getEmailId(),
                booking.getBookingId(),
                booking.getPassengerName(),
                booking.getFlightId(),
                booking.getBookingDate(),
                "PENDING",
                null
        );
        bookingRepository.save(pendingBooking);
        logger.debug("Saved booking as PENDING: {}", pendingBooking);

        try {
            FlightDetails flightDetails = flightServiceFeign.bookSeat(booking.getFlightId(), authHeader);
            logger.info("Seat booked successfully for flight ID: {}", booking.getFlightId());

            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setAmount(flightDetails.getTotalAmountPaid());
            paymentRequest.setCurrency("usd");
            paymentRequest.setFlightId(booking.getFlightId());
            paymentRequest.setBookingId(String.valueOf(booking.getBookingId()));
            paymentRequest.setUserId(booking.getEmailId());

            StripeResponse paymentResponse = paymentServiceFeign.makePayment(paymentRequest, authHeader);
            if (!"SUCCESS".equalsIgnoreCase(paymentResponse.getStatus())) {
                logger.error("Payment failed: {}", paymentResponse.getMessage());
                throw new IllegalStateException("Payment failed: " + paymentResponse.getMessage());
            }

            BookingResponse confirmedBooking = new BookingResponse(
                    booking.getEmailId(),
                    booking.getBookingId(),
                    booking.getPassengerName(),
                    booking.getFlightId(),
                    booking.getBookingDate(),
                    "CONFIRMED",
                    flightDetails.getSeatNumber()
            );
            bookingRepository.save(confirmedBooking);
            logger.info("Booking confirmed for user: {}", booking.getEmailId());

            MessagingDetails messagingDetails = new MessagingDetails(
                    confirmedBooking.getEmailId(),
                    confirmedBooking.getPassengerName(),
                    confirmedBooking.getBookingId(),
                    confirmedBooking.getFlightId(),
                    flightDetails.getDepartureAirport(),
                    flightDetails.getArrivalAirport(),
                    flightDetails.getDepartureTime(),
                    flightDetails.getArrivalTime(),
                    flightDetails.getSeatNumber(),
                    flightDetails.getTotalAmountPaid(),
                    "CONFIRMED"
            );
            messagingServiceFeign.sendMessage(messagingDetails, authHeader);
            logger.debug("Confirmation message sent for booking ID: {}", confirmedBooking.getBookingId());

            return paymentResponse;

        } catch (Exception ex) {
            logger.error("Exception occurred during booking: {}", ex.getMessage(), ex);
            BookingResponse failedBooking = new BookingResponse(
                    booking.getEmailId(),
                    booking.getBookingId(),
                    booking.getPassengerName(),
                    booking.getFlightId(),
                    booking.getBookingDate(),
                    "FAILED",
                    null
            );
            bookingRepository.save(failedBooking);
            logger.warn("Booking marked as FAILED: {}", failedBooking);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking failed: " + ex.getMessage());
        }
    }

    // Handles booking cancellation
    public void cancelBooking(Long bookingId) {
        logger.info("Cancelling booking ID: {}", bookingId);
        Optional<BookingResponse> booking = bookingRepository.findById(bookingId);
        if (booking.isPresent()) {
            BookingResponse existingBooking = booking.get();
            existingBooking.setStatus("CANCELED");
            bookingRepository.save(existingBooking);
            logger.debug("Booking status set to CANCELED: {}", bookingId);

            String token = extractTokenFromRequest();
            String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

            flightServiceFeign.cancelSeat(existingBooking.getFlightId(), existingBooking.getSeatNumber(), authHeader);
            logger.info("Seat canceled in flight service for booking ID: {}", bookingId);

            FlightDetails flightDetails = flightServiceFeign.getFlightDetails(existingBooking.getFlightId(), authHeader);

            MessagingDetails messagingDetails = new MessagingDetails(
                    existingBooking.getEmailId(),
                    existingBooking.getPassengerName(),
                    existingBooking.getBookingId(),
                    existingBooking.getFlightId(),
                    flightDetails.getDepartureAirport(),
                    flightDetails.getArrivalAirport(),
                    flightDetails.getDepartureTime(),
                    flightDetails.getArrivalTime(),
                    existingBooking.getSeatNumber(),
                    flightDetails.getTotalAmountPaid(),
                    "CANCELED"
            );
            messagingServiceFeign.sendMessage(messagingDetails, authHeader);
            logger.debug("Cancellation message sent for booking ID: {}", bookingId);
        } else {
            logger.error("Booking not found for cancellation, ID: {}", bookingId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found with ID: " + bookingId);
        }
    }

    public BookingResponse getBookingDetails(Long bookingId) {
        logger.info("Fetching booking details for ID: {}", bookingId);
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    logger.warn("Booking not found with ID: {}", bookingId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found with ID: " + bookingId);
                });
    }

    public List<BookingResponse> getBookingsByUser(String emailId) {
        logger.info("Fetching all bookings for user: {}", emailId);
        return bookingRepository.findByEmailId(emailId);
    }

    private String extractTokenFromRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String token = request.getHeader("Authorization");
            if (token != null) {
                logger.debug("Authorization token extracted from request");
                return token;
            }
        }
        logger.error("Authorization header not found in request");
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization header not found");
    }
}
