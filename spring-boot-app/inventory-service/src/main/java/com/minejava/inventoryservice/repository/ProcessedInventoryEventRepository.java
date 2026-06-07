package com.minejava.inventoryservice.repository;

import com.minejava.inventoryservice.model.ProcessedInventoryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedInventoryEventRepository extends JpaRepository<ProcessedInventoryEvent, String> {
}
