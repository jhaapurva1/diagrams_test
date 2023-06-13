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
@Document(collection = DBConstants.MongoCollections.CAMPAIGN_METRICS_COLLECTION)
public class CampaignMetrics {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    @NotNull(message = "CampaignId field is required")
    @Field(CAMPAIGN_ID)
    private Long campaignId;

    @Field(value = BUDGET_UTILISED, targetType = DECIMAL128)
    private BigDecimal budgetUtilised;

}
