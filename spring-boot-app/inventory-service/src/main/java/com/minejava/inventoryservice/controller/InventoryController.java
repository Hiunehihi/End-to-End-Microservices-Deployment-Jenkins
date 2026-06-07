package com.minejava.inventoryservice.controller;

import java.util.List;
import java.util.Map;

import com.minejava.inventoryservice.dto.InventoryResponse;
import com.minejava.inventoryservice.service.InventoryService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryResponse> isInStock(@RequestParam List<String> skuCode) {
        log.info("Received inventory check request for skuCode: {}", skuCode);
        return inventoryService.isInStock(skuCode);
    }

    @PostMapping("/init")
    @ResponseStatus(HttpStatus.CREATED)
    public void initInventory(@RequestBody Map<String, Object> data) {
        String skuCode = (String) data.get("skuCode");
        Integer quantity = (Integer) data.get("quantity");
        inventoryService.initInventory(skuCode, quantity);
    }

    @GetMapping("/admin/all")
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryResponse> listAll() {
        return inventoryService.listAll();
    }

    @PutMapping("/admin/{skuCode}")
    @ResponseStatus(HttpStatus.OK)
    public InventoryResponse updateQuantity(@PathVariable String skuCode,
                                            @RequestBody Map<String, Object> data) {
        Integer quantity = data.get("quantity") instanceof Number n ? n.intValue() : null;
        return inventoryService.updateQuantity(skuCode, quantity);
    }
}
