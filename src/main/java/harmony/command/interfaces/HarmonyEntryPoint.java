package harmony.command.interfaces;

import com.austinv11.servicer.Service;
import harmony.Harmony;
import org.jetbrains.annotations.NotNull;

/**
 * When implemented and marked with {@link com.austinv11.servicer.WireService}, it is used as the bots entry point.
 * If no custom main method is declared. This is useful as it will wrap your bot such that you can easily signal a
 * restart, restart automatically after a fatal error, and signal restart your bot after updating the bot's jar files.
 * <p>
 * The interface has sensible defaults already, so if you want Harmony to handle everything, simply implement this
 * interface, annotate the class with {@link com.austinv11.servicer.WireService} and run the harmony jar.
 * <p>
 * <b>NOTE: </b>There is undefined behavior for entry point resolution if there are multiple entry points declared.
 *
 * @see com.austinv11.servicer.WireService
 * @see Harmony
 */
@Service
public interface HarmonyEntryPoint {

    /**
     * This is called to parse the bot token from the passed arguments to the jar file.
     *
     * By default, this assumes that the first argument in the array is the token.
     *
     * @param programArgs The args received from the main method.
     * @return The token to use.
     */
    default @NotNull String getToken(@NotNull String[] programArgs) {
        return programArgs[0];  // Assumes token is the only arg
    }

    /**
     * This builds a {@link Harmony} instance given a token. This is useful for customizing Harmony's behavior.
     *
     * By default, this builds {@link Harmony} with the default options.
     *
     * @param token The token to log in with.
     * @return The harmony instance to use for the bot.
     *
     * @see Harmony
     */
    default @NotNull Harmony buildHarmony(@NotNull String token) {
        return new Harmony(token);
    }

    /**
     * This is a callback to handle any additional logic, as well as for signalling for exits/restarts.
     *
     * By default, it simply waits for the harmony instance to stop. If any exceptions are thrown, it will restart the
     * bot.
     *
     * @param harmony The harmony instance.
     * @return The exit reason.
     *
     * @see Harmony#stop()
     */
    default @NotNull ExitSignal startBot(@NotNull Harmony harmony) {
        try {
            harmony.awaitClose();
            return ExitSignal.COMPLETE_CLOSE;
        } catch (Throwable t) {
            System.err.println("Error caught! Restarting...");
            t.printStackTrace();
            return ExitSignal.RESTART;
        }
    }

    /**
     * An enum representing possible reasons for the bot closing. The bot will restart in all cases except for
     * COMPLETE_CLOSE.
     */
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
