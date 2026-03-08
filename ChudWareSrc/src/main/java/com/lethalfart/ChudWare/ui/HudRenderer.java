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
        if (!arrayListModule.isEnabled()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        renderHud(mc);
    }

    private void renderHud(Minecraft mc)
    {
        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();

        int accent    = 0xFF4A8CFF;
        int accentDim = 0xAA3C6FCC;
        int text      = 0xFFE8F0FF;

        mc.fontRendererObj.drawStringWithShadow("ChudWare", 6, 6, accent);

        List<Module> enabled = getEnabledModules();
        int y = 6;
        for (Module module : enabled)
        {
            String name = module.getName();
            int fontHeight = mc.fontRendererObj.FONT_HEIGHT;
            int rowHeight = fontHeight + 4;
            int textWidth = mc.fontRendererObj.getStringWidth(name);
            int x = screenWidth - textWidth - 10;
            int rowTop = y - 2;

            Gui.drawRect(x - 5, rowTop, x + textWidth + 5, rowTop + rowHeight, 0x704FD4FF);
            Gui.drawRect(x - 4, rowTop + 1, x + textWidth + 4, rowTop + rowHeight - 1, 0x44203B2F);
            Gui.drawRect(x + textWidth + 4, rowTop + 1, x + textWidth + 5, rowTop + rowHeight - 1, accentDim);

            int textY = rowTop + ((rowHeight - fontHeight) / 2);
            mc.fontRendererObj.drawStringWithShadow(name, x, textY, text);
            y += rowHeight + 2;
        }
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
                    cachedEnabledModules.add(module);
            }
            final Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.fontRendererObj != null)
            {
                cachedEnabledModules.sort((a, b) ->
                        mc.fontRendererObj.getStringWidth(b.getName())
                                - mc.fontRendererObj.getStringWidth(a.getName()));
            }
            lastModuleChangeTime = now;
        }
        return cachedEnabledModules;
    }
}
