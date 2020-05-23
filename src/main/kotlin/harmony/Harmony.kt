package harmony

import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.User
import discord4j.core.shard.ShardingStrategy
import harmony.command.CommandHandler
import harmony.command.CommandOptions
import harmony.util.Feature

class Harmony @JvmOverloads constructor(token: String,
                                        val commands: Feature<CommandOptions> = Feature.enable(CommandOptions()),
                                        clientHook: (DiscordClient) -> DiscordClient = { dc -> dc },
                                        gatewayHook: (GatewayDiscordClient) -> GatewayDiscordClient = { gdc -> gdc }) {

    val client: GatewayDiscordClient
    val self: User
    val owner: User
    internal val selfAsMention: String
    internal val selfAsMentionWithNick: String
    internal val commandHandler: CommandHandler?

    init {
        val dc = clientHook(DiscordClient
                .builder(token)
                .build())
        client = dc.gateway()
                .setSharding(ShardingStrategy.recommended())
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

    fun awaitClose() {
        client.onDisconnect().block()
    }

    fun stop() {
        client.logout().block()
    }
}