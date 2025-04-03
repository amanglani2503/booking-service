package com.example.booking_service.service;

import com.example.booking_service.entity.Booking;
import com.example.booking_service.entity.BookingResponse;
import com.example.booking_service.feign.FlightServiceFeign;
import com.example.booking_service.repository.BookingRepository;
import jakarta.servlet.http.HttpServletRequest;
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

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private FlightServiceFeign flightServiceFeign;

    public BookingResponse bookFlight(Booking booking) {
        String token = extractTokenFromRequest();
        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

        boolean available = flightServiceFeign.isSeatAvailable(booking.getFlightId(), authHeader);
        if (!available) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seat is not available!");
        }

        booking.setStatus("CONFIRMED");

        // Get the booked seat number
        String seatNumber = flightServiceFeign.updateSeatAvailability(booking.getFlightId(), true, authHeader);

        BookingResponse newBooking =  new BookingResponse(booking.getUserId(), booking.getBookingId(),
                booking.getPassengerName(), booking.getFlightId(), booking.getBookingDate(), booking.getStatus(), seatNumber);

        return bookingRepository.save(newBooking);
    }

    public void cancelBooking(Long bookingId) {
        Optional<BookingResponse> booking = bookingRepository.findById(bookingId);
        if (booking.isPresent()) {
            BookingResponse existingBooking = booking.get();
            existingBooking.setStatus("CANCELED");
            bookingRepository.save(existingBooking);

            String token = extractTokenFromRequest();
            String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

            flightServiceFeign.updateSeatAvailability(existingBooking.getFlightId(), false, authHeader);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found with ID: " + bookingId);
        }
    }

    public BookingResponse getBookingDetails(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found with ID: " + bookingId));
    }

    public List<BookingResponse> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    private String extractTokenFromRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String token = request.getHeader("Authorization");
            if (token != null) {
                return token;
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization header not found");
    }
}
