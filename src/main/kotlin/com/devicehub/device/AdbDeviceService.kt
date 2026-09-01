package com.devicehub.device

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.TimeUnit

@Service
class AdbDeviceService(
    @Value("\${devicehub.adb.path:}") private val configuredAdbPath: String,
) {
    private val commandTimeoutSeconds = 5L

    fun findConnectedDevices(): ConnectedDeviceResponse {
        val adb = resolveAdbExecutable()
            ?: return ConnectedDeviceResponse(
                status = ConnectedDeviceStatus.ADB_NOT_AVAILABLE,
                message = "ADB 실행 파일을 찾을 수 없습니다. PATH 또는 ADB_PATH를 확인해 주세요.",
            )

        val devicesResult = runCommand(listOf(adb, "devices"))
        if (!devicesResult.success) {
            return ConnectedDeviceResponse(status = ConnectedDeviceStatus.ERROR, message = devicesResult.error)
        }

        val entries = devicesResult.output.lineSequence()
            .drop(1)
            .map(String::trim)
            .filter(String::isNotBlank)
            .mapNotNull { line ->
                val parts = line.split(Regex("\\s+"), limit = 3)
                if (parts.size >= 2) AdbEntry(serialNumber = parts[0], status = parts[1]) else null
            }
            .toList()

        val connectedEntries = entries.filter { it.status == "device" }
        if (connectedEntries.isEmpty()) {
            return when {
                entries.any { it.status == "unauthorized" } -> ConnectedDeviceResponse(ConnectedDeviceStatus.UNAUTHORIZED)
                entries.any { it.status == "offline" } -> ConnectedDeviceResponse(ConnectedDeviceStatus.OFFLINE)
                else -> ConnectedDeviceResponse(ConnectedDeviceStatus.NOT_FOUND)
            }
        }

        val devices = connectedEntries.mapNotNull { readDevice(adb, it.serialNumber) }
        if (devices.isEmpty()) {
            return ConnectedDeviceResponse(ConnectedDeviceStatus.ERROR, message = "연결 기기 정보를 읽지 못했습니다.")
        }
        return ConnectedDeviceResponse(status = ConnectedDeviceStatus.CONNECTED, devices = devices)
    }

    private fun readDevice(adb: String, serialNumber: String): ConnectedAndroidDevice? {
        // serialNumber는 외부 요청값이 아니라 위 adb devices 출력에서 얻은 값만 사용한다.
        fun property(name: String): String? =
            runCommand(listOf(adb, "-s", serialNumber, "shell", "getprop", name))
                .takeIf { it.success }
                ?.output
                ?.trim()
                ?.takeIf(String::isNotBlank)

        val manufacturer = property("ro.product.manufacturer") ?: return null
        val modelName = property("ro.product.model") ?: return null
        val osVersion = property("ro.build.version.release") ?: return null
        val sdkVersion = property("ro.build.version.sdk") ?: return null
        return ConnectedAndroidDevice(
            serialNumber = serialNumber,
            manufacturer = manufacturer.replaceFirstChar { it.uppercase() },
            modelName = modelName,
            productName = property("ro.product.name"),
            deviceName = property("ro.product.device"),
            osVersion = osVersion,
            sdkVersion = sdkVersion,
            type = detectDeviceType(adb, serialNumber),
        )
    }

    private fun detectDeviceType(adb: String, serialNumber: String): DeviceType? {
        val size = runCommand(listOf(adb, "-s", serialNumber, "shell", "wm", "size"))
        val density = runCommand(listOf(adb, "-s", serialNumber, "shell", "wm", "density"))
        if (!size.success || !density.success) return null

        val dimensions = Regex("(\\d+)x(\\d+)").find(size.output)?.groupValues ?: return null
        val densityDpi = Regex("(\\d+)").findAll(density.output).lastOrNull()?.value?.toIntOrNull() ?: return null
        val shortestWidthDp = minOf(dimensions[1].toInt(), dimensions[2].toInt()) * 160 / densityDpi
        // Android의 일반적인 large-screen 기준인 smallest width 600dp 이상만 태블릿으로 판단한다.
        return if (shortestWidthDp >= 600) DeviceType.TABLET else DeviceType.PHONE
    }

    private fun resolveAdbExecutable(): String? {
        val executableName =
            if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) "adb.exe" else "adb"
        val candidates = buildList {
            configuredAdbPath.trim().takeIf(String::isNotBlank)?.let(::add)
            System.getenv("ANDROID_HOME")?.let { add(File(it, "platform-tools/$executableName").path) }
            System.getenv("ANDROID_SDK_ROOT")?.let { add(File(it, "platform-tools/$executableName").path) }
            System.getenv("LOCALAPPDATA")?.let { add(File(it, "Android/Sdk/platform-tools/$executableName").path) }
        }
        return candidates.firstOrNull { File(it).isFile } ?: executableName.takeIf { canRun(it) }
    }

    private fun canRun(command: String): Boolean = runCommand(listOf(command, "version")).success

    private fun runCommand(arguments: List<String>): CommandResult = try {
        val process = ProcessBuilder(arguments).start()
        if (!process.waitFor(commandTimeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            CommandResult(false, error = "ADB 명령 실행 시간이 초과되었습니다.")
        } else {
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            CommandResult(process.exitValue() == 0, output.trim(), error.trim())
        }
    } catch (exception: Exception) {
        CommandResult(false, error = exception.message ?: "ADB 명령을 실행하지 못했습니다.")
    }

    private data class AdbEntry(val serialNumber: String, val status: String)
    private data class CommandResult(val success: Boolean, val output: String = "", val error: String = "")
}
