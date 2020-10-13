package com.mrcrayfish.controllable.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mrcrayfish.controllable.client.gui.ControllerLayoutScreen;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.gui.toasts.ToastGui;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

/**
 * Author: MrCrayfish
 */
public class ControllerToast implements IToast
{
    private boolean connected;
    private String controllerName;

    public ControllerToast(boolean connected, String controllerName)
    {
        this.connected = connected;
        this.controllerName = controllerName;
    }

    @Override
    //    TODO: REPLACE WHEN MAPPINGS DONE
    //     public Visibility draw(ToastGui toastGui, long delta)
    public Visibility func_230444_a_(MatrixStack matrixStack, ToastGui toastGui, long delta)
    {
        toastGui.getMinecraft().getTextureManager().bindTexture(TEXTURE_TOASTS);

        toastGui.blit(matrixStack, 0, 0, 0, 32, 160, 32);

        toastGui.getMinecraft().getTextureManager().bindTexture(ControllerLayoutScreen.TEXTURE);
        toastGui.blit(matrixStack, 8, 8, 20, 43, 20, 16);

        // TODO: REPLACE WHEN MAPPINGS DONE
//        String title = toastGui.getMinecraft().fontRenderer.trimStringToWidth(controllerName, 120);
        String title = toastGui.getMinecraft().fontRenderer.func_238412_a_(controllerName, 120);
        toastGui.getMinecraft().fontRenderer.drawString(matrixStack, TextFormatting.DARK_GRAY + title, 35, 7, 0);

        String message = this.connected ?
                TextFormatting.DARK_GREEN.toString() + TextFormatting.BOLD.toString() + I18n.format("controllable.toast.connected") :
                TextFormatting.RED.toString() + TextFormatting.BOLD.toString() + I18n.format("controllable.toast.disconnected");
        toastGui.getMinecraft().fontRenderer.drawString(matrixStack, TextFormatting.BOLD + message, 35, 18, 0);

        return delta >= 3000L ? IToast.Visibility.HIDE : IToast.Visibility.SHOW;
    }


}
