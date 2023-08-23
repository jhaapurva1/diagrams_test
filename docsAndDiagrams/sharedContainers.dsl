cacheComponent = container "Cache" {
    technology "Redis"
    tags "Database"
    description "In context of CPS, it stores User Interaction data and Campaign Catalog date keys"
}
databaseComponent = container "Relational Database" {
    technology "MySQL"
    tags "Database"
    description "In context of CPS it is just used to store scheduler offsets"

}
adsKafka = container "Ads Kafka" {
    technology "Kafka"
}