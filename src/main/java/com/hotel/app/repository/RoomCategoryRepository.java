package com.hotel.app.repository;

import com.hotel.app.model.entity.RoomCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomCategoryRepository extends JpaRepository<RoomCategory, Long> {
    boolean existsByName(String name);
}
