package com.example.booking_service.service;

import com.example.booking_service.entity.*;
import com.example.booking_service.feign.FlightServiceFeign;
import com.example.booking_service.feign.MessagingServiceFeign;
import com.example.booking_service.feign.PaymentServiceFeign;
import com.example.booking_service.repository.BookingRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BookingServiceTest {

    @InjectMocks
    private BookingService bookingService;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FlightServiceFeign flightServiceFeign;

    @Mock
    private MessagingServiceFeign messagingServiceFeign;

    @Mock
    private PaymentServiceFeign paymentServiceFeign;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        when(attributes.getRequest()).thenReturn(request);
        when(request.getHeader("Authorization")).thenReturn("Bearer mock_token");
        RequestContextHolder.setRequestAttributes(attributes);
    }

    @Test
    void testGetBookingDetails_found() {
        BookingResponse booking = new BookingResponse("user@example.com", 1L, "John Doe", 101L, LocalDateTime.now(), "CONFIRMED", "A1");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        BookingResponse result = bookingService.getBookingDetails(1L);

        assertEquals("John Doe", result.getPassengerName());
        verify(bookingRepository).findById(1L);
    }

    @Test
    void testGetBookingDetails_notFound() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> bookingService.getBookingDetails(1L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testGetBookingsByUser() {
        List<BookingResponse> mockList = List.of(
                new BookingResponse("user@example.com", 1L, "John", 123L, LocalDateTime.now(), "CONFIRMED", "B1")
        );
        when(bookingRepository.findByEmailId("user@example.com")).thenReturn(mockList);

        List<BookingResponse> result = bookingService.getBookingsByUser("user@example.com");

        assertEquals(1, result.size());
        verify(bookingRepository).findByEmailId("user@example.com");
    }

    @Test
    void testCancelBooking_found() {
        BookingResponse booking = new BookingResponse("user@example.com", 1L, "Jane", 123L, LocalDateTime.now(), "CONFIRMED", "C2");

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        FlightDetails flightDetails = new FlightDetails(
                "IndiGo", "C2", "DEL", "MUM",
                LocalDateTime.of(2025, 4, 15, 10, 0),
                LocalDateTime.of(2025, 4, 15, 12, 0),
                100.0
        );
        when(flightServiceFeign.getFlightDetails(anyLong(), anyString())).thenReturn(flightDetails);
        bookingService.cancelBooking(1L);

        verify(bookingRepository).save(argThat(b -> b.getStatus().equals("CANCELED")));
        verify(flightServiceFeign).cancelSeat(eq(123L), eq("C2"), anyString());
        verify(messagingServiceFeign).sendMessage(any(MessagingDetails.class), anyString());
    }

    @Test
    void testCancelBooking_notFound() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> bookingService.cancelBooking(999L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
