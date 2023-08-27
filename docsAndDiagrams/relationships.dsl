//Context relationships

//Container Relationships
scheduler -> presto "fetches orders/revenue/catalog level cpc data from" {
    tags "Scheduled-Run"
}


// Scheduler Component relationships
campaignPerformanceScheduler -> presto "fetches orders/revenue data from "
campaignPerformanceScheduler -> cms "fetches supplier-id for a particular campaign-catalog from"
campaignPerformanceScheduler -> database "Writes campaign-catalog-date-metrics-collection to"
campaignPerformanceScheduler -> cacheComponent "Writes campaign-catalog-date keys to"


catalogCPCDiscountScheduler -> presto "Fetches catalog level cpc discount data from"
catalogCPCDiscountScheduler -> database "Writes catalog-cpc-discount-collection to"

dayWisePerformanceEventsScheduler -> cacheComponent "fetches campaign-catalog-date keys from"
dayWisePerformanceEventsScheduler -> adsKafka "Publishes campaign-catalog-date keys to"

//Listener component relationships
prestoSchedulerEventListener -> adsKafka "Consumes scheduler run messages from

//API component relationships
adsPayment -> campaignPerformanceController "Gets budget data from"
developer -> debugController "Gets staging data from"
developer -> manualSchedulerController "Triggers schedulers manually using"
developer -> backfillCampaignDataController "Triggers backfilling of missed campaign data using"
developer -> backfillMissedEventsController "Triggers backfilling of missed events using"
cms -> campaignPerformanceController "Gets campaign performance data from"