package com.farmatrade.lot.controller;

import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.farmatrade.lot.dto.CreateLotRequest;
import com.farmatrade.lot.dto.LotResponse;
import com.farmatrade.lot.enums.LotStatus;
import com.farmatrade.lot.enums.SaleType;
import com.farmatrade.lot.exception.InvalidLotException;
import com.farmatrade.lot.service.LotService;
import jakarta.validation.Valid;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/lots")
@Validated
public class LotController {

	private final LotService lotService;

	public LotController(LotService lotService) {
	    this.lotService = lotService;
	}

	@PostMapping(
	        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
	        produces = MediaType.APPLICATION_JSON_VALUE
	)
	public ResponseEntity<LotResponse> createLot(
	        @Valid @ModelAttribute CreateLotRequest request,

	        @RequestPart(value = "image", required = false)
	        MultipartFile image) {

	    LotResponse response = lotService.createLot(request, image);

	    return new ResponseEntity<>(response, HttpStatus.CREATED);
	}
    
    
    @GetMapping
    public ResponseEntity<List<LotResponse>> getAllLots() {

        List<LotResponse> lots = lotService.getAllLots();
        return ResponseEntity.ok(lots);
    }
    
    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<List<LotResponse>> getLotsByFarmer(
            @PathVariable Long farmerId) {

        List<LotResponse> lots = lotService.getLotsByFarmer(farmerId);

        return ResponseEntity.ok(lots);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<LotResponse>> searchLotsByCrop(
            @RequestParam(name = "cropName", required = true) String cropName) {

        if (cropName == null || cropName.trim().isEmpty()) {
            throw new InvalidLotException(
                    "Crop name is required for search.");
        }

        List<LotResponse> lots =
                lotService.searchLotsByCrop(cropName.trim());

        return ResponseEntity.ok(lots);
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<LotResponse>> getLotsByStatus(
            @PathVariable String status) {

        LotStatus lotStatus;

        try {
            lotStatus = LotStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidLotException(
                    "Invalid status. Allowed values: ACTIVE, SOLD, CANCELLED, EXPIRED");
        }

        List<LotResponse> lots = lotService.getLotsByStatus(lotStatus);

        return ResponseEntity.ok(lots);
    }
    
    
    @GetMapping("/sale-type/{saleType}")
    public ResponseEntity<List<LotResponse>> getLotsBySaleType(
            @PathVariable String saleType) {

        SaleType type;

        try {
            type = SaleType.valueOf(saleType.toUpperCase());
        } catch (IllegalArgumentException e) {

            throw new InvalidLotException(
                    "Invalid sale type. Allowed values: AUCTION, FIXED_PRICE");
        }

        List<LotResponse> lots =
                lotService.getLotsBySaleType(type);

        return ResponseEntity.ok(lots);
    }
    
    
    @GetMapping("/{id}")
    public ResponseEntity<LotResponse> getLotById(@PathVariable Long id) {

        LotResponse response = lotService.getLotById(id);

        return ResponseEntity.ok(response);
    }
    
    
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<LotResponse> updateLot(
            @PathVariable Long id,

            @RequestPart("lot")
            String lotJson,

            @RequestPart(value = "image", required = false)
            MultipartFile image) {

        try {

            ObjectMapper objectMapper = new ObjectMapper();

            CreateLotRequest request =
                    objectMapper.readValue(lotJson, CreateLotRequest.class);

            LotResponse response =
                    lotService.updateLot(id, request, image);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            throw new InvalidLotException(
                    "Invalid lot JSON: " + e.getMessage());
        }
    }
    
    
    @PutMapping(
            value = "/{id}/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<LotResponse> updateLotImage(
            @PathVariable Long id,

            @RequestPart("image")
            MultipartFile image) throws IOException {

        LotResponse response =
                lotService.updateLotImage(id, image);

        return ResponseEntity.ok(response);
    }
    
	 
	 // Cancel Lot
	 @DeleteMapping("/{id}")
	 public ResponseEntity<LotResponse> cancelLot(
	         @PathVariable Long id) {
	
	     LotResponse response = lotService.cancelLot(id);
	
	     return ResponseEntity.ok(response);
	 }
	 
	 
	// Update Lot Status
	@PutMapping("/status/{id}")
	public ResponseEntity<LotResponse> updateLotStatus(
	        @PathVariable Long id,
	        @RequestParam LotStatus status) {

	    LotResponse response =
	            lotService.updateLotStatus(id, status);

	    return ResponseEntity.ok(response);
	}
    

}