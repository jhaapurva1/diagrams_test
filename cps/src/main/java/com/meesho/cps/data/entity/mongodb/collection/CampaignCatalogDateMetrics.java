package com.meesho.cps.data.entity.mongodb.collection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.meesho.cps.constants.DBConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
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
@Document(collection = DBConstants.MongoCollections.CAMPAIGN_CATALOG_DATE_METRICS_COLLECTION)
@CompoundIndex(name = "campaign_catalog_date", unique = true, def = "{'" + CAMPAIGN_ID + "': 1, '" + CATALOG_ID + "': 1, '" + DATE + "': 1}")
@CompoundIndex(name = "supplier_date", def = "{'" + SUPPLIER_ID + "': 1, '" + DATE + "':1}")
public class CampaignCatalogDateMetrics {

    @Id
    private ObjectId id;

    @NotNull(message = "SupplierId field is required")
    @Field(SUPPLIER_ID)
    private Long supplierId;

    @NotNull(message = "CampaignId field is required")
    @Field(CAMPAIGN_ID)
    private Long campaignId;

    @NotNull(message = "CatalogId field is required")
    @Field(CATALOG_ID)
    private Long catalogId;

    @NotNull(message = "Date field is required")
    @Indexed
    @Field(DATE)
    private String date;

    @Field(value = VIEWS)
    private Long views;

    @Field(CLICKS)
    private Long clicks;

    @Field(value = BUDGET_UTILISED, targetType = DECIMAL128)
    private BigDecimal budgetUtilised;

    @Field(WISHLISTS)
    private Long wishlists;

    @Field(SHARES)
    private Long shares;

    @Field(ORDERS)
    private Integer orders;

    @Field(value = REVENUE, targetType = DECIMAL128)
    private BigDecimal revenue;

}
