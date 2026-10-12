package com.campuslink.realtime.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.io.Resource;

@Getter
@AllArgsConstructor
public class ChatMediaDownload {
    private final Resource resource;
    private final String fileName;
    private final String contentType;
    private final long sizeBytes;
}
