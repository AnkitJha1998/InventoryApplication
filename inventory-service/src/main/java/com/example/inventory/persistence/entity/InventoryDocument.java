package com.example.inventory.persistence.entity;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.core.mapping.Field;
import org.springframework.data.couchbase.repository.Scope;

@Document
@Scope("Inventory")
@Collection("InventoryList")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryDocument {
    @Id
    private String productId;

    @Field
    private String productName;

    @Field
    private String productType;

    @Field
    private int availableCount;

    @Field
    private int availableQuantity;

    @Field
    private boolean available;

    @Field
    private long version;

    @Field
    private Instant updatedAt;
}
