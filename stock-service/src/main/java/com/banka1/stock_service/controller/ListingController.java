package com.banka1.stock_service.controller;

import com.banka1.stock_service.dto.ListingRefreshResponse;
import com.banka1.stock_service.service.ListingMarketDataRefreshService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Listing API for manual market-data refresh operations.
 */
@RestController
@RequiredArgsConstructor
public class ListingController {

    private final ListingMarketDataRefreshService listingMarketDataRefreshService;

    /**
     * Manually refreshes one listing snapshot by id.
     *
     * @param id listing identifier
     * @return refresh summary
     */
    @PostMapping("/api/listings/{id}/refresh")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ListingRefreshResponse> refreshListing(@PathVariable Long id) {
        ListingRefreshResponse response = listingMarketDataRefreshService.refreshListing(id);
        return ResponseEntity.ok(response);
    }
}
