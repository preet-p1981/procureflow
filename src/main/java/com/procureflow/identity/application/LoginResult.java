package com.procureflow.identity.application;

import com.procureflow.identity.application.port.AccessToken;

public record LoginResult(AccessToken accessToken, String refreshToken) {}