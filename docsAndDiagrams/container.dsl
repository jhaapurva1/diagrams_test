database = container "Document Database" "Database"{
    technology "MongoDB"
    tags "Database"
}
apiapp = container "API-application"{
    technology "Java SpringBoot"
    !include apiComponent.dsl
}
scheduler = container "Schedulers" {
    !include SchedulerComponent.dsl
}
listener = container "Listener" {
    !include ListenerComponent.dsl
}




