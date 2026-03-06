package com.lethalfart.ChudWare.ui;

import com.lethalfart.ChudWare.config.ConfigManager;
import com.lethalfart.ChudWare.module.Module;
import com.lethalfart.ChudWare.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

public class GuiToggleHandler
{
    private final ModuleManager moduleManager;
    private final ConfigManager configManager;
    private final KeyBinding openGuiKey;

    public GuiToggleHandler(ModuleManager moduleManager, ConfigManager configManager)
    {
        this.moduleManager = moduleManager;
        this.configManager = configManager;
        openGuiKey = new KeyBinding("key.ChudWare.opengui", Keyboard.KEY_RSHIFT, "key.categories.ChudWare");
        ClientRegistry.registerKeyBinding(openGuiKey);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || !mc.inGameHasFocus || !Display.isActive())
        {
            return;
        }

        if (openGuiKey.isPressed())
        {
            if (!(mc.currentScreen instanceof ModuleGuiScreen))
            {
                mc.displayGuiScreen(new ModuleGuiScreen(moduleManager, configManager));
            }
            return;
        }

        int eventKey = Keyboard.getEventKey();
        if (Keyboard.getEventKeyState() && eventKey != Keyboard.KEY_NONE)
        {
            
            for (Module module : moduleManager.getModules())
            {
                if (module.getKeyBind() == eventKey)
                {
                    module.toggle();
                    break; 
                }
            }
        }
    }
}
