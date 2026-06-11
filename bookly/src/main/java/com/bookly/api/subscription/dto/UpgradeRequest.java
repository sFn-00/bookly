package com.bookly.api.subscription.dto;

import com.bookly.domain.tenant.Plan;
import jakarta.validation.constraints.NotNull;

public record UpgradeRequest(@NotNull Plan plan) {}
