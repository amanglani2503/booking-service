package com.example.booking_service.service;

import com.example.booking_service.config.RabbitMQConfig;
import com.example.booking_service.entity.Booking;
import com.example.booking_service.entity.BookingResponse;
import com.example.booking_service.entity.FlightDetails;
import com.example.booking_service.entity.MessagingDetails;
import com.example.booking_service.feign.FlightServiceFeign;
import com.example.booking_service.repository.BookingRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private FlightServiceFeign flightServiceFeign;

    @Autowired
    private RabbitMQConfig rabbitMQConfig;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public BookingResponse bookFlight(Booking booking) {
        String token = extractTokenFromRequest();
        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

        boolean available = flightServiceFeign.isSeatAvailable(booking.getFlightId(), authHeader);
        if (!available) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seat is not available!");
        }

        booking.setStatus("CONFIRMED");

        FlightDetails flightDetails = flightServiceFeign.bookSeat(booking.getFlightId(), authHeader);

        BookingResponse newBooking = new BookingResponse(booking.getEmailId(), booking.getBookingId(),
                booking.getPassengerName(), booking.getFlightId(), booking.getBookingDate(),
                booking.getStatus(), flightDetails.getSeatNumber());

        bookingRepository.save(newBooking);

        // Create MessagingDetails Object
        MessagingDetails messagingDetails = new MessagingDetails(
                booking.getEmailId(),
                booking.getPassengerName(),
                newBooking.getBookingId(),
                booking.getFlightId(),
                flightDetails.getDepartureAirport(),
                flightDetails.getArrivalAirport(),
                flightDetails.getDepartureTime(),
                flightDetails.getArrivalTime(),
                flightDetails.getSeatNumber(),
                flightDetails.getTotalAmountPaid(),
                "CONFIRMED"
        );

        // Send message to RabbitMQ
        rabbitTemplate.convertAndSend(rabbitMQConfig.getQueueName(), messagingDetails);

        return newBooking;
    }

    public void cancelBooking(Long bookingId) {
        Optional<BookingResponse> booking = bookingRepository.findById(bookingId);
        if (booking.isPresent()) {
            BookingResponse existingBooking = booking.get();
            existingBooking.setStatus("CANCELED");
            bookingRepository.save(existingBooking);

            String token = extractTokenFromRequest();
            String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
            System.out.println("Auth Header :- " + authHeader);

            flightServiceFeign.cancelSeat(existingBooking.getFlightId(), existingBooking.getSeatNumber(), authHeader);

            // Get flight details again for the email (if needed, or store them during booking)
            FlightDetails flightDetails = flightServiceFeign.getFlightDetails(existingBooking.getFlightId(), authHeader);

            // Build MessagingDetails object
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


            rabbitTemplate.convertAndSend(rabbitMQConfig.getQueueName(), messagingDetails);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found with ID: " + bookingId);
        }
    }


    public BookingResponse getBookingDetails(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found with ID: " + bookingId));
    }

    public List<BookingResponse> getBookingsByUser(String emailId) {
        return bookingRepository.findByEmailId(emailId);
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