package com.lethalfart.ChudWare.ui;

import com.lethalfart.ChudWare.module.Module;
import com.lethalfart.ChudWare.module.ModuleManager;
import com.lethalfart.ChudWare.module.impl.ArrayListModule;
import com.lethalfart.ChudWare.ui.modern.GuiRender;
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

    private static final int C_BG        = 0xB8141B29;
    private static final int C_BG_INNER  = 0xCC1A2436;
    private static final int C_BORDER    = 0x603B5577;
    private static final int C_TEXT      = 0xFFE8F0FF;
    private static final int C_ACCENT_1  = 0xFF4A8CFF;
    private static final int C_ACCENT_3  = 0xFF00D4FF;
    private static final int C_SHADOW    = 0x66000000;

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
        int headerFontSize = ModernFontRenderer.SIZE_SMALL;
        int listFontSize = useModernList ? ModernFontRenderer.SIZE_TINY : ModernFontRenderer.SIZE_SMALL;

        int headerX = 4;
        int headerY = 4;
        if (useModernHeader)
            font.drawStringWithShadow("ChudWare", headerX, headerY, C_ACCENT_1, headerFontSize);
        else
            mc.fontRendererObj.drawStringWithShadow("ChudWare", headerX, headerY, C_ACCENT_1);

        List<Module> enabled = getEnabledModules(mc, font, useModernList, listFontSize);
        if (enabled.isEmpty())
            return;

        int paddingX = 5;
        int paddingY = 1;
        int gap = 3;
        int accentW = 3;
        int marginRight = 4;
        int y = 4;

        int fontHeight = getFontHeight(mc, font, useModernList, listFontSize);
        int rowH = fontHeight + paddingY * 2;
        int radius = Math.max(4, rowH / 2);
        int idx = 0;
        int total = enabled.size();
        for (Module module : enabled)
        {
            String name = module.getName();
            int textW = getTextWidth(mc, font, useModernList, listFontSize, name);
            int rowW = textW + paddingX * 2 + accentW + 4;
            int x = screenWidth - rowW - marginRight;
            int textX = x + rowW - paddingX - textW;

            int accent = lerpColor(C_ACCENT_1, C_ACCENT_3, total <= 1 ? 0f : (idx / (float) (total - 1)));
            int shadow = GuiRender.withAlpha(C_SHADOW, 0.65f);

            GuiRender.drawRoundedRect(x + 1, y + 1, rowW, rowH, radius, shadow);
            GuiRender.drawRoundedRect(x + 1, y + 1, accentW + 2, rowH - 2, 1, accent);
            GuiRender.drawRoundedBorder(x, y, rowW, rowH, radius, C_BORDER, C_BG);
            GuiRender.drawRoundedRect(x + 1, y + 1, rowW - 2, rowH - 2, Math.max(0, radius - 1), C_BG_INNER);

            int textY = y + ((rowH - fontHeight) / 2);
            if (useModernList)
                font.drawString(name, textX, textY, C_TEXT, listFontSize);
            else
                mc.fontRendererObj.drawString(name, textX, textY, C_TEXT);

            y += rowH + gap;
            idx++;
        }
    }

    private List<Module> getEnabledModules(Minecraft mc, ModernFontRenderer font, boolean useModern, int fontSize)
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
}
