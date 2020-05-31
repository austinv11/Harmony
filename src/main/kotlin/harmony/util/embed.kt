package harmony.util

import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import java.time.Instant
import java.util.function.Consumer

/**
 * DSL for building an embed.
 *
 * @see Embed
 */
fun embed(modifier: Embed.() -> Unit): Embed = Embed().apply(modifier)

// https://i.stack.imgur.com/HRWHk.png
/**
 * @see EmbedCreateSpec
 */
data class EmbedField(val name: String, val value: String, val inline: Boolean)

/**
 * @see EmbedCreateSpec
 */
data class Embed @JvmOverloads constructor(
    var title: String? = null,
    var description: String? = null,
    var url: String? = null,
    var timestamp: Instant? = null,
    var color: Color? = null,
    var footer: String? = null,
    var footerIconUrl: String? = null,
    var imageUrl: String? = null,
    var thumbnailUrl: String? = null,
    var author: String? = null,
    var authorUrl: String? = null,
    var authorIconUrl: String? = null,
    var fields: Array<EmbedField>? = null
) {

    fun addField(field: EmbedField) {
        fields = if (fields == null) {
            arrayOf(field)
        } else {
            fields!! + field
        }
    }

    fun send(channel: MessageChannel) = channel.createEmbed(toSpecConsumer())

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    internal fun toSpecConsumer() = Consumer<EmbedCreateSpec> {
        if (title != null)
            it.setTitle(title)

        if (description != null)
            it.setDescription(description)

        if (url != null)
            it.setUrl(url)

        if (timestamp != null)
            it.setTimestamp(timestamp)

        if (color != null)
            it.setColor(color)

        if (footer != null)
            it.setFooter(footer, footerIconUrl)

        if (imageUrl != null)
            it.setImage(imageUrl)

        if (thumbnailUrl != null)
            it.setThumbnail(thumbnailUrl)

        if (author != null)
            it.setAuthor(author, authorUrl, authorIconUrl)

        if (fields != null)
            fields!!.forEach { f -> it.addField(f.name, f.value, f.inline) }
    }
}
