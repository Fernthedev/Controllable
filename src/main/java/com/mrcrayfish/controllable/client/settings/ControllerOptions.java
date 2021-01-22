package com.mrcrayfish.controllable.client.settings;

import com.mrcrayfish.controllable.Config;
import com.mrcrayfish.controllable.client.ControllerIcons;
import com.mrcrayfish.controllable.client.CursorType;
import net.minecraft.client.settings.BooleanOption;
import net.minecraft.client.settings.SliderPercentageOption;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TranslationTextComponent;

import java.text.DecimalFormat;

/**
 * Author: MrCrayfish
 */
public class ControllerOptions
{
    private static final DecimalFormat FORMAT = new DecimalFormat("0.0#");

    public static final BooleanOption FORCE_FEEDBACK = new ControllableBooleanOption("controllable.options.forceFeedback", gameSettings -> {
        return Config.CLIENT.options.forceFeedback.get();
    }, (gameSettings, value) -> {
        Config.CLIENT.options.forceFeedback.set(value);
        Config.save();
    });

    public static final BooleanOption AUTO_SELECT = new ControllableBooleanOption("controllable.options.autoSelect", gameSettings -> {
        return Config.CLIENT.options.autoSelect.get();
    }, (gameSettings, value) -> {
        Config.CLIENT.options.autoSelect.set(value);
        Config.save();
    });

    public static final BooleanOption RENDER_MINI_PLAYER = new ControllableBooleanOption("controllable.options.renderMiniPlayer", gameSettings -> {
        return Config.CLIENT.options.renderMiniPlayer.get();
    }, (gameSettings, value) -> {
        Config.CLIENT.options.renderMiniPlayer.set(value);
        Config.save();
    });

    public static final BooleanOption VIRTUAL_MOUSE = new ControllableBooleanOption("controllable.options.virtualMouse", gameSettings -> {
        return Config.CLIENT.options.virtualMouse.get();
    }, (gameSettings, value) -> {
        Config.CLIENT.options.virtualMouse.set(value);
        Config.save();
    });

    public static final BooleanOption CONSOLE_HOTBAR = new ControllableBooleanOption("controllable.options.consoleHotbar", gameSettings -> {
        return Config.CLIENT.options.consoleHotbar.get();
    }, (gameSettings, value) -> {
        Config.CLIENT.options.consoleHotbar.set(value);
        Config.save();
    });

    public static final ControllableEnumOption<CursorType> CURSOR_TYPE = new ControllableEnumOption<>("controllable.options.cursorType", CursorType.class, gameSettings -> {
        return Config.CLIENT.options.cursorType.get();
    }, (gameSettings, cursorType) -> {
        Config.CLIENT.options.cursorType.set(cursorType);
        Config.save();
    }, (gameSettings, controllableEnumOption) -> {
        CursorType cursorType = controllableEnumOption.get(gameSettings);
        return new TranslationTextComponent("controllable.cursor." + cursorType.getString());
    });

    public static final ControllableEnumOption<ControllerIcons> CONTROLLER_TYPE = new ControllableEnumOption<>("controllable.options.controllerIcons", ControllerIcons.class, gameSettings -> {
        return Config.CLIENT.options.controllerIcons.get();
    }, (gameSettings, controllerIcons) -> {
        Config.CLIENT.options.controllerIcons.set(controllerIcons);
        Config.save();
    }, (gameSettings, controllableEnumOption) -> {
        ControllerIcons controllerIcons = controllableEnumOption.get(gameSettings);
        return new TranslationTextComponent("controllable.controller." + controllerIcons.getString());
    });

    public static final BooleanOption INVERT_LOOK = new ControllableBooleanOption("controllable.options.invertLook", gameSettings -> {
        return Config.CLIENT.options.invertLook.get();
    }, (gameSettings, value) -> {
        Config.CLIENT.options.invertLook.set(value);
        Config.save();
    });

    public static final SliderPercentageOption DEAD_ZONE = new ControllableSliderPercentageOption("controllable.options.deadZone", 0.0, 1.0, 0.01F, gameSettings -> {
        return Config.CLIENT.options.deadZone.get();
    }, (gameSettings, value) -> {
        Config.CLIENT.options.deadZone.set(MathHelper.clamp(value, 0.0, 1.0));
        Config.save();
    }, (gameSettings, option) -> {
        double deadZone = Config.CLIENT.options.deadZone.get();
        return new TranslationTextComponent("controllable.options.deadZone.format", FORMAT.format(deadZone));
    });

    public static final SliderPercentageOption ROTATION_SPEED = new ControllableSliderPercentageOption("controllable.options.rotationSpeed", 1.0, 50.0, 1.0F, gameSettings -> {
        return Config.CLIENT.options.rotationSpeed.get();
    }, (gameSettings, value) -> {
        Config.CLIENT.options.rotationSpeed.set(MathHelper.clamp(value, 0.0, 50.0));
        Config.save();
    }, (gameSettings, option) -> {
        double rotationSpeed = Config.CLIENT.options.rotationSpeed.get();
        return new TranslationTextComponent("controllable.options.rotationSpeed.format", FORMAT.format(rotationSpeed));
    });

    public static final SliderPercentageOption MOUSE_SPEED = new ControllableSliderPercentageOption("controllable.options.mouseSpeed", 1.0, 50.0, 1.0F, gameSettings -> {
        return Config.CLIENT.options.mouseSpeed.get();
    }, (gameSettings, value) -> {
        Config.CLIENT.options.mouseSpeed.set(MathHelper.clamp(value, 0.0, 50.0));
        Config.save();
    }, (gameSettings, option) -> {
        double mouseSpeed = Config.CLIENT.options.mouseSpeed.get();
        return new TranslationTextComponent("controllable.options.mouseSpeed.format", FORMAT.format(mouseSpeed));
    });
}
