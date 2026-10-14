package com.hotel.app.repository;

import com.hotel.app.model.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface GuestRepository extends JpaRepository<Guest, Long> {
    boolean existsByDocumentNumber(String documentNumber);
    Optional<Guest> findByDocumentNumber(String documentNumber);
    Optional<Guest> findByEmail(String email);
    List<Guest> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);
}
