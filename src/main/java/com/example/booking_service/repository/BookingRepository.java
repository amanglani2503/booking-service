package com.example.booking_service.repository;

import com.example.booking_service.entity.BookingResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<BookingResponse, Long> {
    List<BookingResponse> findByUserId(Long userId);
}
