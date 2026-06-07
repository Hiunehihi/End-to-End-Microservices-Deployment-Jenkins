package com.minejava.inventoryservice.repository;

import com.minejava.inventoryservice.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    List<Inventory> findBySkuCodeIn(List<String> skuCode);
    Optional<Inventory> findBySkuCodeIgnoreCase(String skuCode);

    @Modifying
    @Query("""
            update Inventory inventory
            set inventory.quantity = inventory.quantity - :quantity
            where lower(inventory.skuCode) = lower(:skuCode)
              and inventory.quantity >= :quantity
            """)
    int decrementStockIfAvailable(@Param("skuCode") String skuCode, @Param("quantity") Integer quantity);
}
