package mod.inf_iron;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

public class ExpToolRadialScreen extends Screen {

    private int selectedMode = 0;

    public ExpToolRadialScreen() {
        super(Component.literal("ExpTool Modes"));
    }

    @Override
    protected void init() {
        super.init();
        var player = Minecraft.getInstance().player;
        if (player != null) {
            var stack = player.getMainHandItem();
            if (stack.getItem() instanceof OrichalcumExpToolItem) {
                selectedMode = OrichalcumExpToolItem.getMode(stack);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int cx = this.width / 2;
        int cy = this.height / 2;
        int radius = 80;
        int modes = OrichalcumExpToolItem.MODE_NAMES.length;

        graphics.drawCenteredString(this.font, "§4§lExpTool 処刑モード", cx, cy - radius - 30, 0xFFFFFF);
        for (int i = 0; i < modes; i++) {
            double angle = (2 * Math.PI * i / modes) - Math.PI / 2;
            int x = cx + (int) (Math.cos(angle) * radius);
            int y = cy + (int) (Math.sin(angle) * radius);
            boolean selected = i == selectedMode;
            int color = selected ? 0xFFFF55 : 0xAAAAAA;
            graphics.drawCenteredString(this.font, (selected ? "§e> " : "") + OrichalcumExpToolItem.MODE_NAMES[i], x, y, color);
        }
        graphics.drawCenteredString(this.font, "§7クリックで決定 / Escで閉じる", cx, cy + radius + 24, 0x888888);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int radius = 80;
        int modes = OrichalcumExpToolItem.MODE_NAMES.length;
        for (int i = 0; i < modes; i++) {
            double angle = (2 * Math.PI * i / modes) - Math.PI / 2;
            int x = cx + (int) (Math.cos(angle) * radius);
            int y = cy + (int) (Math.sin(angle) * radius);
            if (mouseX >= x - 60 && mouseX <= x + 60 && mouseY >= y - 8 && mouseY <= y + 8) {
                selectedMode = i;
                ModNetwork.CHANNEL.sendToServer(new ExpToolSkillPacket(selectedMode));
                this.onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)
                || ClientKeybinds.KEY_EXP_TOOL_MENU.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
