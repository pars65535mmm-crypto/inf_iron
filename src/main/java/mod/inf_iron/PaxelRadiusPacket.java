package mod.inf_iron;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PaxelRadiusPacket {
    private final int delta; 

    public PaxelRadiusPacket(int delta) {
        this.delta = delta;
    }

    public PaxelRadiusPacket(FriendlyByteBuf buffer) {
        this.delta = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.delta);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ItemStack stack = player.getMainHandItem();
                if (stack.getItem() instanceof OrichalcumPaxelItem) {
                    int currentRadius = OrichalcumPaxelItem.getMiningRadius(stack);
                    int newRadius = currentRadius + delta;
                    if (newRadius < 0) newRadius = 0; // 0 = 1x1
                    if (newRadius > 8) newRadius = 8; // 8 = 17x17 
                    OrichalcumPaxelItem.setMiningRadius(stack, newRadius);
                    
                    int size = (newRadius * 2) + 1;
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§b[Orichalcum Paxel] §f採掘範囲: " + size + "x" + size), true);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
