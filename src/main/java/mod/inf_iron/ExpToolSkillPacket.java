package mod.inf_iron;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ExpToolSkillPacket {

    private final int modeId;

    public ExpToolSkillPacket(int modeId) {
        this.modeId = modeId;
    }

    public ExpToolSkillPacket(FriendlyByteBuf buffer) {
        this.modeId = buffer.readVarInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.modeId);
    }

    public static void handle(ExpToolSkillPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof OrichalcumExpToolItem) {
                OrichalcumExpToolItem.setMode(stack, packet.modeId);
                int m = OrichalcumExpToolItem.getMode(stack);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "§4§l処刑モード: §r§f" + OrichalcumExpToolItem.MODE_NAMES[m]), true);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
