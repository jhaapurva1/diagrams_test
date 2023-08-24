
systemContext cps "CPS-SystemContext" {
    include *
}
theme default

container cps "CPS-Containers" {
    include *
}
component apiapp "API-Application" {
    include *
}
component scheduler "Schedulers" {
    include *
}
styles {
    element "Database"{
        shape cylinder
    }
}