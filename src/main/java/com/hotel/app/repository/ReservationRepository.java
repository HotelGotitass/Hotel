package com.hotel.app.repository;

import com.hotel.app.model.entity.Reservation;
import com.hotel.app.model.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.roomCategory.id = :categoryId AND r.status IN :activeStatuses " +
           "AND (r.checkInDate < :checkOut AND r.checkOutDate > :checkIn)")
    long countOverlappingReservations(
            @Param("categoryId") Long categoryId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("activeStatuses") List<ReservationStatus> activeStatuses
    );

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.roomCategory.id = :categoryId AND r.status IN :activeStatuses " +
           "AND (r.checkInDate < :checkOut AND r.checkOutDate > :checkIn) AND r.id != :excludeId")
    long countOverlappingReservationsExcludingId(
            @Param("categoryId") Long categoryId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("activeStatuses") List<ReservationStatus> activeStatuses,
            @Param("excludeId") Long excludeId
    );

    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, java.time.LocalDateTime expiresAt);

    List<Reservation> findByUserId(Long userId);
    
    List<Reservation> findByUserIdOrderByCheckInDateDesc(Long userId);
    
    java.util.Optional<Reservation> findByBookingCode(String bookingCode);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.room.id = :roomId AND r.status IN :activeStatuses " +
           "AND (r.checkInDate < :checkOut AND r.checkOutDate > :checkIn)")
    long countOverlappingRoomReservations(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("activeStatuses") List<ReservationStatus> activeStatuses
    );
}

