package com.meesho.cps.transformer;

import com.meesho.cps.enums.FeedType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class OriginScreenREMapper {

    private static final Map<String, String> originScreenREMap = new HashMap<>();

    static {
        originScreenREMap.put("social_profile_shared-category_tree", FeedType.CLP.getValue());
        originScreenREMap.put("cart_wishlist-main", FeedType.FY.getValue());
        originScreenREMap.put("cart_wishlist-pdp_recommendations", FeedType.PRODUCT_RECO.getValue());
        originScreenREMap.put("cart_wishlist-similar_products", FeedType.PRODUCT_RECO.getValue());
        originScreenREMap.put("social_profile_shared-category_bar", FeedType.CLP.getValue());
        originScreenREMap.put("main-banner", FeedType.COLLECTION.getValue());
        originScreenREMap.put("main-main", FeedType.FY.getValue());
        originScreenREMap.put("single_collection-category_bar", FeedType.CLP.getValue());
        originScreenREMap.put("social_profile_wishlist-pdp_recommendations", FeedType.PRODUCT_RECO.getValue());
        originScreenREMap.put("single_collection-banner", FeedType.COLLECTION.getValue());
        originScreenREMap.put("single_collection-widget", FeedType.COLLECTION.getValue());
        originScreenREMap.put("wishlist-plp_recommendations", FeedType.PRODUCT_RECO.getValue());
        originScreenREMap.put("cart_wishlist-catalog_listing_page", FeedType.CLP.getValue());
        originScreenREMap.put("single_product-pdp_recommendations", FeedType.PRODUCT_RECO.getValue());
        originScreenREMap.put("cart_wishlist-banner", FeedType.COLLECTION.getValue());
        originScreenREMap.put("main-category_bar", FeedType.CLP.getValue());
        originScreenREMap.put("my_shared_catalogs-pdp_recommendations", FeedType.PRODUCT_RECO.getValue());
        originScreenREMap.put("catalog-similar_catalogs", FeedType.PRODUCT_RECO.getValue());
        originScreenREMap.put("social_profile_shared-widget", FeedType.COLLECTION.getValue());
        originScreenREMap.put("wishlist-widget", FeedType.COLLECTION.getValue());
        originScreenREMap.put("cart_wishlist-widget", FeedType.COLLECTION.getValue());
        originScreenREMap.put("social_profile_shared-pdp_recommendations", FeedType.PRODUCT_RECO.getValue());
        originScreenREMap.put("single_product-plp_recommendations", FeedType.PRODUCT_RECO.getValue());
        originScreenREMap.put("catalog_listing_page-widget", FeedType.COLLECTION.getValue());
        originScreenREMap.put("social_profile_wishlist-category_bar", FeedType.CLP.getValue());
        originScreenREMap.put("wishlist-category_tree", FeedType.CLP.getValue());
        originScreenREMap.put("wishlist-similar_products", FeedType.PRODUCT_RECO.getValue());
        originScreenREMap.put("catalog_listing_page-category_tree", FeedType.CLP.getValue());
        originScreenREMap.put("social_profile_wishlist-widget", FeedType.COLLECTION.getValue());
        originScreenREMap.put("wishlist-pdp_recommendations", FeedType.PRODUCT_RECO.getValue());
        originScreenREMap.put("single_collection-single_collection", FeedType.COLLECTION.getValue());
        originScreenREMap.put("my_shared_catalogs-category_tree", FeedType.CLP.getValue());
        originScreenREMap.put("catalog_search_results-catalog_search_results", FeedType.TEXT_SEARCH.getValue());
        originScreenREMap.put("cart_wishlist-category_bar", FeedType.CLP.getValue());
        originScreenREMap.put("social_profile_shared-similar_products", FeedType.PRODUCT_RECO.getValue());
        originScreenREMap.put("cart_wishlist-catalog_search_results", FeedType.TEXT_SEARCH.getValue());
        originScreenREMap.put("catalog_listing_page-category_bar", FeedType.CLP.getValue());
        originScreenREMap.put("cart_wishlist-single_collection", FeedType.COLLECTION.getValue());
        originScreenREMap.put("my_shared_catalogs-plp_recommendations", FeedType.PRODUCT_RECO.getValue());
        originScreenREMap.put("my_shared_catalogs-widget", FeedType.COLLECTION.getValue());
        originScreenREMap.put("social_profile_shared-plp_recommendations", FeedType.PRODUCT_RECO.getValue());
        originScreenREMap.put("social_profile_wishlist-category_tree", FeedType.CLP.getValue());
        originScreenREMap.put("my_shared_catalogs-category_bar", FeedType.CLP.getValue());
        originScreenREMap.put("single_collection-category_tree", FeedType.CLP.getValue());
        originScreenREMap.put("catalog-plp_recommendations", FeedType.PRODUCT_RECO.getValue());
        originScreenREMap.put("single_product-catalog_search_results", FeedType.TEXT_SEARCH.getValue());
        originScreenREMap.put("social_profile_wishlist-plp_recommendations", FeedType.PRODUCT_RECO.getValue());
        originScreenREMap.put("cart_wishlist-category_tree", FeedType.CLP.getValue());
        originScreenREMap.put("wishlist-category_bar", FeedType.CLP.getValue());
        originScreenREMap.put("catalog_listing_page-banner", FeedType.COLLECTION.getValue());
        originScreenREMap.put("my_shared_catalogs-similar_products", FeedType.PRODUCT_RECO.getValue());
        originScreenREMap.put("main-widget", FeedType.COLLECTION.getValue());
        originScreenREMap.put("catalog_search_results-widget", FeedType.COLLECTION.getValue());
        originScreenREMap.put("social_profile_wishlist-similar_products", FeedType.PRODUCT_RECO.getValue());
        originScreenREMap.put("pdp_recommendations-similar_catalogs", FeedType.PRODUCT_RECO.getValue());
    }

    public static String getFeedType(String origin, String screen){

        if(Objects.isNull(origin) || Objects.isNull(screen))
            return null;

        origin = StringUtils.lowerCase(origin);
        screen = StringUtils.lowerCase(screen);
        return originScreenREMap.get(screen + "-" + origin);
    }

}
