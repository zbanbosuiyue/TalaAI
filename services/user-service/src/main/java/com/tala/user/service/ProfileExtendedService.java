package com.tala.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tala.core.exception.ErrorCode;
import com.tala.core.exception.TalaException;
import com.tala.user.domain.Profile;
import com.tala.user.domain.ProfileExtended;
import com.tala.user.dto.ProfileExtendedData;
import com.tala.user.repository.ProfileExtendedRepository;
import com.tala.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileExtendedService {

    private final ProfileRepository profileRepository;
    private final ProfileExtendedRepository profileExtendedRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ProfileExtendedData getExtendedProfile(Long profileId) {
        Profile profile = profileRepository.findByIdAndNotDeleted(profileId)
            .orElseThrow(() -> new TalaException(ErrorCode.USER_NOT_FOUND, "Profile not found"));

        return profileExtendedRepository.findByProfileIdAndNotDeleted(profile.getId())
            .map(this::toDto)
            .orElseGet(() -> createDefaultExtendedProfile(profile));
    }

    @Transactional
    public ProfileExtendedData saveExtendedProfile(Long profileId, ProfileExtendedData data) {
        Profile profile = profileRepository.findByIdAndNotDeleted(profileId)
            .orElseThrow(() -> new TalaException(ErrorCode.USER_NOT_FOUND, "Profile not found"));

        ProfileExtended entity = profileExtendedRepository.findByProfileIdAndNotDeleted(profile.getId())
            .orElseGet(() -> ProfileExtended.builder()
                .profileId(profile.getId())
                .data(new HashMap<>())
                .build());

        entity.getData().clear();
        Map<String, Object> map = entity.getData();
        map.put("profileId", data.getProfileId());
        map.put("measurements", data.getMeasurements());
        map.put("gender", data.getGender());
        map.put("allergies", data.getAllergies());
        map.put("medicalHistory", data.getMedicalHistory());
        map.put("milestones", data.getMilestones());
        map.put("preferences", data.getPreferences());
        map.put("emergencyContacts", data.getEmergencyContacts());
        map.put("pediatrician", data.getPediatrician());
        map.put("daycare", data.getDaycare());

        entity = profileExtendedRepository.save(entity);
        return toDto(entity);
    }

    private ProfileExtendedData toDto(ProfileExtended entity) {
        Map<String, Object> map = entity.getData();

        // Safely convert the stored JSONB map (which may contain LinkedHashMaps) into the typed DTO
        ProfileExtendedData data = objectMapper.convertValue(map, ProfileExtendedData.class);

        // Ensure profileId is always set from the owning entity if missing in JSON
        if (data.getProfileId() == null) {
            data.setProfileId(entity.getProfileId());
        }

        return data;
    }

    private ProfileExtendedData createDefaultExtendedProfile(Profile profile) {
        ProfileExtendedData data = ProfileExtendedData.builder()
            .profileId(profile.getId())
            .measurements(java.util.List.of())
            .gender(profile.getGender() != null ? profile.getGender() : "")
            .allergies(java.util.List.of())
            .medicalHistory(ProfileExtendedData.MedicalHistory.builder()
                .vaccinations(java.util.List.of())
                .pastConditions(java.util.List.of())
                .currentMedications(java.util.List.of())
                .build())
            .milestones(java.util.List.of())
            .preferences(ProfileExtendedData.Preferences.builder()
                .foods(java.util.List.of())
                .toys(java.util.List.of())
                .sleepComfort("")
                .build())
            .emergencyContacts(java.util.List.of())
            .pediatrician(null)
            .daycare(null)
            .build();

        Map<String, Object> map = new HashMap<>();
        map.put("profileId", data.getProfileId());
        map.put("measurements", data.getMeasurements());
        map.put("gender", data.getGender());
        map.put("allergies", data.getAllergies());
        map.put("medicalHistory", data.getMedicalHistory());
        map.put("milestones", data.getMilestones());
        map.put("preferences", data.getPreferences());
        map.put("emergencyContacts", data.getEmergencyContacts());
        map.put("pediatrician", data.getPediatrician());
        map.put("daycare", data.getDaycare());

        ProfileExtended entity = ProfileExtended.builder()
            .profileId(profile.getId())
            .data(map)
            .build();

        profileExtendedRepository.save(entity);
        return data;
    }
}
