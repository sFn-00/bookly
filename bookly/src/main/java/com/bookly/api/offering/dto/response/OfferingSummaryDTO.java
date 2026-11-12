package com.bookly.api.offering.dto.response;

import com.bookly.domain.offering.Offering;

import java.util.UUID;

public record OfferingSummaryDTO(
        UUID id,
        String name
) {
    public static OfferingSummaryDTO from(Offering offering) {
        return new OfferingSummaryDTO(offering.getId(), offering.getName());
    }
}
