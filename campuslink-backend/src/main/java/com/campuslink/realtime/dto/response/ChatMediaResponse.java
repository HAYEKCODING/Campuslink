package com.campuslink.realtime.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMediaResponse {
    private String fileName;
    private String contentType;
    private Long sizeBytes;
}
