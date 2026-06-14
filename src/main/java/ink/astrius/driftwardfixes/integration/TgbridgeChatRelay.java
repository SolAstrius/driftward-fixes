package ink.astrius.driftwardfixes.integration;

import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridges public player chat to tgbridge when a chat-formatting mod swallows it.
 *
 * <p>tgbridge's NeoForge build forwards chat by listening to NeoForge's {@code ServerChatEvent}.
 * StyledChat (running here via Sinytra Connector) {@code @Redirect}s the chat decorator inside
 * {@code ServerGamePacketListenerImpl#lambda$handleChat$0}, which shadows NeoForge's event
 * injection point, so that event never fires for player chat — death/join/leave/advancements use
 * separate hooks and keep working. See {@code ChatRelayMixin}.
 *
 * <p>This re-feeds the message into tgbridge from {@code broadcastChatMessage} (where StyledChat
 * only injects, never cancels) via tgbridge's documented public API
 * ({@code TelegramBridge.INSTANCE.onChatMessage(...)}). The actual API calls live in
 * {@link TgbridgeChatForwarder}, which is only loaded once tgbridge is confirmed present, so this
 * gate carries no hard tgbridge dependency and silently no-ops if tgbridge (or StyledChat) is
 * absent — the same optionality the old reflective implementation had, now type-checked at compile
 * time against the vendored {@code libs/tgbridge.jar}.
 */
public final class TgbridgeChatRelay {
    private static final Logger LOGGER = LoggerFactory.getLogger("driftward-fixes/tgbridge-relay");

    private static volatile boolean initialized;
    private static boolean enabled;

    private TgbridgeChatRelay() {}

    /** Forward a public chat message to Telegram. Called from the server thread. */
    public static void relay(ServerPlayer player, PlayerChatMessage message) {
        if (player == null || message == null) {
            return;
        }
        if (!initialized) {
            init();
        }
        if (!enabled) {
            return;
        }
        // Touches tgbridge types — only reached (and only class-loaded) when tgbridge is present.
        TgbridgeChatForwarder.forward(player, message);
    }

    private static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ModList mods = ModList.get();
        // tgbridge must be present (TgbridgeChatForwarder references its classes). StyledChat must
        // also be present: without it tgbridge's native ServerChatEvent listener handles chat and
        // relaying here too would post every message to Telegram twice.
        enabled = mods.isLoaded("tgbridge") && mods.isLoaded("styledchat");
        if (enabled) {
            LOGGER.info("tgbridge chat relay enabled (StyledChat compatibility bridge)");
        }
    }
}
