package com.farmatrade.logistics.controller;

import com.farmatrade.logistics.dto.LogisticsAcceptResult;
import com.farmatrade.logistics.dto.LogisticsChoiceResponse;
import com.farmatrade.logistics.dto.OptInRequest;
import com.farmatrade.logistics.entity.LogisticsRequest;
import com.farmatrade.logistics.service.LogisticsRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class LogisticsRequestController {

    private final LogisticsRequestService logisticsRequestService;

    public LogisticsRequestController(LogisticsRequestService logisticsRequestService) {
        this.logisticsRequestService = logisticsRequestService;
    }

    /**
     * Called by bidding-service when a lot is won. Creates one LogisticsRequest row per lot
     * in PENDING_CHOICE, awaiting the buyer's accept/decline decision.
     */
    @PostMapping("/api/logistics/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public LogisticsChoiceResponse createFromLotWin(@Valid @RequestBody OptInRequest optInRequest) {
        return logisticsRequestService.createFromLotWin(optInRequest);
    }

    @GetMapping("/api/logistics/requests/{id}")
    public LogisticsRequest getById(@PathVariable Long id) {
        return logisticsRequestService.getById(id);
    }

    /**
     * Previously trusted a buyerId query param supplied by the caller -- any authenticated user
     * could list any OTHER buyer's logistics requests just by passing a different id. The buyerId
     * is now always taken from the caller's own JWT "sub" claim instead.
     */
    @GetMapping("/api/logistics/requests")
    public List<LogisticsRequest> findByBuyer(JwtAuthenticationToken authentication) {
        return logisticsRequestService.findByBuyer(extractUserId(authentication));
    }

    @PostMapping("/api/logistics/requests/{id}/accept")
    public LogisticsAcceptResult accept(@PathVariable Long id, JwtAuthenticationToken authentication) {
        return logisticsRequestService.accept(id, extractUserId(authentication));
    }

    @PostMapping("/api/logistics/requests/{id}/decline")
    public LogisticsRequest decline(@PathVariable Long id, JwtAuthenticationToken authentication) {
        return logisticsRequestService.decline(id, extractUserId(authentication));
    }

    /**
     * Resolves the caller's platform user id from the standard "sub" claim (see auth-service's
     * JwtUtil.generateToken) so accept()/decline() can verify the caller actually owns this
     * logistics request -- previously any authenticated user could accept/decline any other
     * buyer's request just by knowing its numeric id.
     */
    private Long extractUserId(JwtAuthenticationToken authentication) {
        String subject = authentication.getToken().getSubject();
        if (subject == null) {
            throw new IllegalStateException("JWT is missing required 'sub' claim");
        }
        return Long.valueOf(subject);
    }
}
