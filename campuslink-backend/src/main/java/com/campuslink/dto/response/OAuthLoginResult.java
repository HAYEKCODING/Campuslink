package com.campuslink.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthLoginResult {

    private AuthResponse authResponse;
    private boolean newAccount;
}
