package com.tala.user.controller;

import com.tala.user.dto.ProfileRequest;
import com.tala.user.dto.ProfileResponse;
import com.tala.user.dto.ProfileUpdateRequest;
import com.tala.user.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {
    
    private final ProfileService profileService;
    
    @PostMapping
    public ResponseEntity<ProfileResponse> createProfile(
        @RequestHeader("X-User-Id") Long userId,
        @Valid @RequestBody ProfileRequest request
    ) {
        log.info("POST /api/v1/profiles - userId: {}", userId);
        ProfileResponse response = profileService.createProfile(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ProfileResponse>> getUserProfiles(
        @PathVariable Long userId
    ) {
        log.info("GET /api/v1/profiles/user/{}", userId);
        List<ProfileResponse> profiles = profileService.getUserProfiles(userId);
        return ResponseEntity.ok(profiles);
    }
    
    @GetMapping("/user/{userId}/default")
    public ResponseEntity<Long> getDefaultProfileId(@PathVariable Long userId) {
        log.info("GET /api/v1/profiles/user/{}/default", userId);
        Long profileId = profileService.getDefaultProfileId(userId);
        return ResponseEntity.ok(profileId);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable Long id) {
        log.info("GET /api/v1/profiles/{}", id);
        ProfileResponse profile = profileService.getProfile(id);
        return ResponseEntity.ok(profile);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ProfileResponse> updateProfile(
        @PathVariable Long id,
        @RequestBody ProfileUpdateRequest request
    ) {
        log.info("PUT /api/v1/profiles/{}", id);
        ProfileResponse profile = profileService.updateProfile(id, request);
        return ResponseEntity.ok(profile);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable Long id) {
        log.info("DELETE /api/v1/profiles/{}", id);
        profileService.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }
}
