package com.mrcrayfish.controllable.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mrcrayfish.controllable.Controllable;
import com.mrcrayfish.controllable.registry.ButtonRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.glfw.GLFW;

/**
 * Author: MrCrayfish
 */
@SuppressWarnings("unused")
public class Hooks
{
    /**
     * Used in order to fix block breaking progress. This method is linked via ASM.
     */
    @SuppressWarnings("unused")
    public static boolean isLeftClicking()
    {
        Minecraft mc = Minecraft.getInstance();
        boolean isLeftClicking = mc.gameSettings.keyBindAttack.isKeyDown();
        Controller controller = Controllable.getController();
        if(controller != null)
        {
            if(ButtonRegistry.ButtonActions.ATTACK.getButton().isButtonDown())
            {
                isLeftClicking = true;
            }
        }
        boolean usingVirtualMouse = (Controllable.getOptions().isVirtualMouse() && Controllable.getInput().getLastUse() > 0);
        return mc.currentScreen == null && isLeftClicking && (mc.mouseHelper.isMouseGrabbed() || usingVirtualMouse);
    }

    /**
     * Used in order to fix actions like eating or pulling bow back. This method is linked via ASM.
     */
    @SuppressWarnings("unused")
    public static boolean isRightClicking()
    {
        Minecraft mc = Minecraft.getInstance();
        boolean isRightClicking = mc.gameSettings.keyBindUseItem.isKeyDown();
        Controller controller = Controllable.getController();
        if(controller != null)
        {
            if(ButtonRegistry.ButtonActions.USE_ITEM.getButton().isButtonDown())
            {
                isRightClicking = true;
            }
        }
        return isRightClicking;
    }

    /**
     * Used in order to fix the quick move check in inventories. This method is linked via ASM.
     */
    @SuppressWarnings("unused")
    public static boolean canQuickMove()
    {
        boolean canQuickMove = InputMappings.isKeyDown(Minecraft.getInstance().getMainWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) || InputMappings.isKeyDown(Minecraft.getInstance().getMainWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT);
        Controller controller = Controllable.getController();
        if(controller != null)
        {
            if(ButtonRegistry.ButtonActions.QUICK_MOVE.getButton().isButtonDown())
            {
                canQuickMove = true;
            }
        }
        return canQuickMove;
    }

    /**
     * Allows the player list to be shown. This method is linked via ASM.
     */
    @SuppressWarnings("unused")
    public static boolean canShowPlayerList()
    {
        Minecraft mc = Minecraft.getInstance();
        boolean canShowPlayerList = mc.gameSettings.keyBindPlayerList.isKeyDown();
        Controller controller = Controllable.getController();
        if(controller != null)
        {
            if(ButtonRegistry.ButtonActions.PLAYER_LIST.getButton().isButtonDown())
            {
                canShowPlayerList = true;
            }
        }
        return canShowPlayerList;
    }

    /**
     * Fixes the mouse position when virtual mouse is turned on for controllers. This method is linked via ASM.
     */
    @SuppressWarnings("unused")
    public static void drawScreen(MatrixStack matrixStack, Screen screen, int mouseX, int mouseY, float partialTicks)
    {
        ControllerInput input = Controllable.getInput();
        if(Controllable.getController() != null && Controllable.getOptions().isVirtualMouse() && input.getLastUse() > 0)
        {
            Minecraft minecraft = Minecraft.getInstance();
            mouseX = (int) (input.getVirtualMouseX() * (double) minecraft.getMainWindow().getScaledWidth() / (double) minecraft.getMainWindow().getWidth());
            mouseY = (int) (input.getVirtualMouseY() * (double) minecraft.getMainWindow().getScaledHeight() / (double) minecraft.getMainWindow().getHeight());
        }
        if(!MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.DrawScreenEvent.Pre(screen, matrixStack, mouseX, mouseY, partialTicks)))
        {
            screen.render(matrixStack, mouseX, mouseY, partialTicks);
        }
        MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.DrawScreenEvent.Post(screen, matrixStack, mouseX, mouseY, partialTicks));
    }

    /**
     * Fixes selected item name rendering not being offset by console hotbar
     */
    @SuppressWarnings("unused")
    public static void applyHotbarOffset(MatrixStack matrixStack)
    {
        if(Controllable.getOptions().useConsoleHotbar())
        {
            matrixStack.translate(0, -20, 0);
        }
    }
}
