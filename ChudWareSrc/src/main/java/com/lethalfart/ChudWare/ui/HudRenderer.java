package com.lethalfart.ChudWare.ui;

import com.lethalfart.ChudWare.module.Module;
import com.lethalfart.ChudWare.module.ModuleManager;
import com.lethalfart.ChudWare.module.impl.ArrayListModule;
import net.minecraft.client.Minecraft;
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
    private ModernFontRenderer modernFont;
    private boolean fontWarmed = false;

    private static final int C_TEXT      = 0xFFF3F7FF;
    private static final int C_TEXT_SOFT = 0x88C8D6EA;
    private static final int C_ACCENT_1  = 0xFFA9D8FF;
    private static final int C_ACCENT_2  = 0xFF8FD7C9;
    private static final int C_SHADOW    = 0x70000000;

    public HudRenderer(ModuleManager moduleManager, ArrayListModule arrayListModule)
    {
        this.moduleManager = moduleManager;
        this.arrayListModule = arrayListModule;
        this.cachedEnabledModules = new ArrayList<>();
        this.lastModuleChangeTime = System.currentTimeMillis();
        this.modernFont = new ModernFontRenderer(Minecraft.getMinecraft());
    }

    private void prewarmFont()
    {
        if (modernFont == null || !modernFont.isReady()) return;
        modernFont.getStringWidth("A", ModernFontRenderer.SIZE_TINY);
        modernFont.getStringWidth("A", ModernFontRenderer.SIZE_SMALL);
        modernFont.getStringWidth("A", ModernFontRenderer.SIZE_NORMAL);
        modernFont.getStringWidth("A", ModernFontRenderer.SIZE_MEDIUM);
        modernFont.getStringWidth("A", ModernFontRenderer.SIZE_LARGE);
        modernFont.getStringWidth("A", ModernFontRenderer.SIZE_HEADER);
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Text event)
    {
        if (!arrayListModule.isEnabled()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (!fontWarmed)
        {
            prewarmFont();
            fontWarmed = true;
            return;
        }

        renderHud(mc);
    }

    private void renderHud(Minecraft mc)
    {
        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();

        ModernFontRenderer font = modernFont;
        boolean useModernHeader = font != null && font.isReady();
        boolean useModernList = useModernHeader;
        int headerFontSize = ModernFontRenderer.SIZE_TINY;
        int listFontSize = useModernList ? ModernFontRenderer.SIZE_TINY : ModernFontRenderer.SIZE_SMALL;

        int headerX = 5;
        int headerY = 5;
        int marginRight = 5;
        int y = 4;

        int headerTextW = useModernHeader
                ? font.getStringWidth("chudware", headerFontSize)
                : mc.fontRendererObj.getStringWidth("chudware");
        if (useModernHeader)
            font.drawStringWithShadow("chudware", headerX, headerY, C_TEXT_SOFT, headerFontSize);
        else
            mc.fontRendererObj.drawStringWithShadow("chudware", headerX, headerY, C_TEXT_SOFT);

        List<Module> enabled = getEnabledModules(mc, font, useModernList, listFontSize);
        if (enabled.isEmpty())
            return;

        int gap = 1;
        int fontHeight = getFontHeight(mc, font, useModernList, listFontSize);
        y = 4;
        int idx = 0;
        int total = enabled.size();
        for (Module module : enabled)
        {
            String name = module.getName();
            int textW = getTextWidth(mc, font, useModernList, listFontSize, name);
            int textX = screenWidth - textW - marginRight;

            float t = total <= 1 ? 0f : (idx / (float) (total - 1));
            int textColor = mixText(lerpColor(C_ACCENT_1, C_ACCENT_2, t));
            int shadowColor = C_SHADOW;
            int textY = y;
            if (useModernList)
            {
                font.drawString(name, textX + 0.5f, textY + 1.0f, shadowColor, listFontSize);
                font.drawString(name, textX, textY, textColor, listFontSize);
            }
            else
            {
                mc.fontRendererObj.drawStringWithShadow(name, textX, textY, textColor);
            }

            y += fontHeight + gap;
            idx++;
        }
    }

    private List<Module> getEnabledModules(Minecraft mc, ModernFontRenderer font, boolean useModern, int fontSize)
    {
        long now = System.currentTimeMillis();
        if (now - lastModuleChangeTime > 100L)
        {
            cachedEnabledModules.clear();
            for (Module module : moduleManager.getModules())
            {
                if (module.isEnabled() && module != arrayListModule)
                    cachedEnabledModules.add(module);
            }
            cachedEnabledModules.sort((a, b) ->
                    getTextWidth(mc, font, useModern, fontSize, b.getName())
                            - getTextWidth(mc, font, useModern, fontSize, a.getName()));
            lastModuleChangeTime = now;
        }
        return cachedEnabledModules;
    }

    private int getTextWidth(Minecraft mc, ModernFontRenderer font, boolean useModern, int size, String text)
    {
        if (useModern)
            return font.getStringWidth(text, size);
        return mc.fontRendererObj.getStringWidth(text);
    }

    private int getFontHeight(Minecraft mc, ModernFontRenderer font, boolean useModern, int size)
    {
        if (useModern)
            return font.getFontHeight(size);
        return mc.fontRendererObj.FONT_HEIGHT;
    }

    private int lerpColor(int a, int b, float t)
    {
        int aA = (a >> 24) & 0xFF;
        int aR = (a >> 16) & 0xFF;
        int aG = (a >> 8) & 0xFF;
        int aB = a & 0xFF;

        int bA = (b >> 24) & 0xFF;
        int bR = (b >> 16) & 0xFF;
        int bG = (b >> 8) & 0xFF;
        int bB = b & 0xFF;

        int oA = (int) (aA + (bA - aA) * t);
        int oR = (int) (aR + (bR - aR) * t);
        int oG = (int) (aG + (bG - aG) * t);
        int oB = (int) (aB + (bB - aB) * t);

        return (oA << 24) | (oR << 16) | (oG << 8) | oB;
    }

    private int mixText(int accent)
    {
        int r = (((accent >> 16) & 0xFF) + ((C_TEXT_SOFT >> 16) & 0xFF) * 2 + ((C_TEXT >> 16) & 0xFF) * 2) / 5;
        int g = (((accent >> 8) & 0xFF) + ((C_TEXT_SOFT >> 8) & 0xFF) * 2 + ((C_TEXT >> 8) & 0xFF) * 2) / 5;
        int b = ((accent & 0xFF) + (C_TEXT_SOFT & 0xFF) * 2 + (C_TEXT & 0xFF) * 2) / 5;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
