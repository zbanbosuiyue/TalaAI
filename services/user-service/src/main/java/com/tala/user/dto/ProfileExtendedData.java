package com.tala.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileExtendedData {

    private Long profileId;
    private List<Measurement> measurements;
    private String gender;
    private List<Allergy> allergies;
    private MedicalHistory medicalHistory;
    private List<Milestone> milestones;
    private Preferences preferences;
    private List<EmergencyContact> emergencyContacts;
    private Pediatrician pediatrician;
    private Daycare daycare;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Measurement {
        private String date;
        private Double weight;
        private Double height;
        private Double head;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Allergy {
        private String id;
        private String name;
        private String emoji;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Vaccination {
        private String id;
        private String name;
        private String date;
        private String nextDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PastCondition {
        private String id;
        private String name;
        private String date;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentMedication {
        private String id;
        private String name;
        private String dosage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Milestone {
        private String id;
        private String name;
        private String status;
        private String date;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmergencyContact {
        private String id;
        private String name;
        private String relation;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pediatrician {
        private String name;
        private String clinic;
        private String phone;
        private String address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Daycare {
        private String name;
        private String address;
        private String phone;
        private String teacherName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Preferences {
        private List<String> foods;
        private List<String> toys;
        private String sleepComfort;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicalHistory {
        private List<Vaccination> vaccinations;
        private List<PastCondition> pastConditions;
        private List<CurrentMedication> currentMedications;
    }
}
