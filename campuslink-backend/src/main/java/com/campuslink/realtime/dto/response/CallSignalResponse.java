package com.campuslink.realtime.dto.response;

import com.campuslink.realtime.dto.request.CallSignalRequest.CallMediaType;
import com.campuslink.realtime.dto.request.CallSignalRequest.CallSignalType;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CallSignalResponse {
    private Long matchId;
    private String callId;
    private Long fromUserId;
    private CallSignalType type;
    private CallMediaType mediaType;
    private JsonNode payload;
}
