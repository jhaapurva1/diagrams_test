
systemContext cps "CPS-SystemContext" {
    include *
    autoLayout
}
theme default

container cps "CPS-Containers" {
    include *
    autoLayout
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
    autoLayout
}
styles {
    element "Database"{
        shape cylinder
    }
    element "System-Under-Consideration"{
        background seagreen
    }
    relationship "Scheduled-Run"{
        color blue
    }
}