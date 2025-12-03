package com.tala.user.domain;

import com.tala.core.domain.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.util.Map;

@Entity
@Table(name = "profile_extended", schema = "users")
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileExtended extends BaseEntity {

    @Column(name = "profile_id", nullable = false, unique = true)
    private Long profileId;

    @Type(JsonBinaryType.class)
    @Column(name = "data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> data;
}
