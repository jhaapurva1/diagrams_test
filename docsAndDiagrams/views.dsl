
systemContext cps "CPS-SystemContext" {
    include *
}
theme default

container cps "CPS-Containers" {
    include *
}
component apiapp {
    include *
}
component scheduler {
    include *
}
styles {
    element "Database"{
        shape cylinder
    }
}