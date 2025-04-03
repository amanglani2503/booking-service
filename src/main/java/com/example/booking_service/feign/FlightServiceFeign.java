package com.example.booking_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "FLIGHT-SERVICE")
public interface FlightServiceFeign {

    @GetMapping("/flights/check-availability")
    boolean isSeatAvailable(@RequestParam Long flightId,
                            @RequestHeader("Authorization") String token);

    @PutMapping("/flights/update-seats")
    String updateSeatAvailability(@RequestParam Long flightId,
                                @RequestParam boolean isBooking,
                                @RequestHeader("Authorization") String token);
}
