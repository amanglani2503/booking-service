package com.example.booking_service.feign;

import com.example.booking_service.entity.Booking;
import com.example.booking_service.entity.FlightDetails;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "FLIGHT-SERVICE")
public interface FlightServiceFeign {

    @GetMapping("/flights/check-availability")
    boolean isSeatAvailable(@RequestParam Long flightId,
                            @RequestHeader("Authorization") String token);

    @PutMapping("/flights/book-seats")
    FlightDetails bookSeat(@RequestParam Long flightId,
                              @RequestHeader("Authorization") String token);

    @PutMapping("/flights/cancel-seat")
    void cancelSeat(@RequestParam Long flightId,
                    @RequestParam String seatNumber, @RequestHeader("Authorization") String authHeader);

    @GetMapping("flights/getDetails")
    FlightDetails getFlightDetails(@RequestParam Long flightId, @RequestHeader("Authorization") String authHeader);
}
