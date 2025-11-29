package com.tala.media.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class MediaResponse {
    public Long id;
    public Long profileId;
    public Long userId;
    public String source;
    public String mediaType;
    public String storageUrl;
    public String thumbnailUrl;
    public Instant occurredAt;
    public List<String> aiTags;
    public Integer facesCount;
    public Map<String, Object> emotionScore;
}
