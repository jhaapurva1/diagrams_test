package com.meesho.cps.db.redis.dao;

import com.meesho.cps.data.internal.CampaignCatalogDate;

import java.util.List;

/**
 * @author shubham.aggarwal
 * 18/11/21
 */
public interface UpdatedCampaignCatalogCacheDao {

    void add(List<CampaignCatalogDate> campaignCatalogDates);

    void delete(List<CampaignCatalogDate> campaignCatalogDates);

    List<CampaignCatalogDate> getAllUpdatedCampaignCatalogs();

}
