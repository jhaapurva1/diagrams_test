
systemContext cps "CPS-SystemContext" {
    include *
    autoLayout
}
theme default

container cps "CPS-Containers" {
    include *
}
component apiapp "API-Application" {
    include *
    autoLayout
}
component scheduler "Schedulers" {
    include *
}
component listener "Listener" {
    include *
}
styles {
    element "Database"{
        shape cylinder
    }
    element "System-Under-Consideration"{
        background #3b444b
    }
    relationship "Relationship"{
        dashed false
    }
    relationship "Scheduled-Run"{
        dashed true
    }
}