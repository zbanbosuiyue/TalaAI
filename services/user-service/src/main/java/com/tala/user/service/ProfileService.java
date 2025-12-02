package com.tala.user.service;

import com.tala.core.exception.ErrorCode;
import com.tala.core.exception.TalaException;
import com.tala.user.domain.Profile;
import com.tala.user.dto.ProfileRequest;
import com.tala.user.dto.ProfileResponse;
import com.tala.user.dto.ProfileUpdateRequest;
import com.tala.user.mapper.ProfileMapper;
import com.tala.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {
    
    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    
    @Transactional
    public ProfileResponse createProfile(Long userId, ProfileRequest request) {
        log.info("Creating profile for user: {}", userId);
        
        // Check if this is the first profile for the user
        List<Profile> existingProfiles = profileRepository.findByUserIdAndNotDeleted(userId);
        boolean isFirstProfile = existingProfiles.isEmpty();
        
        Profile profile = Profile.builder()
            .userId(userId)
            .babyName(request.getBabyName())
            .birthDate(request.getBirthDate())
            .timezone(request.getTimezone() != null ? request.getTimezone() : "UTC")
            .gender(request.getGender())
            .photoUrl(request.getPhotoUrl())
            .parentName(request.getParentName())
            .parentRole(request.getParentRole())
            .zipcode(request.getZipcode())
            .concerns(request.getConcerns())
            .hasDaycare(request.getHasDaycare() != null ? request.getHasDaycare() : false)
            .daycareName(request.getDaycareName())
            .updateMethod(request.getUpdateMethod())
            .isDefault(isFirstProfile) // Set as default if first profile
            .build();
        
        profile = profileRepository.save(profile);
        
        log.info("Profile created with isDefault={} for user: {}", isFirstProfile, userId);
        
        return profileMapper.toResponse(profile);
    }
    
    @Transactional(readOnly = true)
    public List<ProfileResponse> getUserProfiles(Long userId) {
        List<Profile> profiles = profileRepository.findByUserIdAndNotDeleted(userId);
        return profiles.stream()
            .map(profileMapper::toResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Long getDefaultProfileId(Long userId) {
        List<Profile> profiles = profileRepository.findByUserIdAndNotDeleted(userId);
        
        if (profiles.isEmpty()) {
            throw new TalaException(ErrorCode.PROFILE_NOT_FOUND, 
                "User has no profiles: " + userId);
        }
        
        // Return default profile if exists, otherwise return first profile
        return profiles.stream()
            .filter(Profile::getIsDefault)
            .findFirst()
            .map(Profile::getId)
            .orElse(profiles.get(0).getId());
    }
    
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long profileId) {
        Profile profile = profileRepository.findByIdAndNotDeleted(profileId)
            .orElseThrow(() -> new TalaException(ErrorCode.USER_NOT_FOUND, 
                "Profile not found"));
        return profileMapper.toResponse(profile);
    }
    
    @Transactional
    public ProfileResponse updateProfile(Long profileId, ProfileUpdateRequest request) {
        Profile profile = profileRepository.findByIdAndNotDeleted(profileId)
            .orElseThrow(() -> new TalaException(ErrorCode.USER_NOT_FOUND, 
                "Profile not found"));
        
        profileMapper.updateFromRequest(profile, request);
        
        profile = profileRepository.save(profile);
        
        return profileMapper.toResponse(profile);
    }
    
    @Transactional
    public void deleteProfile(Long profileId) {
        Profile profile = profileRepository.findByIdAndNotDeleted(profileId)
            .orElseThrow(() -> new TalaException(ErrorCode.USER_NOT_FOUND, 
                "Profile not found"));
        
        profile.softDelete();
        profileRepository.save(profile);
    }
    
}
