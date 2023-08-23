campaignPerformanceScheduler = component "Campaign Performance Scheduler"{
}
catalogCPCDiscountScheduler = component "Catalog CPC Discount Scheduler"{

}

dayWisePerformanceEventsScheduler = component "Daywise Performance Events Scheduler"{

}

campaignPerformanceScheduler -> presto "fetches orders/revenue data from "
campaignPerformanceScheduler -> cms "fetches supplier-id for a particular campaign-catalog from"
campaignPerformanceScheduler -> database "Writes campaign-catalog-date-metrics-collection to"
campaignPerformanceScheduler -> cacheComponent "Writes campaign-catalog-date keys to"


catalogCPCDiscountScheduler -> presto "Fetches catalog level cpc discount data from"
catalogCPCDiscountScheduler -> database "Writes catalog-cpc-discount-collection to"

dayWisePerformanceEventsScheduler -> cacheComponent "fetches campaign-catalog-date keys from"
dayWisePerformanceEventsScheduler -> adsKafka "Publishes campaign-catalog-date keys to"




