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
@Document(collection = DBConstants.MongoCollections.CAMPAIGN_DATE_WISE_METRICS_COLLECTION)
@CompoundIndex(name = "campaign_date", unique = true, def = "{'" + CAMPAIGN_ID + "': 1, '" + DATE + "':1}")
public class CampaignDateWiseMetrics {

    @Id
    private ObjectId id;

    @NotNull(message = "CampaignId field is required")
    @Field(CAMPAIGN_ID)
    private Long campaignId;

    @NotNull(message = "Date field is required")
    @Field(DATE)
    private String date;

    @Field(value = BUDGET_UTILISED, targetType = DECIMAL128)
    private BigDecimal budgetUtilised;

}
