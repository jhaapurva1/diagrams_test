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
@Document(collection = DBConstants.MongoCollections.SUPPLIER_WEEK_WISE_METRICS_COLLECTION)
@CompoundIndex(name = "supplier_week_start_date", unique = true, def = "{'" + SUPPLIER_ID + "': 1, '" + WEEK_START_DATE + "':1}")
public class SupplierWeekWiseMetrics {

    @Id
    private ObjectId id;

    @NotNull(message = "SupplierId field is required")
    @Field(SUPPLIER_ID)
    private Long supplierId;

    @NotNull(message = "WeekStartDate field is required")
    @Field(WEEK_START_DATE)
    private String weekStartDate;

    @Field(value = BUDGET_UTILISED, targetType = DECIMAL128)
    private BigDecimal budgetUtilised;

}
