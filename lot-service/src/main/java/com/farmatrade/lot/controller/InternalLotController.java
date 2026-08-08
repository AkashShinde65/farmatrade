package com.farmatrade.lot.controller;

import com.farmatrade.lot.dto.BidUpdateRequest;
import com.farmatrade.lot.dto.MarkSoldRequest;
import com.farmatrade.lot.service.LotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service API used by bidding-service to keep a lot's auction state in sync during
 * and after bidding. Authenticated via the shared internal-service token, not a user JWT -- see
 * InternalServiceTokenFilter and SecurityConfig's "/internal/**" matcher.
 */
@RestController
@RequestMapping("/internal/lots")
public class InternalLotController {

    private final LotService lotService;

    public InternalLotController(LotService lotService) {
        this.lotService = lotService;
    }

    @PutMapping("/{id}/highest-bid")
    public ResponseEntity<Void> updateHighestBid(@PathVariable Long id, @RequestBody BidUpdateRequest request) {
        lotService.updateHighestBid(id, request.getHighestBid(), request.getHighestBidderId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/mark-sold")
    public ResponseEntity<Void> markSold(@PathVariable Long id, @RequestBody MarkSoldRequest request) {
        lotService.markSold(id, request.buyerId(), request.amount());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/close-auction")
    public ResponseEntity<Void> closeAuction(@PathVariable Long id) {
        lotService.closeAuction(id);
        return ResponseEntity.ok().build();
    }
}
