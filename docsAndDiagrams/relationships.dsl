//Context relationships

//Container Relationships
scheduler -> presto "fetches orders/revenue/catalog level cpc data from" {
    tags "Scheduled-Run"
}
listener -> adsKafka "Consumes from and Publishes to"
scheduler -> databaseComponent "Saves scheduler offsets to"



// Scheduler Component relationships
campaignPerformanceScheduler -> presto "fetches orders/revenue data from " {
    tags "scheduler-presto-fetch"
}
campaignPerformanceScheduler -> cms "fetches supplier-id for a particular campaign-catalog from"{
    tags "scheduler-cms-fetch"
}
campaignPerformanceScheduler -> database "Writes campaign-catalog-date-metrics-collection to"{
    tags "scheduler-db-write"
}
campaignPerformanceScheduler -> cacheComponent "Writes campaign-catalog-date keys to"{
    tags "scheduler-cache-write"
}


catalogCPCDiscountScheduler -> presto "Fetches catalog level cpc discount data from"
catalogCPCDiscountScheduler -> database "Writes catalog-cpc-discount-collection to"

dayWisePerformanceEventsScheduler -> cacheComponent "fetches campaign-catalog-date keys from"
dayWisePerformanceEventsScheduler -> adsKafka "Publishes campaign-catalog-date keys to"

//Listener component relationships
prestoSchedulerEventListener -> adsKafka "Consumes scheduler run messages from"
unPartitionedIngestionConfluentKafkaInteractionEventListener -> ingestionKafka "Consumes clicks/interaction events from"
unPartitionedIngestionConfluentKafkaInteractionEventListener -> adsKafka "Publishes interaction events/exception events to"
prestoSchedulerEventListener -> scheduler "Triggers the corresponsing scheduler"
adInteractionEventListener -> ingestionKafka "Consumes interaction events from"
adInteractionEventListener -> cms "Fecthes SupplierCampaignCatalogMetaDataResponse from"
adInteractionEventListener -> database "Fetches campaign’s, supplier’s and real-estate’s budgetUtilised data from"
adInteractionEventListener -> adsKafka "Publishes budget exhaust event to"
adInteractionEventListener -> cacheComponent "Add campaign catalog date keys to"
ingestionConfluentKafkaViewEventsListener -> ingestionKafka "Consumes view events from"
ingestionConfluentKafkaViewEventsListener -> cms "Fecthes AdViewEventMetadataResponse from"
ingestionConfluentKafkaViewEventsListener -> database "Batch up view counts at a campaign-catalog-date level in JVM and writes to"


//API component relationships
adsPayment -> campaignPerformanceController "Gets budget data from"
developer -> debugController "Gets staging data from"
developer -> manualSchedulerController "Triggers schedulers manually using"
developer -> backfillCampaignDataController "Triggers backfilling of missed campaign data using"
developer -> backfillMissedEventsController "Triggers backfilling of missed events using"
cms -> campaignPerformanceController "Gets campaign performance data from"
campaignPerformanceController -> database "Gets campaign performance data from"
debugController -> database "Gets data from"
backfillCampaignDataController -> database "Saves data in"
backfillMissedEventsController -> database "Saves data in"
