@file:JvmName("LauncherDaemon")

package harmony.util

import harmony.command.interfaces.HarmonyEntryPoint
import java.util.*
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val entryPoint = ServiceLoader.load(HarmonyEntryPoint::class.java).findFirst().asNullable()
            ?: throw Exception("No entry point defined! Have you wired it using @WireService(HarmonyEntryPoint.class)?")

    val token = entryPoint.getToken(args) ?: throw Exception("No token provided!")
    val harmony = entryPoint.buildHarmony(token) ?: throw Exception("The harmony client was incorrectly built!")
    val status = entryPoint.startBot(harmony) ?: throw Exception("No exit status provided!")

    exitProcess(status.toExitCode())
}
