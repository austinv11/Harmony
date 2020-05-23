package harmony.command.interfaces;

import com.austinv11.servicer.Service;
import harmony.Harmony;

/**
 * Use with @WireService to have the bot automatically managed. There is undefined behavior if there are multiple
 * entry points
 */
@Service
public interface HarmonyEntryPoint {

    default String getToken(String[] programArgs) {
        return programArgs[0];  // Assumes token is the only arg
    }

    default Harmony buildHarmony(String token) {
        return new Harmony(token);
    }

    default ExitSignal startBot(Harmony harmony) { // Basic handler that restarts on exceptions
        try {
            harmony.awaitClose();
            return ExitSignal.COMPLETE_CLOSE;
        } catch (Throwable t) {
            return ExitSignal.RESTART;
        }
    }

    enum ExitSignal {
        COMPLETE_CLOSE,
        ABNORMAL_CLOSE,
        RESTART;

        public static final int BASE_EXIT_CODE = 8000;

        public int toExitCode() {
            return BASE_EXIT_CODE + this.ordinal();
        }
    }
}
