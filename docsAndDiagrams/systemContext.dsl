workspace  "Ads - Campaign Performance Service(CPS)" "Tracks campaign related metrics"{
        !docs docs
        model {
                cms = softwareSystem "Campaign Management Service"
                ingestionKafka = softwareSystem "Ingestion Kafka"
                adsPayment = softwareSystem "Ads Payment Service"
                developer = person "Developer" {
                        description "Uses the system for manual intervention for things like triggering a scheduler"
                }
                presto  = softwareSystem "Presto"
                cps = softwareSystem "Campaign Performance Service(CPS)"{
                                group "Shared across all ads"{
                                    !include sharedContainers.dsl
                                }
                                !include container.dsl

                        developer -> apiapp "Uses"
                        scheduler -> databaseComponent "Stores scheduler offsets in"
                        listener -> cacheComponent "Updates campaign catalog date key to"
                        cms -> campaignPerformanceController "Gets campaign level data like budget utilised using"
                    }
                }


                views {
                    !include views.dsl
                }

}