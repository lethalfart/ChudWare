package com.lethalfart.ChudWare.module;

import org.lwjgl.input.Keyboard;

public abstract class Module
{
    private final String name;
    private final Category category;
    private boolean enabled;
    private int keyBind = Keyboard.KEY_NONE;

    protected Module(String name, Category category)
    {
        this.name = name;
        this.category = category;
    }

    public String getName() { return name; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public int getKeyBind() { return keyBind; }

    public void setKeyBind(int keyBind)
    {
        if (keyBind < Keyboard.KEY_NONE) keyBind = Keyboard.KEY_NONE;
        this.keyBind = keyBind;
    }

    public void toggle()
    {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean enabled)
    {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) onEnable();
        else onDisable();
    }

    protected void onEnable() {}
    protected void onDisable() {}
}