package ink.astrius.driftwardfixes.mixin;

import ink.astrius.driftwardfixes.integration.TgbridgeChatRelay;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// StyledChat (via Sinytra Connector) redirects the chat decorator, which shadows NeoForge's
// ServerChatEvent injection point — so tgbridge's native chat listener never fires and player
// chat never reaches Telegram (death/join/leave/advancements use other hooks and still work).
//
// broadcastChatMessage(PlayerChatMessage) is where StyledChat itself @Injects (HEAD, no cancel),
// so it is guaranteed to run for every public chat message. We tap the same point and hand the
// message to tgbridge. See TgbridgeChatRelay for the (reflective, optional) forwarding.
@Mixin(ServerGamePacketListenerImpl.class)
public class ChatRelayMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;)V", at = @At("HEAD"))
    private void driftward$relayChatToTelegram(PlayerChatMessage message, CallbackInfo ci) {
        TgbridgeChatRelay.relay(this.player, message);
    }
}
