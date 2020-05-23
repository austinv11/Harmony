@file:JvmName("Launcher")

package harmony.util

import harmony.command.interfaces.HarmonyEntryPoint
import java.io.File
import java.lang.management.ManagementFactory


private const val MIN_DURATION = 5  // Require uptime to be 5 seconds, if not restarting will not occur to prevent restart loops


fun main(args: Array<String>) {
    // Partially based on https://stackoverflow.com/questions/4159802/how-can-i-restart-a-java-application
    val cmd = mutableListOf<String>()

    cmd.add("${System.getProperty("java.home")}${File.separator}bin${File.separator}java")

    for (jvmArg in ManagementFactory.getRuntimeMXBean().inputArguments) {
        cmd.add(jvmArg)
    }

    cmd.add("-cp")
    cmd.add(ManagementFactory.getRuntimeMXBean().classPath)

    cmd.add("harmony.util.LauncherDaemon")

    for (arg in args) {
        cmd.add(arg)
    }

    var lastExitCode: Int
    do {
        val startTime = System.currentTimeMillis()

        lastExitCode = ProcessBuilder(cmd)
                .inheritIO()
                .start()
                .waitFor()

        val exitStatus: HarmonyEntryPoint.ExitSignal
        if (lastExitCode !in HarmonyEntryPoint.ExitSignal.BASE_EXIT_CODE until (HarmonyEntryPoint.ExitSignal.BASE_EXIT_CODE + HarmonyEntryPoint.ExitSignal.values().size)) {
            System.err.println("WARNING: Harmony daemon ended with unexpected exit code $lastExitCode!")
            System.err.println("This will be treated as an abnormal close exit status!")
            exitStatus = HarmonyEntryPoint.ExitSignal.ABNORMAL_CLOSE
        } else {
            exitStatus = HarmonyEntryPoint.ExitSignal.values()[lastExitCode-8000]
            println("Harmony daemon closed with exit status: $exitStatus")
        }

        if (exitStatus != HarmonyEntryPoint.ExitSignal.COMPLETE_CLOSE
                && (System.currentTimeMillis() - startTime) < MIN_DURATION * 1000) {
            System.err.println("Harmony process died after less than $MIN_DURATION seconds, it will NOT be restarted!")
            break
        }

    } while (lastExitCode != HarmonyEntryPoint.ExitSignal.COMPLETE_CLOSE.ordinal)
}
