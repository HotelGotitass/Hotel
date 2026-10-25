package com.hotel.app.repository;

import com.hotel.app.model.entity.RoomCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoomCategoryRepository extends JpaRepository<RoomCategory, Long> {
    boolean existsByName(String name);

    // Serializa reservas web concurrentes sobre la misma categoria para evitar sobreventa
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM RoomCategory c WHERE c.id = :id")
    Optional<RoomCategory> findByIdForUpdate(@Param("id") Long id);
}
