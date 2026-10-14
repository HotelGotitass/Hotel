package com.hotel.app.repository;

import com.hotel.app.model.entity.ExtraService;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExtraServiceRepository extends JpaRepository<ExtraService, Long> {
    boolean existsByName(String name);
    List<ExtraService> findByIsActiveTrue();
}
