package com.tala.user.repository;

import com.tala.user.domain.ProfileExtended;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileExtendedRepository extends JpaRepository<ProfileExtended, Long> {

    @Query("SELECT p FROM ProfileExtended p WHERE p.profileId = :profileId AND p.deletedAt IS NULL")
    Optional<ProfileExtended> findByProfileIdAndNotDeleted(Long profileId);
}
