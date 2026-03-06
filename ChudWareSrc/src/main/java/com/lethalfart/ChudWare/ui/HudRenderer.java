package com.lethalfart.ChudWare.ui;

import com.lethalfart.ChudWare.module.Module;
import com.lethalfart.ChudWare.module.ModuleManager;
import com.lethalfart.ChudWare.module.impl.ArrayListModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class HudRenderer
{
    private final ModuleManager moduleManager;
    private final ArrayListModule arrayListModule;
    private ModernFontRenderer modernFont;
    private List<Module> cachedEnabledModules;
    private long lastModuleChangeTime;

    public HudRenderer(ModuleManager moduleManager, ArrayListModule arrayListModule)
    {
        this.moduleManager = moduleManager;
        this.arrayListModule = arrayListModule;
        this.cachedEnabledModules = new ArrayList<>();
        this.lastModuleChangeTime = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Text event)
    {
        if (!arrayListModule.isEnabled())
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null)
        {
            return;
        }

        if (modernFont == null)
        {
            modernFont = new ModernFontRenderer(mc);
        }

        renderHud(mc);
    }

    private void renderHud(Minecraft mc)
    {
        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();

        int accent = 0xFF66FFB8;
        int accentDim = 0xAA47C78F;
        int text = 0xFFE8FFF2;

        modernFont.drawString("ChudWare", 6.0F, 6.0F, accent, true, 0.92F);

        List<Module> enabled = getEnabledModules();
        int y = 6;
        for (int i = 0; i < enabled.size(); i++)
        {
            Module module = enabled.get(i);
            String name = module.getName();
            int textWidth = modernFont.getStringWidth(name, 0.78F);
            int x = screenWidth - textWidth - 10;

            Gui.drawRect(x - 5, y - 2, x + textWidth + 5, y + 11, 0x704FD4FF);
            drawRoundedRect(x - 4, y - 1, x + textWidth + 4, y + 10, 5, 0x44203B2F);
            Gui.drawRect(x + textWidth + 4, y - 1, x + textWidth + 5, y + 10, accentDim);
            modernFont.drawString(name, x, y, text, true, 0.78F);
            y += 11;
        }
    }

    private void drawRoundedRect(int left, int top, int right, int bottom, int radius, int color)
    {
        if (right <= left || bottom <= top)
        {
            return;
        }
        Gui.drawRect(left, top, right, bottom, color);
    }

    private List<Module> getEnabledModules()
    {
        long now = System.currentTimeMillis();
        
        if (now - lastModuleChangeTime > 250L)
        {
            cachedEnabledModules.clear();
            for (Module module : moduleManager.getModules())
            {
                if (module.isEnabled() && module != arrayListModule)
                {
                    cachedEnabledModules.add(module);
                }
            }
            cachedEnabledModules.sort((a, b) -> {
                if (modernFont == null)
                {
                    modernFont = new ModernFontRenderer(Minecraft.getMinecraft());
                }
                int wA = modernFont.getStringWidth(a.getName(), 0.78F);
                int wB = modernFont.getStringWidth(b.getName(), 0.78F);
                return wB - wA;
            });
            lastModuleChangeTime = now;
        }
        return cachedEnabledModules;
    }
}
