package com.meesho.cps.converter.writeconverter;

import com.meesho.cps.constants.MongoFields;
import com.meesho.cps.data.entity.mongodb.collection.CampaignDateWiseMetrics;
import com.meesho.cps.utils.ConverterUtils;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;


@WritingConverter
public class CampaignDateWiseMetricsToDocumentConverter implements Converter<CampaignDateWiseMetrics, Document> {

    @Override
    public Document convert(CampaignDateWiseMetrics campaignDateWiseMetrics) {
        Document document = new Document();
        ConverterUtils.addDataToDocument(MongoFields.ID, campaignDateWiseMetrics.getId(), document::put);
        ConverterUtils.addDataToDocument(MongoFields.CAMPAIGN_ID, campaignDateWiseMetrics.getCampaignId(), document::put);
        ConverterUtils.addDataToDocument(MongoFields.DATE, campaignDateWiseMetrics.getDate(), document::put);
        ConverterUtils.addDataToDocument(MongoFields.BUDGET_UTILISED, campaignDateWiseMetrics.getBudgetUtilised(),
                document::put);
        ConverterUtils.putRealEstateBudgetUtilised(document, campaignDateWiseMetrics.getRealEstateBudgetUtilisedMap());
        return document;
    }
}