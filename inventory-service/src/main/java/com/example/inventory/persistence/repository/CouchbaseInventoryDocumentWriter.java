package com.example.inventory.persistence.repository;

import com.couchbase.client.core.error.CasMismatchException;
import com.couchbase.client.core.error.DocumentExistsException;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.InsertOptions;
import com.couchbase.client.java.kv.ReplaceOptions;
import com.example.inventory.persistence.entity.InventoryDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.stereotype.Component;

@Component
public class CouchbaseInventoryDocumentWriter implements InventoryDocumentWriter {
  private static final int MAX_CAS_ATTEMPTS = 8;

  private final Collection collection;
  private final ObjectMapper objectMapper;

  public CouchbaseInventoryDocumentWriter(
      CouchbaseTemplate template,
      ObjectMapper objectMapper,
      @Value("${spring.data.couchbase.collection-name:InventoryList}") String collectionName) {
    this.collection = template.getCollection(collectionName);
    this.objectMapper = objectMapper;
    this.objectMapper.findAndRegisterModules();
  }

  @Override
  public void applyIfNewer(InventoryDocument document) {
    JsonObject content = toJson(document);
    for (int attempt = 0; attempt < MAX_CAS_ATTEMPTS; attempt++) {
      try {
        GetResult current = collection.get(document.getProductId());
        Long storedVersion = current.contentAsObject().getLong("version");
        long currentVersion = storedVersion == null ? 0 : storedVersion;
        if (currentVersion >= document.getVersion()) {
          return;
        }
        collection.replace(
            document.getProductId(), content, ReplaceOptions.replaceOptions().cas(current.cas()));
        return;
      } catch (DocumentNotFoundException exception) {
        try {
          collection.insert(document.getProductId(), content, InsertOptions.insertOptions());
          return;
        } catch (DocumentExistsException retry) {
          // A concurrent writer won the insert; retry with its CAS.
        }
      } catch (CasMismatchException retry) {
        // Read the current CAS and version again.
      }
    }
    throw new IllegalStateException("Unable to apply inventory document after CAS retries");
  }

  private JsonObject toJson(InventoryDocument document) {
    try {
      JsonObject content = JsonObject.fromJson(objectMapper.writeValueAsString(document));
      content.put("_class", InventoryDocument.class.getName());
      return content;
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to encode inventory document", exception);
    }
  }
}
