package com.minejava.inventoryservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_inventory_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedInventoryEvent {

    @Id
    private String orderId;
    private String status;
    private LocalDateTime processedAt;
}
