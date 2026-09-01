package com.devicehub

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DeviceHubApplication

fun main(args: Array<String>) {
    runApplication<DeviceHubApplication>(*args)
}
