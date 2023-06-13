package com.meesho.cps.helper;

import com.meesho.cps.constants.Constants;
import com.meesho.cps.data.internal.CampaignCatalogDate;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class BackFillCampaignHelper {

    public static List<CampaignCatalogDate> getCampaignCatalogAndDateFromCSV(Reader reader) throws IOException {
        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader(Constants.CAMPAIGN_CATALOG_DATE_FORMAT)
                .withSkipHeaderRecord(true)
                .withTrim()
                .withIgnoreHeaderCase(true));
        List<CampaignCatalogDate> campaignCatalogDateList = new ArrayList<>();
        for (CSVRecord csvRecord : csvParser) {
            CampaignCatalogDate campaignCatalogDate  = CampaignCatalogDate.builder()
                    .campaignId(Long.parseLong(csvRecord.get("campaign_id")))
                    .catalogId(Long.parseLong(csvRecord.get("catalog_id")))
                    .date(csvRecord.get("date"))
                    .build();
            campaignCatalogDateList.add(campaignCatalogDate);
        }
        return campaignCatalogDateList;
    }

}
