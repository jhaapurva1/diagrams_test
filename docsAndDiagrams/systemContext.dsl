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
                                tags "System-Under-Consideration"
                                description "Tracks campaign related metrics"
                                group "Shared across all ads"{
                                    !include sharedContainers.dsl
                                }
                                !include container.dsl
                                !include relationships.dsl
                    }
                }




                views {
                    !include views.dsl
                }

}