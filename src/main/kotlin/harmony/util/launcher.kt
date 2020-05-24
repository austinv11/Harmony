@file:JvmName("Launcher")

package harmony.util

import harmony.command.interfaces.HarmonyEntryPoint
import java.io.File
import java.lang.management.ManagementFactory


private const val MIN_DURATION = 5  // Require uptime to be 5 seconds, if not restarting will not occur to prevent restart loops


/**
 * This launches a process which builds a child process that runs Harmony. This allows automatic restarts.
 */
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

    var exitSignal: HarmonyEntryPoint.ExitSignal
    do {
        val startTime = System.currentTimeMillis()

        val lastExitCode = ProcessBuilder(cmd)
                .inheritIO()
                .start()
                .waitFor()

        if (lastExitCode !in HarmonyEntryPoint.ExitSignal.BASE_EXIT_CODE until (HarmonyEntryPoint.ExitSignal.BASE_EXIT_CODE + HarmonyEntryPoint.ExitSignal.values().size)) {
            System.err.println("WARNING: Harmony daemon ended with unexpected exit code $lastExitCode!")
            System.err.println("This will be treated as an abnormal close exit status!")
            exitSignal = HarmonyEntryPoint.ExitSignal.ABNORMAL_CLOSE
        } else {
            exitSignal = HarmonyEntryPoint.ExitSignal.values()[lastExitCode-8000]
            println("Harmony daemon closed with exit status: $exitSignal")
        }

        if (exitSignal != HarmonyEntryPoint.ExitSignal.COMPLETE_CLOSE
                && (System.currentTimeMillis() - startTime) < MIN_DURATION * 1000) {
            System.err.println("Harmony process died after less than $MIN_DURATION seconds, it will NOT be restarted!")
            break
        }

    } while (exitSignal != HarmonyEntryPoint.ExitSignal.COMPLETE_CLOSE)
}
