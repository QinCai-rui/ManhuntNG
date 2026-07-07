tasks.register("printVersion") {
    doLast {
        println("VERSION=${version}")
    }
}
