package com.hotel.app.repository;

import com.hotel.app.model.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByUserId(Long userId);
    Optional<VerificationToken> findByUserIdAndPurpose(Long userId, String purpose);
}
