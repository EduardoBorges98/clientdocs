package com.eduardo.clientdocs.dto;

public record ApiInfoResponse(
        String service,
        String status,
        String version,
        String health,
        String docs
) {
}