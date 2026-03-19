package com.lethalfart.ChudWare.ui;

import com.lethalfart.ChudWare.config.ConfigManager;
import com.lethalfart.ChudWare.module.ModuleManager;
import com.lethalfart.ChudWare.module.impl.AutoPotModule;
import com.lethalfart.ChudWare.ui.modern.ModernClickGui;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

public class GuiToggleHandler
{
    private final ModuleManager moduleManager;
    private final ConfigManager configManager;

    public GuiToggleHandler(ModuleManager moduleManager, ConfigManager configManager)
    {
        this.moduleManager = moduleManager;
        this.configManager = configManager;
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return;
        if (!Keyboard.getEventKeyState()) return;

        int key = Keyboard.getEventKey();

        if (key == Keyboard.KEY_RSHIFT)
        {
            mc.displayGuiScreen(new ModernClickGui(moduleManager, configManager));
            return;
        }

        for (com.lethalfart.ChudWare.module.Module mod : moduleManager.getModules())
        {
            if (mod.getKeyBind() != Keyboard.KEY_NONE && mod.getKeyBind() == key)
            {
                mod.onKeyPress();
                if (!(mod instanceof AutoPotModule))
                {
                    mod.toggle();
                }
            }
        }
    }
}
