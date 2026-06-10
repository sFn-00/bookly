package com.bookly.api.auth.dto.response;

public record TokenPair(String accessToken, String refreshToken) {}
