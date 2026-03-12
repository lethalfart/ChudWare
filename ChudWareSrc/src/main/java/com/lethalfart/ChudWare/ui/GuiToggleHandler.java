package com.lethalfart.ChudWare.ui;

import com.lethalfart.ChudWare.config.ConfigManager;
import com.lethalfart.ChudWare.module.ModuleManager;
import com.lethalfart.ChudWare.module.impl.AutoPotHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

public class GuiToggleHandler
{
    private final ModuleManager moduleManager;
    private final ConfigManager configManager;
    private final AutoPotHandler autoPotHandler;

    public GuiToggleHandler(ModuleManager moduleManager, ConfigManager configManager, AutoPotHandler autoPotHandler)
    {
        this.moduleManager = moduleManager;
        this.configManager = configManager;
        this.autoPotHandler = autoPotHandler;
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return;
        if (!Keyboard.getEventKeyState()) return;

        int key = Keyboard.getEventKey();

        // Open GUI on RSHIFT
        if (key == Keyboard.KEY_RSHIFT)
        {
            mc.displayGuiScreen(new ModuleGuiScreen(moduleManager, configManager));
            return;
        }

        // Fire module keybinds
        for (com.lethalfart.ChudWare.module.Module mod : moduleManager.getModules())
        {
            if (mod.getKeyBind() != Keyboard.KEY_NONE && mod.getKeyBind() == key)
            {
                mod.toggle();
            }
        }
    }
}
