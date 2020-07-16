package com.mrcrayfish.controllable.client.gui;

import com.github.fernthedev.config.common.exceptions.ConfigLoadException;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mrcrayfish.controllable.Controllable;
import com.mrcrayfish.controllable.client.ButtonBinding;
import com.mrcrayfish.controllable.client.ControllerProperties;
import com.mrcrayfish.controllable.client.settings.ControllableBooleanOption;
import com.mrcrayfish.controllable.event.ControllerEvent;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ControlsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.Util;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * @author Fernthedev
 * {@link "https://github.com/Fernthedev"}
 */
public class ControllerActionMenu extends ControlsScreen {

    public ControllerActionList.KeyEntry entry;
    public ButtonBinding controllerButtonId;
    public String action;

    private ControllerActionList buttonActionList;
    private Button buttonReset;


    private boolean showIcons = true;


    private ControllableBooleanOption buttonShowIcons = new ControllableBooleanOption("controllable.gui.option.actionMenuShowIcons", gameSettings -> {
        return showIcons;
    }, (gameSettings, value) -> {
        showIcons = value;
    });

    public ControllerActionMenu(Screen screen, GameSettings settings) {
        super(screen, settings);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    protected void init() {
        this.buttonActionList = new ControllerActionList(this, this.minecraft);
        this.children.add(buttonActionList);

        this.buttonReset = this.addButton(new Button(this.width / 2 - 155, this.height - 29, 150, 20, new TranslationTextComponent("controls.resetAll"), (p_213125_1_) -> {
            Controllable.getButtonRegistry().getButtonBindings().forEach((action, buttonBinding) -> {
                buttonBinding.resetButton();
            });

            try {
                ControllerProperties.saveActionRegistry();
            } catch (ConfigLoadException e) {
                e.printStackTrace();
            }

        }));
        addButton(buttonShowIcons.createWidget(this.gameSettings, this.width / 2 - (150 / 2), 18, 150));

        this.addButton(new Button(this.width / 2 - 155 + 160, this.height - 29, 150, 20, new TranslationTextComponent("gui.done"), (p_213124_1_) -> {
            this.minecraft.displayGuiScreen(this.parentScreen);
            MinecraftForge.EVENT_BUS.unregister(this); // Prevent storing this instance in the event bus.
        }));
    }

    public void render(MatrixStack matrixStack, int p_render_1_, int p_render_2_, float p_render_3_) {
//        this.renderBackground(matrixStack);
        this.buttonActionList.render(matrixStack, p_render_1_, p_render_2_, p_render_3_);
        this.drawCenteredString(matrixStack, this.font, this.title.getString(), this.width / 2, 8, 16777215);
        boolean flag = false;

        for (ButtonBinding buttonBinding : Controllable.getButtonRegistry().getButtonBindings().values()) {
            if (!buttonBinding.isDefault()) {
                flag = true;
                break;
            }
        }

        this.buttonReset.active = flag;

        for (Widget button : this.buttons) {
            button.render(matrixStack, p_render_1_, p_render_2_, p_render_3_);
        }
    }


    @SubscribeEvent
    public void buttonPressedEvent(ControllerEvent.ButtonInput e) {
        if (Minecraft.getInstance().currentScreen instanceof ControllerActionMenu) {
            if (this.controllerButtonId != null) {

                if (e.getState()) {
                    Controllable.getButtonRegistry().getButton(action).setButton(e.getModifiedButton());

                    try {
                        ControllerProperties.saveActionRegistry();
                    } catch (ConfigLoadException ex) {
                        ex.printStackTrace();
                    }

                    controllerButtonId = null;
                    entry = null;
                    action = null;
                }

                this.time = Util.milliTime();
            }
        }
    }

    public boolean showIcons()
    {
        return showIcons;
    }
}
