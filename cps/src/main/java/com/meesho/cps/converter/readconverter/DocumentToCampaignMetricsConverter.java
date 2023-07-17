package com.meesho.cps.converter.readconverter;

import com.meesho.ad.client.constants.FeedType;
import com.meesho.cps.constants.MongoFields;
import com.meesho.cps.data.entity.mongodb.collection.CampaignMetrics;
import com.meesho.cps.utils.ConverterUtils;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.util.ArrayList;

@ReadingConverter
public class DocumentToCampaignMetricsConverter implements Converter<Document, CampaignMetrics> {

    @Override
    public CampaignMetrics convert(Document source) {
        return CampaignMetrics.builder()
                .id(source.getObjectId(MongoFields.ID))
                .campaignId(source.getLong(MongoFields.CAMPAIGN_ID))
                .budgetUtilised(ConverterUtils.getBigDecimalFromDecimal128(source.get(MongoFields.BUDGET_UTILISED,
                        Decimal128.class)))
                .realEstateBudgetUtilisedMap(ConverterUtils.getRealEstateBudgetUtilisedMap(source,
                        new ArrayList<>(FeedType.ACTIVE_REAL_ESTATE_TYPES)))
                .build();
    }
}