package harmony.command.interfaces;

import discord4j.common.util.Snowflake;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * This interface is useful for programmatically defining a prefix.
 *
 * @see #staticPrefixProvider(String)
 * @see #noPrefixProvider()
 * @see harmony.command.CommandOptions
 */
public interface PrefixProvider {

    /**
     * This is called to dynamically retrieve a prefix for a given guild.
     *
     * @param guildId The guild id.
     * @param channelId The channel id.
     * @return The prefix if there is one, else empty.
     */
    @NotNull
    Optional<String> getGuildPrefix(@NotNull Snowflake guildId, @NotNull Snowflake channelId);

    /**
     * This is called to dynamically retrieve a prefix for a given private channel.
     *
     * @param authorId The dm's author's id.
     * @return The prefix if there is one, else empty.
     */
    @NotNull
    Optional<String> getDmPrefix(@NotNull Snowflake authorId);

    /**
     * Creates a PrefixProvider that returns the same prefix regardless of context.
     *
     * @param prefix The prefix.
     * @return The static prefix provider.
     */
    @NotNull
    static PrefixProvider staticPrefixProvider(@NotNull String prefix) {
        return new PrefixProvider() {
            @NotNull
            @Override
            public Optional<String> getGuildPrefix(@NotNull Snowflake guildId, @NotNull Snowflake channelId) {
                return Optional.of(prefix);
            }

            @NotNull
            @Override
            public Optional<String> getDmPrefix(@NotNull Snowflake authorId) {
                return Optional.of(prefix);
            }
        };
    }

    /**
     * Creates a PrefixProvider that returns no prefix (disables prefixes).
     *
     * @return The no prefix provider.
     */
    @NotNull
    static PrefixProvider noPrefixProvider() {
        return new PrefixProvider() {
            @NotNull
            @Override
            public Optional<String> getGuildPrefix(@NotNull Snowflake guildId, @NotNull Snowflake channelId) {
                return Optional.empty();
            }

            @NotNull
            @Override
            public Optional<String> getDmPrefix(@NotNull Snowflake authorId) {
                return Optional.empty();
            }
        };
    }
}
