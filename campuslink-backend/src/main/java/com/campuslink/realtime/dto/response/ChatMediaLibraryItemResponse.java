package com.campuslink.realtime.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMediaLibraryItemResponse {
    private Long messageId;
    private Long senderId;
    private Instant dateEnvoi;
    private String category;
    private String fileName;
    private String contentType;
    private Long sizeBytes;
    private List<String> links;
}
