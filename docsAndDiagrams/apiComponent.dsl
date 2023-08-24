backfillCampaignDataController = component "Backfill Campaign Data Controller"
backfillMissedEventsController = component "Backfill Missed Events Controller"
campaignPerformanceController = component "Campaign Performance Controller"
debugController = component "Debug Controller"
manualSchedulerController = component "Manual Scheduler Controller"

adsPayment -> campaignPerformanceController "Gets budget data from"
developer -> debugController "Gets staging data from"
developer -> manualSchedulerController "Triggers schedulers manually using"
developer -> backfillCampaignDataController "Triggers backfilling of missed campaign data using"
developer -> backfillMissedEventsController "Triggers backfilling of missed events using"
cms -> campaignPerformanceController "Gets campaign performance data from"

