package com.campuslink.realtime.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CallSignalRequest {
    @NotNull
    private Long matchId;

    @NotNull
    @Pattern(regexp = "^[0-9a-fA-F-]{36}$")
    private String callId;

    @NotNull
    private CallSignalType type;

    private CallMediaType mediaType;

    private JsonNode payload;

    public enum CallSignalType {
        INVITE, ACCEPT, REJECT, CANCEL, OFFER, ANSWER, ICE, END, MISSED, ERROR
    }

    public enum CallMediaType {
        AUDIO, VIDEO
    }
}
