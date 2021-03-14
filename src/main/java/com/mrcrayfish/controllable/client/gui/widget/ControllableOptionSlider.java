package com.mrcrayfish.controllable.client.gui.widget;

import net.minecraft.client.GameSettings;
import net.minecraft.client.gui.widget.OptionSlider;
import net.minecraft.client.settings.SliderPercentageOption;

/**
 * Author: MrCrayfish
 */
public class ControllableOptionSlider extends OptionSlider
{
    private final SliderPercentageOption option;

    public ControllableOptionSlider(GameSettings settings, int x, int y, int width, int height, SliderPercentageOption option)
    {
        super(settings, x, y, width, height, option);
        this.option = option;
    }

    @Override
    // TODO: Fix when mappings done
    //    protected void applyValue()
    protected void func_230972_a_()
    {
        this.option.set(this.settings, this.option.denormalizeValue(this.sliderValue));
        Controllable.getOptions().saveOptions();
    }
}
