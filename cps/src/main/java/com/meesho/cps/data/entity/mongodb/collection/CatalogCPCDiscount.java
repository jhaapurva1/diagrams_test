package com.meesho.cps.data.entity.mongodb.collection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.meesho.cps.constants.DBConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

import static com.meesho.cps.constants.MongoFields.*;
import static org.springframework.data.mongodb.core.mapping.FieldType.DECIMAL128;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = DBConstants.MongoCollections.CATALOG_CPC_DISCOUNT_COLLECTION)
public class CatalogCPCDiscount {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    @NotNull(message = "CatalogId field is required")
    @Field(CATALOG_ID)
    private Long catalogId;

    @Field(value = DISCOUNT, targetType = DECIMAL128)
    private BigDecimal discount;

}
