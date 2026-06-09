package com.bookly.api.auth.dto;

public record TokenPair(String accessToken, String refreshToken) {}
