package com.hotel.app.repository;

import com.hotel.app.model.entity.Room;
import com.hotel.app.model.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByRoomNumber(String roomNumber);
    boolean existsByCategoryId(Long categoryId);
    long countByCategoryId(Long categoryId);
    
    @Query("SELECT r FROM Room r WHERE (:status IS NULL OR r.status = :status) AND (:categoryId IS NULL OR r.category.id = :categoryId)")
    List<Room> findByEstadoAndCategory(@Param("status") RoomStatus status, @Param("categoryId") Long categoryId);
    long countByCategoryIdAndStatus(Long categoryId, RoomStatus status);
}
