package com.meesho.cps.transformer;

import com.meesho.cps.data.entity.elasticsearch.ESDailyIndexDocument;
import com.meesho.cps.data.entity.elasticsearch.ESMonthlyIndexDocument;
import com.meesho.cps.data.entity.hbase.CampaignCatalogDateMetrics;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author shubham.aggarwal
 * 16/12/21
 */
public class ESDocumentTransformer {

    private static final String DELIMITER = "_";

    public static ESMonthlyIndexDocument getMonthIndexDocument(
            List<CampaignCatalogDateMetrics> campaignCatalogDateMetrics, String monthPrefix, Long campaignId,
            Long catalogId, Long supplierId) {
        ESMonthlyIndexDocument monthlyIndexDocument = ESMonthlyIndexDocument.builder().build();
        monthlyIndexDocument.setMonth(monthPrefix);
        monthlyIndexDocument.setId(campaignId.toString() + DELIMITER + catalogId.toString() + DELIMITER + monthPrefix);
        monthlyIndexDocument.setBudgetUtilised(new BigDecimal(0));
        monthlyIndexDocument.setCampaignId(campaignId);
        monthlyIndexDocument.setCatalogId(catalogId);
        monthlyIndexDocument.setClicks(0L);
        monthlyIndexDocument.setOrders(0);
        monthlyIndexDocument.setRevenue(new BigDecimal(0L));
        monthlyIndexDocument.setShares(0L);
        monthlyIndexDocument.setWishlist(0L);
        monthlyIndexDocument.setViews(0L);
        monthlyIndexDocument.setSupplierId(supplierId);

        campaignCatalogDateMetrics.forEach(entry -> {
            monthlyIndexDocument.setViews(monthlyIndexDocument.getViews() +
                    Optional.ofNullable(entry.getViewCount()).orElse(0L));
            monthlyIndexDocument.setBudgetUtilised(monthlyIndexDocument.getBudgetUtilised().add(
                    Objects.isNull(entry.getBudgetUtilised()) ? new BigDecimal(0) : entry.getBudgetUtilised()));
            monthlyIndexDocument.setWishlist(monthlyIndexDocument.getWishlist() +
                    Optional.ofNullable(entry.getWishlistCount()).orElse(0L));
            monthlyIndexDocument.setShares(monthlyIndexDocument.getShares() +
                    Optional.ofNullable(entry.getSharesCount()).orElse(0L));
            monthlyIndexDocument.setOrders(monthlyIndexDocument.getOrders() +
                    Optional.ofNullable(entry.getOrders()).orElse(0));
            monthlyIndexDocument.setClicks(monthlyIndexDocument.getClicks() +
                    Optional.ofNullable(entry.getClickCount()).orElse(0L));
            monthlyIndexDocument.setRevenue(monthlyIndexDocument.getRevenue().add(
                    Objects.isNull(entry.getRevenue()) ? new BigDecimal(0) : entry.getRevenue()));
        });
        return monthlyIndexDocument;
    }

    public static List<ESDailyIndexDocument> getDailyIndexDocument(
            List<CampaignCatalogDateMetrics> campaignCatalogDateMetrics, Long campaignId, Long catalogId,
            Set<String> dateSet, Long supplierId) {
        List<ESDailyIndexDocument> dailyIndexDocuments = new ArrayList<>();
        campaignCatalogDateMetrics.forEach(entry -> {
            if(dateSet.contains(entry.getDate().toString())){
                ESDailyIndexDocument dailyIndexDocument = ESDailyIndexDocument.builder().build();
                dailyIndexDocument.setId(campaignId + DELIMITER + catalogId + DELIMITER + entry.getDate());
                dailyIndexDocument.setCampaignId(campaignId);
                dailyIndexDocument.setCatalogId(catalogId);
                dailyIndexDocument.setClicks(entry.getClickCount());
                dailyIndexDocument.setOrders(entry.getOrders());
                dailyIndexDocument.setViews(entry.getViewCount());
                dailyIndexDocument.setBudgetUtilised(entry.getBudgetUtilised());
                dailyIndexDocument.setWishlist(entry.getWishlistCount());
                dailyIndexDocument.setShares(entry.getSharesCount());
                dailyIndexDocument.setRevenue(entry.getRevenue());
                dailyIndexDocument.setDate(entry.getDate().toString());
                dailyIndexDocument.setSupplierId(supplierId);
                dailyIndexDocuments.add(dailyIndexDocument);
            }
        });
        return dailyIndexDocuments;
    }

}
