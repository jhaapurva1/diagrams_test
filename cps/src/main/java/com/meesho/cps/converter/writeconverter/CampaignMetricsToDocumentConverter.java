package com.meesho.cps.converter.writeconverter;

import com.meesho.cps.constants.MongoFields;
import com.meesho.cps.data.entity.mongodb.collection.CampaignMetrics;
import com.meesho.cps.utils.ConverterUtils;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;


@WritingConverter
public class CampaignMetricsToDocumentConverter implements Converter<CampaignMetrics, Document> {

    @Override
    public Document convert(CampaignMetrics campaignMetrics) {
        Document document = new Document();
        ConverterUtils.addDataToDocument(MongoFields.ID, campaignMetrics.getId(), document::put);
        ConverterUtils.addDataToDocument(MongoFields.CAMPAIGN_ID, campaignMetrics.getCampaignId(), document::put);
        ConverterUtils.addDataToDocument(MongoFields.BUDGET_UTILISED, campaignMetrics.getBudgetUtilised(),
                document::put);
        ConverterUtils.putRealEstateBudgetUtilised(document, campaignMetrics.getRealEstateBudgetUtilisedMap());
        return document;
    }
}