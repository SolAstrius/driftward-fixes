package ink.astrius.driftwardfixes.integration;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

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
 * only injects, never cancels) by calling {@code TelegramBridge.INSTANCE.onChatMessage(...)}.
 * Everything is reflective so the mod carries no tgbridge compile dependency and silently no-ops
 * if tgbridge (or StyledChat) is absent.
 */
public final class TgbridgeChatRelay {
    private static final Logger LOGGER = LoggerFactory.getLogger("driftward-fixes/tgbridge-relay");

    private static volatile boolean initialized;
    private static boolean enabled;
    private static boolean warned;

    // Cached reflective handles (resolved once in init()).
    private static Object companion;
    private static Method companionGetInstance;
    private static Method bridgeGetPlatform;
    private static Method platformPlayerToTgbridge;
    private static Method utilsToAdventure;
    private static Method bridgeOnChatMessage;
    private static Constructor<?> eventCtor;

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
        try {
            Object bridge = companionGetInstance.invoke(companion);
            if (bridge == null) {
                return;
            }
            Object platform = bridgeGetPlatform.invoke(bridge);
            Object sender = platformPlayerToTgbridge.invoke(platform, player);
            // Forward the raw, signed message body — exactly what tgbridge's own
            // ServerChatEvent listener would have used. tgbridge wraps it in its
            // configured telegramFormat and applies replacements.json itself.
            Object adventureMessage = utilsToAdventure.invoke(null, Component.literal(message.signedContent()));
            Object event = eventCtor.newInstance(sender, adventureMessage, null, null, false);
            bridgeOnChatMessage.invoke(bridge, event);
        } catch (Throwable t) {
            warnOnce("Failed to relay chat message to tgbridge", t);
        }
    }

    private static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            ModList mods = ModList.get();
            if (!mods.isLoaded("tgbridge")) {
                return;
            }
            // Only relay when a chat mod is actually intercepting the event; otherwise
            // tgbridge's native listener handles chat and we'd forward every message twice.
            if (!mods.isLoaded("styledchat")) {
                return;
            }

            Class<?> bridgeCls = Class.forName("dev.vanutp.tgbridge.common.TelegramBridge");
            companion = bridgeCls.getField("Companion").get(null);
            companionGetInstance = companion.getClass().getMethod("getINSTANCE");
            bridgeGetPlatform = bridgeCls.getMethod("getPlatform");

            Class<?> platformCls = Class.forName("dev.vanutp.tgbridge.common.IPlatform");
            platformPlayerToTgbridge = platformCls.getMethod("playerToTgbridge", Object.class);

            Class<?> utilsCls = Class.forName("dev.vanutp.tgbridge.forge.UtilsKt");
            utilsToAdventure = utilsCls.getMethod("toAdventure", Component.class);

            Class<?> eventCls = Class.forName("dev.vanutp.tgbridge.common.models.TgbridgeMcChatMessageEvent");
            bridgeOnChatMessage = bridgeCls.getMethod("onChatMessage", eventCls);
            // Primary constructor: (ITgbridgePlayer sender, Component message,
            //                       String chatName, Object originalEvent, boolean isCancelled)
            for (Constructor<?> c : eventCls.getConstructors()) {
                if (c.getParameterCount() == 5) {
                    eventCtor = c;
                    break;
                }
            }
            if (eventCtor == null) {
                throw new NoSuchMethodException("TgbridgeMcChatMessageEvent 5-arg constructor not found");
            }

            enabled = true;
            LOGGER.info("tgbridge chat relay enabled (StyledChat compatibility bridge)");
        } catch (Throwable t) {
            warnOnce("Could not wire up tgbridge chat relay; player chat will not reach Telegram", t);
        }
    }

    private static void warnOnce(String message, Throwable t) {
        if (!warned) {
            warned = true;
            LOGGER.warn(message, t);
        }
    }
}
