package ink.astrius.driftwardfixes.integration;

import dev.vanutp.tgbridge.common.IPlatform;
import dev.vanutp.tgbridge.common.TelegramBridge;
import dev.vanutp.tgbridge.common.models.ITgbridgePlayer;
import dev.vanutp.tgbridge.common.models.TgbridgeMcChatMessageEvent;
import dev.vanutp.tgbridge.forge.UtilsKt;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Type-safe forwarder onto tgbridge's public API. Statically references tgbridge classes, so it is
 * loaded only after {@link TgbridgeChatRelay} confirms tgbridge is present (otherwise touching it
 * would raise {@code NoClassDefFoundError}).
 */
final class TgbridgeChatForwarder {
    private static final Logger LOGGER = LoggerFactory.getLogger("driftward-fixes/tgbridge-relay");

    private static boolean warned;

    private TgbridgeChatForwarder() {}

    static void forward(ServerPlayer player, PlayerChatMessage message) {
        try {
            TelegramBridge bridge = TelegramBridge.Companion.getINSTANCE();
            if (bridge == null) {
                return;
            }
            IPlatform platform = bridge.getPlatform();
            ITgbridgePlayer sender = platform.playerToTgbridge(player);
            if (sender == null) {
                return;
            }
            // Forward the DECORATED content: StyledChat's chat-decorator redirect renders
            // markdown/emoji/links into native text styling here (the message body, without the
            // "<player>" prefix). tgbridge's MinecraftToTelegramConverter maps those styles to
            // Telegram entities (bold/italic/underline/strikethrough/spoiler/text_link), so the
            // formatting shows in Telegram just like in-game. (signedContent() would be the raw
            // typed string, e.g. "**test**", which Telegram would show verbatim.)
            //
            // toAdventure returns tgbridge's relocated adventure Component (tgbridge.shaded.kyori),
            // which is exactly the type onChatMessage's event expects.
            var adventureMessage = UtilsKt.toAdventure(message.decoratedContent());
            bridge.onChatMessage(new TgbridgeMcChatMessageEvent(sender, adventureMessage, null, null, false));
        } catch (Throwable t) {
            warnOnce(t);
        }
    }

    private static void warnOnce(Throwable t) {
        if (!warned) {
            warned = true;
            LOGGER.warn("Failed to relay chat message to tgbridge", t);
        }
    }
}
