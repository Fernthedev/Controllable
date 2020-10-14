package com.mrcrayfish.controllable.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mrcrayfish.controllable.Controllable;
import com.mrcrayfish.controllable.Reference;
import com.mrcrayfish.controllable.client.gui.ControllerLayoutScreen;
import com.mrcrayfish.controllable.client.settings.ControllerOptions;
import com.mrcrayfish.controllable.event.ControllerEvent;
import com.mrcrayfish.controllable.registry.ControllableButtons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.client.util.MouseSmoother;
import net.minecraft.client.util.NativeUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.controller.LookController;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.PhantomEntity;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.entity.passive.AmbientEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemGroup;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.libsdl.SDL.SDL_CONTROLLER_BUTTON_DPAD_DOWN;
import static org.libsdl.SDL.SDL_CONTROLLER_BUTTON_DPAD_UP;

/**
 * Author: MrCrayfish
 */
@OnlyIn(Dist.CLIENT)
public class ControllerInput
{
    private static final ResourceLocation CURSOR_TEXTURE = new ResourceLocation(Reference.MOD_ID, "textures/gui/cursor.png");

    private int lastUse = 0;
    private boolean keyboardSneaking = false;
    private boolean keyboardSprinting = false;

    private boolean sneaking = false;

    private boolean sprinting = false;

    private final MouseSmoother xSmoother = new MouseSmoother();
    private final MouseSmoother ySmoother = new MouseSmoother();

    private boolean isFlying = false;
    private boolean nearSlot = false;
    private double virtualMouseX;
    private double virtualMouseY;
    private float prevXAxis;
    private float prevYAxis;
    private int prevTargetMouseX;
    private int prevTargetMouseY;
    private int targetMouseX;
    private int targetMouseY;
    private double mouseSpeedX;
    private double mouseSpeedY;
    private boolean moved;
    private float targetPitch;
    private float targetYaw;

    private boolean drawVirtualCursor = true;

    private Entity aimAssistTarget;
    private boolean aimAssistIgnore = true; //If true, aim assist will not aim at an entity until it becomes false. True when mouse is moved, false when controller is moved. Mouse will always override controller state.
    private double rawMouseX;
    private double rawMouseY;

    private int currentAttackTimer;

    private int dropCounter = -1;
    private boolean mouseMoved;
    private boolean controllerInput;
    private double lastLookTime = Double.MIN_VALUE;

    public double getVirtualMouseX()
    {
        return this.virtualMouseX;
    }

    public double getVirtualMouseY()
    {
        return this.virtualMouseY;
    }

    public int getLastUse()
    {
        return this.lastUse;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if(event.phase == TickEvent.Phase.START)
        {
            this.prevTargetMouseX = this.targetMouseX;
            this.prevTargetMouseY = this.targetMouseY;

            if(this.lastUse > 0)
            {
                this.lastUse--;
            }

            Controller controller = Controllable.getController();
            if(controller == null)
                return;

            if(Math.abs(controller.getLTriggerValue()) >= 0.1F || Math.abs(controller.getRTriggerValue()) >= 0.1F)
            {
                this.lastUse = 100;
            }

            Minecraft mc = Minecraft.getInstance();
            if(mc.mouseHelper.isMouseGrabbed())
                return;

            if(mc.currentScreen == null || mc.currentScreen instanceof ControllerLayoutScreen)
                return;

            float deadZone = (float) Controllable.getOptions().getDeadZone();

            /* Only need to run code if left thumb stick has input */
            boolean moving = Math.abs(controller.getLThumbStickXValue()) >= deadZone || Math.abs(controller.getLThumbStickYValue()) >= deadZone;
            if(moving)
            {
                /* Updates the target mouse position when the initial thumb stick movement is
                 * detected. This fixes an issue when the user moves the cursor with the mouse then
                 * switching back to controller, the cursor would jump to old target mouse position. */
                if(Math.abs(this.prevXAxis) < deadZone && Math.abs(this.prevYAxis) < deadZone)
                {
                    double mouseX = mc.mouseHelper.getMouseX();
                    double mouseY = mc.mouseHelper.getMouseY();
                    if(Controllable.getController() != null && Controllable.getOptions().isVirtualMouse())
                    {
//                        mouseX = virtualMouseX;
//                        mouseY = virtualMouseY;
                        drawVirtualCursor = true;
                    }
                    this.prevTargetMouseX = this.targetMouseX = (int) mouseX;
                    this.prevTargetMouseY = this.targetMouseY = (int) mouseY;
                }

                float xAxis = (controller.getLThumbStickXValue() > 0.0F ? 1 : -1) * Math.abs(controller.getLThumbStickXValue());
                if(Math.abs(xAxis) >= deadZone)
                {
                    this.mouseSpeedX = xAxis;
                }
                else
                {
                    this.mouseSpeedX = 0.0F;
                }

                float yAxis = (controller.getLThumbStickYValue() > 0.0F ? 1 : -1) * Math.abs(controller.getLThumbStickYValue());
                if(Math.abs(yAxis) >= deadZone)
                {
                    this.mouseSpeedY = yAxis;
                }
                else
                {
                    this.mouseSpeedY = 0.0F;
                }
            }

            if(Math.abs(this.mouseSpeedX) > 0.05F || Math.abs(this.mouseSpeedY) > 0.05F)
            {
                double mouseSpeed = Controllable.getOptions().getMouseSpeed() * mc.getMainWindow().getGuiScaleFactor();
                this.targetMouseX += mouseSpeed * this.mouseSpeedX;
                this.targetMouseX = MathHelper.clamp(this.targetMouseX, 0, mc.getMainWindow().getWidth());
                this.targetMouseY += mouseSpeed * this.mouseSpeedY;
                this.targetMouseY = MathHelper.clamp(this.targetMouseY, 0, mc.getMainWindow().getHeight());
                this.lastUse = 100;
                this.moved = true;
            }

            this.prevXAxis = controller.getLThumbStickXValue();
            this.prevYAxis = controller.getLThumbStickYValue();

            this.moveMouseToClosestSlot(moving, mc.currentScreen);

            if(mc.currentScreen instanceof CreativeScreen)
            {
                this.handleCreativeScrolling((CreativeScreen) mc.currentScreen, controller);
            }

            if(Controllable.getController() != null && Controllable.getOptions().isVirtualMouse())
            {
                Screen screen = mc.currentScreen;
                if(screen != null && (this.targetMouseX != this.prevTargetMouseX || this.targetMouseY != this.prevTargetMouseY))
                {
                    if(mc.loadingGui == null)
                    {
                        double mouseX = this.virtualMouseX * (double) mc.getMainWindow().getScaledWidth() / (double) mc.getMainWindow().getWidth();
                        double mouseY = this.virtualMouseY * (double) mc.getMainWindow().getScaledHeight() / (double) mc.getMainWindow().getHeight();
                        Screen.wrapScreenError(() -> screen.mouseMoved(mouseX, mouseY), "mouseMoved event handler", ((IGuiEventListener) screen).getClass().getCanonicalName());
                        if(mc.mouseHelper.activeButton != -1 && mc.mouseHelper.eventTime > 0.0D)
                        {
                            double dragX = (this.targetMouseX - this.prevTargetMouseX) * (double) mc.getMainWindow().getScaledWidth() / (double) mc.getMainWindow().getWidth();
                            double dragY = (this.targetMouseY - this.prevTargetMouseY) * (double) mc.getMainWindow().getScaledHeight() / (double) mc.getMainWindow().getHeight();
                            Screen.wrapScreenError(() ->
                            {
                                if(net.minecraftforge.client.ForgeHooksClient.onGuiMouseDragPre(screen, mouseX, mouseY, mc.mouseHelper.activeButton, dragX, dragY))
                                {
                                    return;
                                }
                                if(((IGuiEventListener) screen).mouseDragged(mouseX, mouseY, mc.mouseHelper.activeButton, dragX, dragY))
                                {
                                    return;
                                }
                                net.minecraftforge.client.ForgeHooksClient.onGuiMouseDragPost(screen, mouseX, mouseY, mc.mouseHelper.activeButton, dragX, dragY);
                            }, "mouseDragged event handler", ((IGuiEventListener) screen).getClass().getCanonicalName());
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onScreenInit(GuiOpenEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        if(mc.currentScreen == null)
        {
            this.nearSlot = false;
            this.moved = false;
            this.mouseSpeedX = 0.0;
            this.mouseSpeedY = 0.0;
            this.virtualMouseX = this.targetMouseX = this.prevTargetMouseX = (int) (mc.getMainWindow().getWidth() / 2F);
            this.virtualMouseY = this.targetMouseY = this.prevTargetMouseY = (int) (mc.getMainWindow().getHeight() / 2F);
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onRenderScreen(GuiScreenEvent.DrawScreenEvent.Pre event)
    {
        /* Makes the cursor movement appear smooth between ticks. This will only run if the target
         * mouse position is different to the previous tick's position. This allows for the mouse
         * to still be used as input. */
        Minecraft mc = Minecraft.getInstance();
        if(mc.currentScreen != null && (this.targetMouseX != this.prevTargetMouseX || this.targetMouseY != this.prevTargetMouseY))
        {
            if(!(mc.currentScreen instanceof ControllerLayoutScreen))
            {
                float partialTicks = Minecraft.getInstance().getRenderPartialTicks();
                double mouseX = (this.prevTargetMouseX + (this.targetMouseX - this.prevTargetMouseX) * partialTicks + 0.5);
                double mouseY = (this.prevTargetMouseY + (this.targetMouseY - this.prevTargetMouseY) * partialTicks + 0.5);
                if(Controllable.getOptions().isVirtualMouse())
                {
                    this.virtualMouseX = mouseX;
                    this.virtualMouseY = mouseY;
                    GLFW.glfwSetCursorPos(mc.getMainWindow().getHandle(), mouseX, mouseY);
                    GLFW.glfwSetInputMode(mc.getMainWindow().getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
                }
                else
                {
                    GLFW.glfwSetCursorPos(mc.getMainWindow().getHandle(), mouseX, mouseY);
                    GLFW.glfwSetInputMode(mc.getMainWindow().getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
                }
            }
        }
        else if(
                Controllable.getController() != null &&
                Controllable.getOptions().isVirtualMouse() &&
                !(
                        Math.abs(Controllable.getController().getLThumbStickXValue()) > Controllable.getOptions().getDeadZone() * 0.8 ||
                        Math.abs(Controllable.getController().getLThumbStickYValue()) > Controllable.getOptions().getDeadZone() * 0.8
                )
                        &&
                (
                // Check if mouse move drastically.
                        Math.abs(Minecraft.getInstance().mouseHelper.getMouseX() - virtualMouseX) > 3 * Controllable.getOptions().getMouseSpeed() * 0.5 ||
                        Math.abs(Minecraft.getInstance().mouseHelper.getMouseY() - virtualMouseY) > 3 * Controllable.getOptions().getMouseSpeed() * 0.5
                )
        )
        {
            GLFW.glfwSetInputMode(Minecraft.getInstance().getMainWindow().getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            drawVirtualCursor = false;
        }

        if (Controllable.getController() == null) {
            GLFW.glfwSetInputMode(Minecraft.getInstance().getMainWindow().getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            drawVirtualCursor = false;
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onRenderScreen(GuiScreenEvent.DrawScreenEvent.Post event)
    {
        if(Controllable.getController() != null && Controllable.getOptions().isVirtualMouse() && lastUse > 0 && drawVirtualCursor)
        {
            MatrixStack matrixStack = event.getMatrixStack();
            matrixStack.push();

            CursorType type = Controllable.getOptions().getCursorType();
            Minecraft minecraft = event.getGui().getMinecraft();
            if(minecraft.player == null || (minecraft.player.inventory.getItemStack().isEmpty() || type == CursorType.CONSOLE))
            {
                double mouseX = (prevTargetMouseX + (targetMouseX - prevTargetMouseX) * Minecraft.getInstance().getRenderPartialTicks());
                double mouseY = (prevTargetMouseY + (targetMouseY - prevTargetMouseY) * Minecraft.getInstance().getRenderPartialTicks());

                matrixStack.translate(mouseX / minecraft.getMainWindow().getGuiScaleFactor(), mouseY / minecraft.getMainWindow().getGuiScaleFactor(), 500);

                //                    IRenderTypeBuffer.Impl renderTypeBuffer = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());

                //                    RenderSystem.color3f(1.0F, 1.0F, 1.0F);
                //                    RenderSystem.disableLighting();
                event.getGui().getMinecraft().getTextureManager().bindTexture(CURSOR_TEXTURE);

                if(type == CursorType.CONSOLE)
                {
                    matrixStack.scale(0.7f, 0.7f, 0.7f);
                }

                //                    matrixStack.push();
                Screen.blit(event.getMatrixStack(), -8, -8, 16, 16, nearSlot ? 16 : 0, type.ordinal() * 16, 16, 16, 32, CursorType.values().length * 16);
                //                    matrixStack.pop();
            }
            matrixStack.pop();
            //            RenderSystem.popMatrix();
        }
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event)
    {
        Controller controller = Controllable.getController();
        if(controller == null)
            return;

        if(event.phase == TickEvent.Phase.END)
            return;

        Minecraft mc = Minecraft.getInstance();
        PlayerEntity player = mc.player;
        if(player == null)
            return;

        if(mc.currentScreen == null && (this.targetYaw != 0F || this.targetPitch != 0F))
        {
            float elapsedTicks = Minecraft.getInstance().getTickLength();
            player.rotateTowards((this.targetYaw / 0.15) * elapsedTicks, (this.targetPitch / 0.15) * (Controllable.getOptions().isInvertLook() ? -1 : 1) * elapsedTicks);
            if(player.getRidingEntity() != null)
            {
                player.getRidingEntity().applyOrientationToEntity(player);
            }
        }
    }

    private Vector2f handleAimAssist(float yaw, float pitch)
    {
        Minecraft mc = Minecraft.getInstance();
        PlayerEntity player = mc.player;

        Controller controller = Controllable.getController();

        if(player == null || controller == null || mouseMoved)
        {
            return new Vector2f(pitch, yaw);
        }

        float resultPitch = pitch;
        float resultYaw = yaw;

        boolean entityInRange = mc.objectMouseOver != null && mc.objectMouseOver.getType() == RayTraceResult.Type.ENTITY; // Is true if an entity is in the crosshair range

        boolean targetBoxInCrosshair = false; // Is true if the target is in the crosshair raytrace

        // Change aim assist target if new one in sight
        if (entityInRange) {
            EntityRayTraceResult entityRayTraceResult = (EntityRayTraceResult) mc.objectMouseOver;

            if (entityRayTraceResult.getEntity() instanceof LivingEntity)
            {
                ControllerOptions.AimAssistMode mode = getMode(entityRayTraceResult.getEntity());
                if(mode != null && mode.on())
                {
                    aimAssistTarget = entityRayTraceResult.getEntity();
                    targetBoxInCrosshair = true;
                }
            }
        }

        // intensity as percent decimal
        float assistIntensity = Controllable.getOptions().getAimAssistIntensity() / 100.0f;

        // Inverted intensity
        float invertedIntensity = (float) (1.0 - assistIntensity); // 1.0 - 1.0 max intensity // 1.0 - 0 = the least intensity

        float minDistRequired = 6.5f * assistIntensity;

        if (aimAssistTarget == null || // Avoid null pointers
                !aimAssistTarget.isAlive() ||
                aimAssistTarget.getDistance(player) >= minDistRequired // A little higher than 5 to handle when it gets farther
        ) aimAssistTarget = null; // Remove target when it's out of range

        if(aimAssistTarget != null && aimAssistTarget.isAlive())
        {
            float aimAssistTargetDistance = aimAssistTarget.getDistance(player);

            ControllerOptions.AimAssistMode mode = getMode(aimAssistTarget); // Aim assist mode

            if(mode != null && mode != ControllerOptions.AimAssistMode.NONE &&
                    player.canEntityBeSeen(aimAssistTarget)) // Avoid checking entities such as drops or tnt
            {



                // Get yaw and pitch which will make player look at target
                float targetAimYaw = MathHelper.wrapDegrees(getTargetYaw(aimAssistTarget, player));
                float targetAimPitch = MathHelper.wrapDegrees(getTargetPitch(aimAssistTarget, player));

                // The result pitch and yaw which will make aim assist work as an assist rather force crosshair into player
                float calcPitch = (float) (MathHelper.wrapSubtractDegrees(player.rotationPitch, targetAimPitch) * 0.38 * assistIntensity);
                float calcYaw = (float) (MathHelper.wrapSubtractDegrees(player.rotationYaw, targetAimYaw) * 0.46 * assistIntensity);


                // Maximum distance of pitch and yaw which will trigger range
                float yawMaximumDistance = 3.2f;
                float pitchMaximumDistance = 4.8f;

                yawMaximumDistance -= yawMaximumDistance * invertedIntensity; // Adjust distance accordingly to assist intensity
                pitchMaximumDistance -= pitchMaximumDistance * invertedIntensity;  // Adjust distance accordingly to assist intensity

                float yawMaximumDistanceHigher   = yawMaximumDistance   * 1.8f;
                float pitchMaximumDistanceHigher = pitchMaximumDistance * 2.2f;

                boolean targetInRange = Math.abs(calcYaw) <= yawMaximumDistance && Math.abs(calcPitch) <= pitchMaximumDistance;
                boolean targetInRangeHigher = Math.abs(calcYaw) <= yawMaximumDistanceHigher && Math.abs(calcPitch) <= pitchMaximumDistanceHigher;


                // Lower sensitivity when in bounding box
                if(mode.sensitivity() && controllerInput && (targetInRangeHigher || targetBoxInCrosshair) && (targetBoxInCrosshair || aimAssistTargetDistance <= minDistRequired * 0.88))
                {

                    double multiplier = 0.85;
//
                    if (!targetBoxInCrosshair)
                        multiplier *= 0.09 + (
                                0.0045 *
                                        (Math.abs(MathHelper.wrapSubtractDegrees((float) (yaw * multiplier), targetAimYaw) + MathHelper.wrapSubtractDegrees((float) (targetAimPitch * multiplier), targetAimPitch))));


                    if (mode.aim() && !targetInRange) multiplier *= 1.425; // Allow the player to escape aim assist more easily


                    if (targetBoxInCrosshair) multiplier *= invertedIntensity * (mode.aim() ? 0.95 : 0.6) + 0.08;

                    resultYaw *= (float) (multiplier); // Slows the sensitivity to stop slingshotting the bounding box. It can still be slingshotted though if attempted.
                    resultPitch *= (float) (multiplier); // Slows the sensitivity to stop slingshotting the bounding box. It can still be slingshotted though if attempted.
                }


                //Check if mouse moves. This is to avoid tracking an entity with a mouse as it will be considered hacks.

                if (controllerInput && !mouseMoved) aimAssistIgnore = false;
                if (mouseMoved) aimAssistIgnore = true;

                if(mode.aim() && !aimAssistIgnore && aimAssistTargetDistance <= minDistRequired * 0.88)
                {

                    // Lower aim assist if looking at entity
                    if (targetBoxInCrosshair)
                    {
                        calcYaw *= 0.85;
                        calcPitch *= 0.9;
                    }

                    if (targetInRange)
                    {
                        calcYaw *= 0.85;
                        calcPitch *= 0.9;
                    }

                    // Only track when entity is in view
                    if (targetInRangeHigher) {

                        if (Math.abs(calcYaw) <= yawMaximumDistanceHigher)
                        {
                            resultYaw += calcYaw;
                        }

                        if (Math.abs(calcPitch) <= pitchMaximumDistanceHigher)
                        {
                            resultPitch += calcPitch;
                        }
                    }
                }
            }
        }

        return new Vector2f(resultPitch, resultYaw);
    }

    /**
     * From {@link LookController#getTargetPitch()}
     * @param target
     * @param playerEntity
     * @return
     */
    protected float getTargetPitch(Entity target, PlayerEntity playerEntity) {
        double xDiff = target.getPosX() - playerEntity.getPosX();
        double yDiff = getEyePosition(target) - playerEntity.getPosYEye();
        double zDiff = target.getPosZ() - playerEntity.getPosZ();
        double distance = MathHelper.sqrt(xDiff * xDiff + zDiff * zDiff);
        return (float) (-(MathHelper.atan2(yDiff, distance) * (double)(180F / (float)Math.PI)));
    }

    /**
     * From {@link LookController#getTargetYaw()}
     * @param target
     * @param playerEntity
     * @return
     */
    protected float getTargetYaw(Entity target, PlayerEntity playerEntity) {
        double diffX = target.getPosX() - playerEntity.getPosX();
        double diffZ = target.getPosZ() - playerEntity.getPosZ();
        return (float)(MathHelper.atan2(diffZ, diffX) * (double)(180F / (float)Math.PI)) - 90.0F;
    }

    /**
     * From {@link LookController#getEyePosition(Entity)}
     * @param entity
     * @return
     */
    private static double getEyePosition(Entity entity) {
        return entity instanceof LivingEntity ? entity.getPosYEye() : (entity.getBoundingBox().minY + entity.getBoundingBox().maxY) / 2.0D;
    }

    private ControllerOptions.AimAssistMode getMode(Entity entity)
    {


        if (Controllable.getOptions().isIgnoreSameTeam() && entity.getTeam() != null && entity.isOnSameTeam(entity)) {

            // If both are true (ignore when friendly fire is off || friendly fire is disabled)
            boolean ignore = !Controllable.getOptions().isIgnoreSameTeamFriendlyFire() || !entity.getTeam().getAllowFriendlyFire();

            if (ignore)
                return ControllerOptions.AimAssistMode.NONE;
        }

        if(entity instanceof PlayerEntity)
        {
            return Controllable.getOptions().getPlayerAimMode();
        }

        if(entity instanceof MonsterEntity || entity instanceof SlimeEntity || entity instanceof PhantomEntity)
        {
            return Controllable.getOptions().getHostileAimMode();
        }

        if(entity instanceof AnimalEntity || entity instanceof AmbientEntity || entity instanceof WaterMobEntity)
        {
            if (Controllable.getOptions().isIgnorePets() && entity instanceof TameableEntity) {
                TameableEntity tameableEntity = (TameableEntity) entity;
                if (tameableEntity.getOwnerId() == Minecraft.getInstance().player.getUniqueID()) {
                    return ControllerOptions.AimAssistMode.NONE;
                }
            }

            return Controllable.getOptions().getAnimalAimMode();
        }


        return null;
    }

    @SubscribeEvent
    public void onRender(TickEvent.ClientTickEvent event)
    {
        if(event.phase == TickEvent.Phase.END)
            return;

        this.targetYaw = 0F;
        this.targetPitch = 0F;

        Minecraft mc = Minecraft.getInstance();

        mouseMoved |= Math.abs(rawMouseX - mc.mouseHelper.getMouseX()) > 0.05 || Math.abs(rawMouseY - mc.mouseHelper.getMouseY()) > 0.05;

        rawMouseX = mc.mouseHelper.getMouseX();
        rawMouseY = mc.mouseHelper.getMouseY();

        PlayerEntity player = mc.player;
        if(player == null)
            return;

        Controller controller = Controllable.getController();
        if(controller == null)
            return;

        float deadZone = (float) Controllable.getOptions().getDeadZone();
        controllerInput = (Math.abs(controller.getRThumbStickXValue()) >= deadZone || Math.abs(controller.getRThumbStickYValue()) >= deadZone); // True if controller has been moved

        mouseMoved &= !controllerInput; // true if both are true
        controllerInput &= !mouseMoved; // true if both are true

        if(mc.currentScreen == null)
        {

            double pitchSpeed = Controllable.getOptions().getRotationSpeed();
            double yawSpeed = Controllable.getOptions().getRotationSpeed();

            /* Handles rotating the yaw of player */
            if(Math.abs(controller.getRThumbStickXValue()) >= deadZone)
            {
                this.lastUse = 100;
                double rotationSpeed = Controllable.getOptions().getRotationSpeed();
                ControllerEvent.Turn turnEvent = new ControllerEvent.Turn(controller, (float) rotationSpeed, (float) rotationSpeed * 0.75F);
                if(!MinecraftForge.EVENT_BUS.post(turnEvent))
                {
                    float deadZoneTrimX = (controller.getRThumbStickXValue() > 0 ? 1 : -1) * deadZone;
                    this.targetYaw = (turnEvent.getYawSpeed() * (controller.getRThumbStickXValue() - deadZoneTrimX) / (1.0F - deadZone)) * 0.33F;
                }
            }

            if(Math.abs(controller.getRThumbStickYValue()) >= deadZone)
            {
                this.lastUse = 100;
                double rotationSpeed = Controllable.getOptions().getRotationSpeed();
                ControllerEvent.Turn turnEvent = new ControllerEvent.Turn(controller, (float) rotationSpeed, (float) rotationSpeed * 0.75F);
                if(!MinecraftForge.EVENT_BUS.post(turnEvent))
                {
                    float deadZoneTrimY = (controller.getRThumbStickYValue() > 0 ? 1 : -1) * deadZone;
                    this.targetPitch = (turnEvent.getPitchSpeed() * (controller.getRThumbStickYValue() - deadZoneTrimY) / (1.0F - deadZone)) * 0.33F;
                }
            }

            //            if (targetYaw != 0 || targetPitch != 0)
            //            {



            if(Controllable.getOptions().isAimAssist())
            {
                Vector2f aimAssist = handleAimAssist(targetYaw, targetPitch);


                targetPitch = aimAssist.x;
                targetYaw = aimAssist.y;
            } else {
                aimAssistTarget = null;
            }

            //            }


            // Smooth camera
            if (mc.gameSettings.smoothCamera) {
                double d0 = NativeUtil.getTime();
                double d1 = d0 - lastLookTime;
                lastLookTime = d0;

                targetYaw = (float) this.xSmoother.smooth(targetYaw, d1);
                targetPitch = (float) this.ySmoother.smooth(targetPitch, d1);
            } else {
                this.xSmoother.reset();
                this.ySmoother.reset();
            }

        }

        if(mc.currentScreen == null)
        {
            if(ControllableButtons.ButtonActions.DROP_ITEM.getButton().isButtonDown())
            {
                this.lastUse = 100;
                this.dropCounter++;
            }
        }

        if(this.dropCounter > 20)
        {
            if (!mc.player.isSpectator())
            {
                mc.player.drop(true);
            }
            this.dropCounter = 0;
        }
        else if(dropCounter > 0 && !ControllableButtons.ButtonActions.DROP_ITEM.getButton().isButtonDown())
        {
            if (!mc.player.isSpectator())
            {
                mc.player.drop(false);
            }
            this.dropCounter = 0;
        }
    }

    @SubscribeEvent
    public void onInputUpdate(InputUpdateEvent event)
    {
        PlayerEntity player = Minecraft.getInstance().player;
        if(player == null)
            return;

        Controller controller = Controllable.getController();
        if(controller == null)
            return;

        Minecraft mc = Minecraft.getInstance();

        if(this.keyboardSneaking && !mc.gameSettings.keyBindSneak.isKeyDown())
        {
            this.sneaking = false;
            this.keyboardSneaking = false;
        }

        if(mc.gameSettings.keyBindSneak.isKeyDown())
        {
            this.sneaking = true;
            this.keyboardSneaking = true;
        }

        if(mc.player.abilities.isFlying || mc.player.isPassenger())
        {
            lastUse = 100;
            sneaking = mc.gameSettings.keyBindSneak.isKeyDown();
            sneaking |= ControllableButtons.ButtonActions.SNEAK.getButton().isButtonDown();
            isFlying = true;
        }
        else if(this.isFlying)
        {
            this.sneaking = false;
            this.isFlying = false;
        }

        event.getMovementInput().sneaking = this.sneaking;

        if (Controllable.getOptions().isToggleSprint()) {
            if (keyboardSprinting && !mc.gameSettings.keyBindSprint.isKeyDown()) {
                sprinting = false;
                keyboardSprinting = false;
            }

            if (mc.gameSettings.keyBindSprint.isKeyDown()) {
                sprinting = true;
                keyboardSprinting = true;
            }



            sprinting |= mc.gameSettings.keyBindSprint.isKeyDown();

            if (!mc.player.isSprinting())
                mc.player.setSprinting(sprinting);
        }

        if(mc.currentScreen == null)
        {
            if(!MinecraftForge.EVENT_BUS.post(new ControllerEvent.Move(controller)))
            {
                float deadZone = (float) Controllable.getOptions().getDeadZone();

                if(Math.abs(controller.getLThumbStickYValue()) >= deadZone)
                {
                    this.lastUse = 100;
                    int dir = controller.getLThumbStickYValue() > 0.0F ? -1 : 1;
                    event.getMovementInput().forwardKeyDown = dir > 0;
                    event.getMovementInput().backKeyDown = dir < 0;
                    event.getMovementInput().moveForward = dir * MathHelper.clamp((Math.abs(controller.getLThumbStickYValue()) - deadZone) / (1.0F - deadZone), 0.0F, 1.0F);

                    if(event.getMovementInput().sneaking)
                    {
                        event.getMovementInput().moveForward *= 0.3D;
                    }
                }

                if(Math.abs(controller.getLThumbStickXValue()) >= deadZone)
                {
                    this.lastUse = 100;
                    int dir = controller.getLThumbStickXValue() > 0.0F ? -1 : 1;
                    event.getMovementInput().rightKeyDown = dir < 0;
                    event.getMovementInput().leftKeyDown = dir > 0;
                    event.getMovementInput().moveStrafe = dir * MathHelper.clamp((Math.abs(controller.getLThumbStickXValue()) - deadZone) / (1.0F - deadZone), 0.0F, 1.0F);

                    if(event.getMovementInput().sneaking)
                    {
                        event.getMovementInput().moveStrafe *= 0.3D;
                    }
                }
            }

            if(ControllableButtons.ButtonActions.JUMP.getButton().isButtonDown())
            {
                event.getMovementInput().jump = true;
            }

            // Held down sprint
            if (ControllableButtons.ButtonActions.SPRINT.getButton().isButtonDown() && !Controllable.getOptions().isToggleSprint()) {
                player.setSprinting(true);
            }


            // Reset timer if it reaches target
            if(currentAttackTimer > Controllable.getOptions().getAttackSpeed())
                currentAttackTimer = 0;

            if(ControllableButtons.ButtonActions.USE_ITEM.getButton().isButtonDown() && mc.rightClickDelayTimer == 0 && !mc.player.isHandActive())
            {
                mc.rightClickMouse();
            }

            else if (ControllableButtons.ButtonActions.ATTACK.getButton().isButtonDown() && mc.objectMouseOver != null && mc.objectMouseOver.getType() == RayTraceResult.Type.ENTITY && currentAttackTimer == 0) {
                // This is to keep attacking while the button is held and staring at a mob
                mc.clickMouse();
                currentAttackTimer = 1;
            }

            // Keep the timer going if the first attack was registered
            // This is to avoid only increasing timer while staring at a mob.
            if (ControllableButtons.ButtonActions.ATTACK.getButton().isButtonDown() && currentAttackTimer > 0) {
                currentAttackTimer++;
            }

            // Reset timer when button is no longer held
            if (!ControllableButtons.ButtonActions.ATTACK.getButton().isButtonDown()) {
                currentAttackTimer = 0;
            }
        }
    }

    public void handleButtonInput(Controller controller, int button, boolean state)
    {
        if(Minecraft.getInstance().currentScreen instanceof ControllerLayoutScreen)
        {
            return;
        }

        this.lastUse = 100;

        ControllerEvent.ButtonInput eventInput = new ControllerEvent.ButtonInput(controller, button, state);
        if(MinecraftForge.EVENT_BUS.post(eventInput))
            return;

        button = eventInput.getModifiedButton();
        ButtonBinding.setButtonState(button, state);

        ControllerEvent.Button event = new ControllerEvent.Button(controller);
        if(MinecraftForge.EVENT_BUS.post(event))
            return;

        Minecraft mc = Minecraft.getInstance();
        if(state)
        {
            if(ControllableButtons.ButtonActions.SCREENSHOT.getButton().isButtonPressed())
            {
                ScreenShotHelper.saveScreenshot(mc.gameDir, mc.getMainWindow().getFramebufferWidth(), mc.getMainWindow().getFramebufferHeight(), mc.getFramebuffer(), (p_212449_1_) -> {
                    mc.execute(() -> {
                        mc.ingameGUI.getChatGUI().printChatMessage(p_212449_1_);
                    });
                });
            }
            else if(mc.currentScreen == null)
            {
                if(ControllableButtons.ButtonActions.SPRINT.getButton().isButtonPressed())
                {
                    if(Controllable.getOptions().isToggleSprint() && mc.player != null)
                    {
                        sprinting = !sprinting;
                    }
                }
                else if(ControllableButtons.ButtonActions.INVENTORY.getButton().isButtonPressed())
                {
                    if(mc.playerController.isRidingHorse())
                    {
                        mc.player.sendHorseInventory();
                    }
                    else
                    {
                        mc.getTutorial().openInventory();
                        mc.displayGuiScreen(new InventoryScreen(mc.player));
                    }
                }
                else if(ControllableButtons.ButtonActions.SNEAK.getButton().isButtonPressed())
                {
                    if(mc.player != null && !mc.player.abilities.isFlying && !mc.player.isPassenger())
                    {
                        this.sneaking = !this.sneaking;
                    }
                }
                else if(ControllableButtons.ButtonActions.SCROLL_RIGHT.getButton().isButtonPressed())
                {
                    if(mc.player != null)
                    {
                        mc.player.inventory.changeCurrentItem(-1);
                    }
                }
                else if(ControllableButtons.ButtonActions.SCROLL_LEFT.getButton().isButtonPressed())
                {
                    if(mc.player != null)
                    {
                        mc.player.inventory.changeCurrentItem(1);
                    }
                }
                else if(ControllableButtons.ButtonActions.SWAP_HANDS.getButton().isButtonPressed())
                {
                    if(mc.player != null && !mc.player.isSpectator() && mc.getConnection() != null)
                    {

                        mc.getConnection().sendPacket(new CPlayerDiggingPacket(CPlayerDiggingPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
                    }
                }
                else if(ControllableButtons.ButtonActions.TOGGLE_PERSPECTIVE.getButton().isButtonPressed() && mc.mouseHelper.isMouseGrabbed())
                {
                    cycleThirdPersonView();
                }
                else if(ControllableButtons.ButtonActions.PAUSE_GAME.getButton().isButtonPressed())
                {
                    if(mc.player != null)
                    {
                        mc.displayInGameMenu(false);
                    }
                }
                else if(mc.player != null)
                {
                    boolean slotPressed = false;

                    ButtonBinding[] slotButtonBindings = ControllableButtons.getSlotButtonBindings();
                    for(int i = 0; i < slotButtonBindings.length; i++)
                    {
                        ButtonBinding buttonBinding = slotButtonBindings[i];

                        if(buttonBinding.isButtonPressed())
                        {
                            mc.player.inventory.currentItem = i;
                            slotPressed = true;
                            break;
                        }
                    }

                    if(!slotPressed)
                    {
                        if(ControllableButtons.ButtonActions.OPEN_CHAT.getButton().isButtonPressed())
                        {
                            mc.displayGuiScreen(new ChatScreen(""));
                        }
                        else if(ControllableButtons.ButtonActions.OPEN_COMMAND_CHAT.getButton().isButtonPressed())
                        {
                            mc.displayGuiScreen(new ChatScreen("/"));
                        }
                        else if(ControllableButtons.ButtonActions.SMOOTH_CAMERA_TOGGLE.getButton().isButtonPressed())
                        {
                            mc.gameSettings.smoothCamera = !mc.gameSettings.smoothCamera;
                        }
                        else if(!mc.player.isHandActive())
                        {
                            if(ControllableButtons.ButtonActions.ATTACK.getButton().isButtonPressed())
                            {
                                mc.clickMouse();
                                currentAttackTimer = 1;
                            }
                            else if(ControllableButtons.ButtonActions.USE_ITEM.getButton().isButtonPressed())
                            {
                                mc.rightClickMouse();
                            }
                            else if(ControllableButtons.ButtonActions.PICK_BLOCK.getButton().isButtonPressed())
                            {
                                mc.middleClickMouse();
                            }
                        }

                        // HANDLE SLOTS WHILE NO GUI IS OPEN
                    }
                }
            }
            else
            {
                if(ControllableButtons.ButtonActions.INVENTORY.getButton().isButtonPressed())
                {
                    if(mc.player != null)
                    {
                        mc.player.closeScreen();
                    }
                }

                else if(ControllableButtons.ButtonActions.SCROLL_RIGHT.getButton().isButtonPressed())
                {
                    if(mc.currentScreen instanceof CreativeScreen)
                    {
                        scrollCreativeTabs((CreativeScreen) mc.currentScreen, 1);
                    }
                }
                else if(ControllableButtons.ButtonActions.SCROLL_LEFT.getButton().isButtonPressed())
                {
                    if(mc.currentScreen instanceof CreativeScreen)
                    {
                        scrollCreativeTabs((CreativeScreen) mc.currentScreen, -1);
                    }
                }
                else if(ControllableButtons.ButtonActions.PAUSE_GAME.getButton().isButtonPressed())
                {
                    if(mc.currentScreen instanceof IngameMenuScreen)
                    {
                        mc.displayGuiScreen(null);
                    }
                }
                else if(button == Buttons.A)
                {
                    invokeMouseClick(mc.currentScreen, 0);
                }
                else if(button == Buttons.X)
                {
                    invokeMouseClick(mc.currentScreen, 1);
                }
                else if(ControllableButtons.ButtonActions.DROP_ITEM.getButton().isButtonPressed())
                {
                    if (mc.currentScreen instanceof ContainerScreen && mc.player != null)
                    {
                        ClientPlayerEntity player = mc.player;
                        PlayerInventory inventory = player.inventory;
                        boolean isHotbar = inventory.getItemStack().isEmpty() && PlayerInventory.isHotbar(inventory.currentItem);


                        if (isHotbar)
                        {
                            player.drop(false);
                        } else {
                            mc.playerController.windowClick(mc.player.container.windowId, -999, GLFW.GLFW_MOUSE_BUTTON_LEFT, ClickType.PICKUP, player);
                        }
                    }
                }
                else if(mc.player != null)
                {
                    if(button == Buttons.B && mc.player.inventory.getItemStack().isEmpty())
                    {
                        invokeMouseClick(mc.currentScreen, 0);
                    }
                    else if(mc.currentScreen instanceof ContainerScreen)
                    {
                        ContainerScreen<?> screen = (ContainerScreen<?>) mc.currentScreen;
                        ButtonBinding[] slotButtonBindings = ControllableButtons.getSlotButtonBindings();
                        for(int i = 0; i < slotButtonBindings.length; i++)
                        {
                            ButtonBinding buttonBinding = slotButtonBindings[i];

                            if(buttonBinding.isButtonPressed() && screen.getSlotUnderMouse() != null)
                            {

                                ContainerScreenUtil.handleMouseClick(screen, screen.getSlotUnderMouse(), screen.getSlotUnderMouse().slotNumber, i, ClickType.SWAP);
                                break;

                            }
                        }
                    }
                }
            }
        }
        else
        {
            if(mc.currentScreen == null)
            {

            }
            else
            {
                if(button == Buttons.A)
                {
                    invokeMouseReleased(mc.currentScreen, 0);
                }
                else if(button == Buttons.X)
                {
                    invokeMouseReleased(mc.currentScreen, 1);
                }
            }
        }
    }

    /**
     * Cycles the third person view. Minecraft doesn't have this code in a convenient method.
     */
    private void cycleThirdPersonView()
    {
        Minecraft mc = Minecraft.getInstance();
        PointOfView pointofview = mc.gameSettings.getPointOfView(); // getPointOfView()
        mc.gameSettings.setPointOfView(pointofview.func_243194_c() /* Gets the next point of view */); // setPointOfView()
        if (pointofview.func_243192_a() != mc.gameSettings.getPointOfView().func_243192_a()) {
            mc.gameRenderer.loadEntityShader(mc.gameSettings.getPointOfView().func_243192_a() ? mc.getRenderViewEntity() : null);
        }
    }

    private void scrollCreativeTabs(CreativeScreen creative, int dir)
    {
        this.lastUse = 100;

        try
        {
            Method method = ObfuscationReflectionHelper.findMethod(CreativeScreen.class, "func_147050_b", ItemGroup.class);
            method.setAccessible(true);
            if(dir > 0)
            {
                if(creative.getSelectedTabIndex() < ItemGroup.GROUPS.length - 1)
                {
                    method.invoke(creative, ItemGroup.GROUPS[creative.getSelectedTabIndex() + 1]);
                }
            }
            else if(dir < 0)
            {
                if(creative.getSelectedTabIndex() > 0)
                {
                    method.invoke(creative, ItemGroup.GROUPS[creative.getSelectedTabIndex() - 1]);
                }
            }
        }
        catch(IllegalAccessException | InvocationTargetException e)
        {
            e.printStackTrace();
        }
    }

    private void moveMouseToClosestSlot(boolean moving, Screen screen)
    {
        this.nearSlot = false;

        /* Makes the mouse attracted to slots. This helps with selecting items when using
         * a controller. */
        if(screen instanceof ContainerScreen)
        {
            /* Prevents cursor from moving until at least some input is detected */
            if(!this.moved) return;

            Minecraft mc = Minecraft.getInstance();
            ContainerScreen guiContainer = (ContainerScreen) screen;
            int guiLeft = (guiContainer.width - guiContainer.getXSize()) / 2;
            int guiTop = (guiContainer.height - guiContainer.getYSize()) / 2;
            int mouseX = (int) (this.targetMouseX * (double) mc.getMainWindow().getScaledWidth() / (double) mc.getMainWindow().getWidth());
            int mouseY = (int) (this.targetMouseY * (double) mc.getMainWindow().getScaledHeight() / (double) mc.getMainWindow().getHeight());

            //Slot closestSlot = guiContainer.getSlotUnderMouse();

            /* Finds the closest slot in the GUI within 14 pixels (inclusive) */
            Slot closestSlot = null;
            double closestDistance = -1.0;
            for(Slot slot : guiContainer.getContainer().inventorySlots)
            {
                int posX = guiLeft + slot.xPos + 8;
                int posY = guiTop + slot.yPos + 8;

                double distance = Math.sqrt(Math.pow(posX - mouseX, 2) + Math.pow(posY - mouseY, 2));
                if((closestDistance == -1.0 || distance < closestDistance) && distance <= 14.0)
                {
                    closestSlot = slot;
                    closestDistance = distance;
                }
            }

            if(closestSlot != null && (closestSlot.getHasStack() || !mc.player.inventory.getItemStack().isEmpty()))
            {
                this.nearSlot = true;
                int slotCenterXScaled = guiLeft + closestSlot.xPos + 8;
                int slotCenterYScaled = guiTop + closestSlot.yPos + 8;
                int slotCenterX = (int) (slotCenterXScaled / ((double) mc.getMainWindow().getScaledWidth() / (double) mc.getMainWindow().getWidth()));
                int slotCenterY = (int) (slotCenterYScaled / ((double) mc.getMainWindow().getScaledHeight() / (double) mc.getMainWindow().getHeight()));
                double deltaX = slotCenterX - targetMouseX;
                double deltaY = slotCenterY - targetMouseY;

                if(!moving)
                {
                    if(mouseX != slotCenterXScaled || mouseY != slotCenterYScaled)
                    {
                        this.targetMouseX += deltaX * 0.75;
                        this.targetMouseY += deltaY * 0.75;
                    }
                    else
                    {
                        this.mouseSpeedX = 0.0F;
                        this.mouseSpeedY = 0.0F;
                    }
                }

                this.mouseSpeedX *= 0.75F;
                this.mouseSpeedY *= 0.75F;
            }
            else
            {
                this.mouseSpeedX *= 0.1F;
                this.mouseSpeedY *= 0.1F;
            }
        }
        else
        {
            this.mouseSpeedX = 0.0F;
            this.mouseSpeedY = 0.0F;
        }
    }

    private void handleCreativeScrolling(CreativeScreen creative, Controller controller)
    {
        try
        {
            int i = (creative.getContainer().itemList.size() + 9 - 1) / 9 - 5;
            int dir = 0;

            if(controller.getSDL2Controller().getButton(SDL_CONTROLLER_BUTTON_DPAD_UP) || controller.getRThumbStickYValue() <= -0.8F)
            {
                dir = 1;
            }
            else if(controller.getSDL2Controller().getButton(SDL_CONTROLLER_BUTTON_DPAD_DOWN) || controller.getRThumbStickYValue() >= 0.8F)
            {
                dir = -1;
            }

            Field field = ObfuscationReflectionHelper.findField(CreativeScreen.class, "field_147067_x");
            field.setAccessible(true);

            float currentScroll = field.getFloat(creative);
            currentScroll = (float) ((double) currentScroll - (double) dir / (double) i);
            currentScroll = MathHelper.clamp(currentScroll, 0.0F, 1.0F);
            field.setFloat(creative, currentScroll);
            creative.getContainer().scrollTo(currentScroll);
        }
        catch(IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Invokes a mouse click in a GUI. This is modified version that is designed for controllers.
     * Upon clicking, mouse released is called straight away to make sure dragging doesn't happen.
     *
     * @param screen the screen instance
     * @param button the button to click with
     */
    private void invokeMouseClick(Screen screen, int button)
    {
        Minecraft mc = Minecraft.getInstance();
        if(screen != null)
        {
            double mouseX = mc.mouseHelper.getMouseX();
            double mouseY = mc.mouseHelper.getMouseY();
            if(Controllable.getController() != null && Controllable.getOptions().isVirtualMouse() && lastUse > 0)
            {
                mouseX = virtualMouseX;
                mouseY = virtualMouseY;
            }
            mouseX = mouseX * (double) mc.getMainWindow().getScaledWidth() / (double) mc.getMainWindow().getWidth();
            mouseY = mouseY * (double) mc.getMainWindow().getScaledHeight() / (double) mc.getMainWindow().getHeight();

            mc.mouseHelper.activeButton = button;
            mc.mouseHelper.eventTime = NativeUtil.getTime();

            double finalMouseX = mouseX;
            double finalMouseY = mouseY;
            Screen.wrapScreenError(() ->
            {
                boolean cancelled = ForgeHooksClient.onGuiMouseClickedPre(screen, finalMouseX, finalMouseY, button);
                if(!cancelled)
                {
                    cancelled = screen.mouseClicked(finalMouseX, finalMouseY, button);
                }
                if(!cancelled)
                {
                    ForgeHooksClient.onGuiMouseClickedPost(screen, finalMouseX, finalMouseY, button);
                }
            }, "mouseClicked event handler", screen.getClass().getCanonicalName());
        }
    }

    /**
     * Invokes a mouse released in a GUI. This is modified version that is designed for controllers.
     * Upon clicking, mouse released is called straight away to make sure dragging doesn't happen.
     *
     * @param screen the screen instance
     * @param button the button to click with
     */
    private void invokeMouseReleased(Screen screen, int button)
    {
        Minecraft mc = Minecraft.getInstance();
        if(screen != null)
        {
            double mouseX = mc.mouseHelper.getMouseX();
            double mouseY = mc.mouseHelper.getMouseY();
            if(Controllable.getController() != null && Controllable.getOptions().isVirtualMouse() && lastUse > 0)
            {
                mouseX = this.virtualMouseX;
                mouseY = this.virtualMouseY;
            }
            mouseX = mouseX * (double) mc.getMainWindow().getScaledWidth() / (double) mc.getMainWindow().getWidth();
            mouseY = mouseY * (double) mc.getMainWindow().getScaledHeight() / (double) mc.getMainWindow().getHeight();

            mc.mouseHelper.activeButton = -1;

            double finalMouseX = mouseX;
            double finalMouseY = mouseY;
            Screen.wrapScreenError(() ->
            {
                boolean cancelled = ForgeHooksClient.onGuiMouseReleasedPre(screen, finalMouseX, finalMouseY, button);
                if(!cancelled)
                {
                    cancelled = screen.mouseReleased(finalMouseX, finalMouseY, button);
                }
                if(!cancelled)
                {
                    ForgeHooksClient.onGuiMouseReleasedPost(screen, finalMouseX, finalMouseY, button);
                }
            }, "mouseReleased event handler", screen.getClass().getCanonicalName());
        }
    }

    public boolean isSneaking()
    {
        return sneaking;
    }

    public boolean isSprinting()
    {
        return sprinting;
    }
}
