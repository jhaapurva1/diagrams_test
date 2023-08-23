database = container "Document Database" "Database"{
    technology "MongoDB"
    tags "Database"
}
apiapp = container "API application"{
    technology "Java SpringBoot"
    !include apiComponent.dsl
}
scheduler = container "Schedulers" {
    !include SchedulerComponent.dsl
}
listener = container "Listener"


apiapp -> database "Reads from"
scheduler -> database "Writes order, revenue, cpc-discount data to"
scheduler -> database "Reads daywise performance data from"
listener -> database "Writes ad clicks, views, other interaction data to"
listener -> ingestionKafka "Gets ad interaction events data from"
cms -> apiapp "Gets campaign performance like budget utilisation, ROI, etc. data from"

scheduler -> presto "Pushes daywise performance data vis Prism SDK to"
presto -> scheduler "Gets order, revenues, cpc-discount data from"


