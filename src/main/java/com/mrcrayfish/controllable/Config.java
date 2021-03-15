package com.mrcrayfish.controllable;

import com.mrcrayfish.controllable.client.ActionVisibility;
import com.mrcrayfish.controllable.client.ControllerIcons;
import com.mrcrayfish.controllable.client.CursorType;
import net.minecraft.util.IStringSerializable;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Author: MrCrayfish
 */
public class Config
{
    static final ForgeConfigSpec clientSpec;
    public static final Config.Client CLIENT;

    public static class Client
    {
        public final ForgeConfigSpec.LongValue controllerPollInterval;
        public final Options options;

        Client(ForgeConfigSpec.Builder builder)
        {
            builder.comment("Client configuration settings").push("client");
            this.controllerPollInterval = builder
                    .comment("The time in milliseconds to wait before polling the controller. The lower the value the better the input latency but drains the controller battery faster.")
                    .translation("controllable.configgui.controllerPollInterval")
                    .defineInRange("controllerPollInterval", 8L, 1L, 128L);
            this.options = new Options(builder);
            builder.pop();
        }

        public static class Options
        {
            public final ForgeConfigSpec.BooleanValue forceFeedback;
            public final ForgeConfigSpec.BooleanValue autoSelect;
            public final ForgeConfigSpec.BooleanValue renderMiniPlayer;
            public final ForgeConfigSpec.BooleanValue virtualMouse;
            public final ForgeConfigSpec.BooleanValue consoleHotbar;
            public final ForgeConfigSpec.EnumValue<CursorType> cursorType;
            public final ForgeConfigSpec.EnumValue<ControllerIcons> controllerIcons;
            public final ForgeConfigSpec.BooleanValue invertLook;
            public final ForgeConfigSpec.DoubleValue deadZone;
            public final ForgeConfigSpec.DoubleValue rotationSpeed;
            public final ForgeConfigSpec.DoubleValue mouseSpeed;
            public final ForgeConfigSpec.EnumValue<ActionVisibility> showActions;
            public final ForgeConfigSpec.BooleanValue quickCraft;
            public final ForgeConfigSpec.IntValue aimAssistIntensity;

            public final ForgeConfigSpec.IntValue attackSpeed;

            public final ForgeConfigSpec.BooleanValue toggleSprint;
            public final ForgeConfigSpec.BooleanValue aimAssist;

            public final ForgeConfigSpec.EnumValue<AimAssistMode> hostileAimMode;
            public final ForgeConfigSpec.EnumValue<AimAssistMode> animalAimMode;
            public final ForgeConfigSpec.EnumValue<AimAssistMode> playerAimMode;

            public final ForgeConfigSpec.BooleanValue toggleIgnoreSameTeam;
            public final ForgeConfigSpec.BooleanValue toggleIgnoreSameTeamFriendlyFire;
            public final ForgeConfigSpec.BooleanValue toggleIgnorePets;

            public Options(ForgeConfigSpec.Builder builder)
            {
                builder.comment("In-game related options. These can be changed in game instead of config!").push("options");
                {
                    this.forceFeedback = builder.comment("If enabled, some actions will cause the controller to vibrate").define("forceFeedback", true);
                    this.autoSelect = builder.comment("If enabled, controller will be automatically selected on start up or when plugged in").define("autoSelect", true);
                    this.renderMiniPlayer = builder.comment("If enabled, the player will render in the top left corner likes Bedrock Edition").define("renderMiniPlayer", true);
                    this.virtualMouse = builder.comment("If enabled, the game will use a virtual cursor instead of the real cursor. This must be turned on to be able to run multiple instances!").define("virtualMouse", true);
                    this.consoleHotbar = builder.comment("If enabled, hotbar will render closer to the center of the screen like on console.").define("consoleHotbar", false);
                    this.cursorType = builder.comment("The image to use for the cursor. This only applies if virtual mouse is enabled!").defineEnum("cursorType", CursorType.LIGHT);
                    this.controllerIcons = builder.comment("The controller icons to use in game to display actions").defineEnum("controllerIcons", ControllerIcons.DEFAULT);
                    this.invertLook = builder.comment("If enabled, inverts the controls on the Y axis for the camera").define("invertLook", false);
                    this.deadZone = builder.comment("The distance you have to move the thumbstick before it's input is registered. This fixes drifting as some thumbsticks don't center to zero.").defineInRange("deadZone", 0.15, 0.0, 1.0);
                    this.rotationSpeed = builder.comment("The speed which the camera turns in game").defineInRange("rotationSpeed", 45.0, 0.0, 200.0);
                    this.mouseSpeed = builder.comment("The speed which the cursor or virtual mouse moves around the screen").defineInRange("mouseSpeed", 15.0, 0.0, 50.0);
                    this.showActions = builder.comment("If enabled, shows common actions when displaying available on the screen").defineEnum("showActions", ActionVisibility.MINIMAL);
                    this.quickCraft = builder.comment("If enabled, allows you to craft quickly when clicking an item in the recipe book").define("quickCraft", true);
                    this.attackSpeed = builder.comment("The attack speed of the auto-attack").defineInRange("attackSpeed", 10, 5, 40);
                    this.toggleSprint = builder.comment("Allows usage of sprinting with toggle").define("toggleSprint", false);

                    this.aimAssist = builder.comment("Disables or enables aim assist").define("aimAssist", true);
                    this.aimAssistIntensity = builder.comment("Changes how intense aim assist's control is").defineInRange("aimAssistIntensity", 90, 0, 100);

                    this.hostileAimMode = builder.comment("Aim mode for hostile mobs").defineEnum("hostileAimMode", AimAssistMode.BOTH);
                    this.animalAimMode = builder.comment("Aim mode for animals").defineEnum("animalAimMode", AimAssistMode.AIM);
                    this.playerAimMode = builder.comment("Aim mode for players").defineEnum("playerAimMode", AimAssistMode.AIM);

                    this.toggleIgnorePets = builder.comment("Makes aim assist ignore pets").define("toggleIgnorePets", true);
                    this.toggleIgnoreSameTeam = builder.comment("Makes aim assist ignore your teammates").define("toggleIgnoreSameTeam", false);
                    this.toggleIgnoreSameTeamFriendlyFire = builder.comment("Makes aim assist ignore your teammates if friendly fire is off").define("toggleIgnoreSameTeamFriendlyFire", true);

                }
                builder.pop();
            }
        }

        public enum AimAssistMode implements IStringSerializable
        {
            NONE("none"),
            SENSITIVITY("sensitivity"),
            AIM("aim"),
            BOTH("both");

            private final String strMode;

            AimAssistMode(String strMode)
            {
                this.strMode = strMode;
            }

            public static AimAssistMode byName(String value)
            {
                for(AimAssistMode aimAssistMode : values())
                {
                    if(aimAssistMode.strMode.equalsIgnoreCase(value))
                        return aimAssistMode;
                }
                return null;
            }

            @Override
            public String getString()
            {
                return strMode;
            }

            public boolean sensitivity()
            {
                return this == SENSITIVITY || this == BOTH;
            }

            public boolean aim()
            {
                return this == AIM || this == BOTH;
            }

            public boolean on() {
                return aim() || sensitivity();
            }
        }
    }

    static
    {
        final Pair<Client, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Config.Client::new);
        clientSpec = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    public static void save()
    {
        clientSpec.save();
    }
}
