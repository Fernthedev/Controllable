package com.mrcrayfish.controllable.client.settings;

import com.mrcrayfish.controllable.Controllable;
import net.minecraft.client.AbstractOption;
import net.minecraft.client.GameSettings;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.OptionButton;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Author: MrCrayfish
 */
public class ControllableEnumOption<T extends Enum<T> & IStringSerializable> extends AbstractOption
{
    private Class<T> enumClass;
    private int ordinal = 0;
    private Function<GameSettings, T> getter;
    private BiConsumer<GameSettings, T> setter;
    private BiFunction<GameSettings, ControllableEnumOption<T>, ITextComponent> displayNameGetter;

    public ControllableEnumOption(String title, Class<T> enumClass, Function<GameSettings, T> getter, BiConsumer<GameSettings, T> setter, BiFunction<GameSettings, ControllableEnumOption<T>, ITextComponent> displayNameGetter)
    {
        super(title);
        this.enumClass = enumClass;
        this.getter = getter;
        this.setter = setter;
        this.displayNameGetter = displayNameGetter;
    }

    private void nextEnum(GameSettings options)
    {
        this.set(options, this.getEnum(++ordinal));
        Controllable.getOptions().saveOptions();
    }

    public void set(GameSettings options, T t)
    {
        this.setter.accept(options, t);
        this.ordinal = t.ordinal();
    }

    public T get(GameSettings options)
    {
        T t = this.getter.apply(options);
        this.ordinal = t.ordinal();
        return t;
    }

    @Override
    public Widget createWidget(GameSettings options, int x, int y, int width)
    {
        return new OptionButton(x, y, width, 20, this, this.getTitle(options), (button) -> {
            this.nextEnum(options);
            button.setMessage(this.getTitle(options));
        });
    }

    public ITextComponent getTitle(GameSettings options)
    {
        return this.displayNameGetter.apply(options, this);
    }

    private T getEnum(int ordinal)
    {
        T[] e = this.enumClass.getEnumConstants();
        if(ordinal >= e.length) ordinal = 0;
        return e[ordinal];
    }
}
