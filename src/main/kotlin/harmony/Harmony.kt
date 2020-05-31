package harmony

import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.presence.Presence
import discord4j.core.shard.ShardingStrategy
import discord4j.discordjson.json.gateway.StatusUpdate
import harmony.command.CommandHandler
import harmony.command.CommandOptions
import harmony.util.Feature

/**
 * The class that handles everything. Instantiation automatically logs in.
 *
 * @param token The bot token.
 * @param commands The options for commands. If disabled no commands are handled.
 * @param clientHook A callback for decorating a [DiscordClient] before logging in.
 * @param gatewayHook A callback for decorating a [GatewayDiscordClient] just after logging in.
 *
 * @see CommandOptions
 */
class Harmony @JvmOverloads constructor(token: String,
                                        val commands: Feature<CommandOptions> = Feature.enable(CommandOptions()),
                                        clientHook: (DiscordClient) -> DiscordClient = { dc -> dc },
                                        gatewayHook: (GatewayDiscordClient) -> GatewayDiscordClient = { gdc -> gdc },
                                        initialPresence: StatusUpdate = Presence.online()) {

    /**
     * The Discord4J client.
     */
    val client: GatewayDiscordClient

    /**
     * The bot's user.
     */
    val self: User

    /**
     * The bot's owner's user.
     */
    val owner: User

    /**
     * The status the bot is using.
     */
    var status: StatusUpdate = initialPresence
        get() = field
        set(value) {
            field = value
            client.updatePresence(value).subscribe()
        }

    internal val selfAsMention: String
    internal val selfAsMentionWithNick: String
    internal val commandHandler: CommandHandler?

    init {
        val dc = clientHook(DiscordClient
                .builder(token)
                .build())
        client = dc.gateway()
                .setSharding(ShardingStrategy.recommended())
                .setInitialStatus { si ->
                    status
                }
                .login()
                .map(gatewayHook)
                .block()!!
        self = client.self.block()!!
        selfAsMention = "<@${self.id.asString()}>"
        selfAsMentionWithNick = "<@!${self.id.asString()}>"
        owner = client.applicationInfo.flatMap { it.owner }.block()!!
        commandHandler = commands ifEnabled {
            it.commandHook(this, it).apply {
                this.setup(client).subscribe()
            }
        }
    }

    /**
     * Blocks the thread until the bot logs out.
     *
     * @see stop
     */
    fun awaitClose() {
        client.onDisconnect().block()
    }

    /**
     * Logs the bot out.
     *
     * @see awaitClose
     */
    fun stop() {
        client.logout().block()
    }
}