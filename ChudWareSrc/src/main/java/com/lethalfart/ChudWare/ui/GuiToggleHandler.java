package com.lethalfart.ChudWare.ui;

import com.lethalfart.ChudWare.config.ConfigManager;
import com.lethalfart.ChudWare.module.Module;
import com.lethalfart.ChudWare.module.ModuleManager;
import com.lethalfart.ChudWare.module.impl.AutoPotHandler;
import com.lethalfart.ChudWare.module.impl.AutoPotModule;
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
    private final AutoPotHandler autoPotHandler;
    private final KeyBinding openGuiKey;

    public GuiToggleHandler(ModuleManager moduleManager, ConfigManager configManager)
    {
        this(moduleManager, configManager, null);
    }

    public GuiToggleHandler(ModuleManager moduleManager, ConfigManager configManager, AutoPotHandler autoPotHandler)
    {
        this.moduleManager = moduleManager;
        this.configManager = configManager;
        this.autoPotHandler = autoPotHandler;
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
                    if (module instanceof AutoPotModule)
                    {
                        if (autoPotHandler != null && !isVanillaGuiOpenKey(mc, eventKey))
                        {
                            autoPotHandler.onKeyPress();
                        }
                    }
                    else
                    {
                        module.toggle();
                    }
                    break;
                }
            }
        }
    }

    private boolean isVanillaGuiOpenKey(Minecraft mc, int eventKey)
    {
        if (eventKey == Keyboard.KEY_ESCAPE)
        {
            return true;
        }

        if (mc == null || mc.gameSettings == null)
        {
            return false;
        }

        return eventKey == mc.gameSettings.keyBindInventory.getKeyCode()
                || eventKey == mc.gameSettings.keyBindChat.getKeyCode()
                || eventKey == mc.gameSettings.keyBindCommand.getKeyCode();
    }
}
