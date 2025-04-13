package com.example.booking_service.feign;

import com.example.booking_service.entity.MessagingDetails;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "MESSAGING-SERVICE") // Communicates with the messaging service
public interface MessagingServiceFeign {

    // Sends a notification message
    @PostMapping("message/notify")
    String sendMessage(@RequestBody MessagingDetails messagingDetails,
                       @RequestHeader("Authorization") String token);
}
