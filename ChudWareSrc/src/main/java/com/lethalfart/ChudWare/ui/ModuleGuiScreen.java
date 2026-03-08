package com.lethalfart.ChudWare.ui;

import com.lethalfart.ChudWare.config.ConfigManager;
import com.lethalfart.ChudWare.module.Category;
import com.lethalfart.ChudWare.module.Module;
import com.lethalfart.ChudWare.module.ModuleManager;
import com.lethalfart.ChudWare.module.impl.*;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.*;

public class ModuleGuiScreen extends GuiScreen
{


    private static final int C_BG         = 0x14060810;
    private static final int C_PANEL      = 0xEE111520;
    private static final int C_PANEL_DARK = 0xDE0C1019;
    private static final int C_BORDER     = 0xC61E2D45;
    private static final int C_HEADER     = 0xD6101A2B;
    private static final int C_ROW        = 0xD6111C2D;
    private static final int C_ROW_HOVER  = 0xDF1A2D47;
    private static final int C_ROW_ON     = 0xCF162540;
    private static final int C_TRACK      = 0xCB0D1824;
    private static final int C_ACCENT1    = 0xFF4A8CFF;
    private static final int C_ACCENT2    = 0xFF7B5CFA;
    private static final int C_ACCENT3    = 0xFF00D4FF;
    private static final int C_TEXT       = 0xFFE8F0FF;
    private static final int C_TEXT_DIM   = 0xFF7A9AC0;
    private static final int C_TEXT_OFF   = 0xFF3D5570;
    private static final int C_TAB        = 0x770E1928;
    private static final int C_TAB_ON     = 0xAA1A3160;
    private static final int C_GREEN      = 0xFF2EE89A;
    private static final int C_RED        = 0xFFFF4D6A;


    private static final int SIDEBAR_W = 140;
    private static final int PANE_W    = 580;
    private static final int PANE_H    = 400;
    private static final int ROW_H     = 36;
    private static final int CTRL_H    = 30;
    private static final int HEADER_H  = 44;
    private static final int FOOTER_H  = 28;

    private static final int SLD_H     = 36;


    private static final int S_AC_MIN=1,S_AC_MAX=2,S_REACH=3,S_AIM_SPD=4,S_AIM_SMO=5,
            S_AIM_DST=6,S_ESP_LINE=7,S_ESP_DIST=8,S_ESP_R=9,S_ESP_G=10,S_ESP_B=11,
            S_ESP_A=12,S_CH_VR=13,S_CH_VG=14,S_CH_VB=15,S_CH_VA=16,S_CH_HR=17,
            S_CH_HG=18,S_CH_HB=19,S_CH_HA=20,S_VEL_CH=21,S_VEL_H=22,S_VEL_V=23,
            S_POT_DLY=24,S_POT_HP=25,S_POT_MHP=26,S_REACH_TICKS=27,S_CH_ANIM=28,
            S_CH_GLOW=29,S_CH_OUTLINE=30,S_AIM_FOV=31,S_AIM_DIST=32,S_AIM_SPD2=33;

    private static final ResourceLocation LOGO = new ResourceLocation("chudware", "logo.png");


    private final ModuleManager moduleManager;
    private final ConfigManager configManager;
    private AutoClickerModule autoClickerModule;
    private AutoPotModule     autoPotModule;
    private ReachModule       reachModule;
    private AimAssistModule   aimAssistModule;
    private ESPModule         espModule;
    private ChamsModule       chamsModule;
    private VelocityModule    velocityModule;
    private FullBrightModule  fullBrightModule;

    private ModernFontRenderer font;
    private Category activeCategory = Category.COMBAT;
    private final Set<String>         expanded    = new HashSet<>();
    private final List<ClickRegion>   clicks      = new ArrayList<>();
    private final List<SliderRegion>  sliders     = new ArrayList<>();
    private final Map<String, Float>  hoverAnims  = new HashMap<>();
    private final Map<Integer, Float> sliderAnims = new HashMap<>();
    private final Map<String, Float>  tabAnims    = new HashMap<>();
    private final EnumMap<Category, Integer> scrollOffsets = new EnumMap<>(Category.class);
    private int    activeSliderId = -1;
    private Module keybindTarget  = null;
    private long   openTime       = 0;
    private int contentClipX = 0;
    private int contentClipY = 0;
    private int contentClipW = 0;
    private int contentClipH = 0;
    private boolean drewVisibleContent = false;


    private final List<String> configNames     = new ArrayList<>();
    private boolean            configNameEntry = false;
    private String             configInput     = "";
    private String             configStatus    = "";
    private long               configStatusEnd = 0;
    private long               configRefreshAt = 0;
    private static final int   MAX_CFG_NAME    = 32;


    private static final int ORB_COUNT = 12;
    private final float[] orbX     = new float[ORB_COUNT];
    private final float[] orbY     = new float[ORB_COUNT];
    private final float[] orbR     = new float[ORB_COUNT];
    private final int[]   orbCol   = new int[ORB_COUNT];
    private final float[] orbVx    = new float[ORB_COUNT];
    private final float[] orbVy    = new float[ORB_COUNT];
    private final float[] orbPhase = new float[ORB_COUNT];
    private boolean orbsInit = false;

    private static final int PARTICLE_COUNT = 120;
    private final float[] px  = new float[PARTICLE_COUNT];
    private final float[] py  = new float[PARTICLE_COUNT];
    private final float[] pvx = new float[PARTICLE_COUNT];
    private final float[] pvy = new float[PARTICLE_COUNT];
    private final float[] pal = new float[PARTICLE_COUNT];
    private final float[] psz = new float[PARTICLE_COUNT];
    private boolean particlesInit = false;
    private long    lastParticleMs = 0;
    private final Random rng = new Random();

    public ModuleGuiScreen(ModuleManager mm, ConfigManager cm)
    {
        this.moduleManager = mm;
        this.configManager = cm;
    }


    @Override
    public void initGui()
    {
        autoClickerModule = moduleManager.getModule(AutoClickerModule.class);
        autoPotModule     = moduleManager.getModule(AutoPotModule.class);
        reachModule       = moduleManager.getModule(ReachModule.class);
        aimAssistModule   = moduleManager.getModule(AimAssistModule.class);
        espModule         = moduleManager.getModule(ESPModule.class);
        chamsModule       = moduleManager.getModule(ChamsModule.class);
        velocityModule    = moduleManager.getModule(VelocityModule.class);
        fullBrightModule  = moduleManager.getModule(FullBrightModule.class);
        font = new ModernFontRenderer(mc);
        clicks.clear(); sliders.clear(); hoverAnims.clear();
        scrollOffsets.clear();
        for (Category category : Category.values()) scrollOffsets.put(category, 0);
        activeSliderId = -1; keybindTarget = null;
        openTime = System.currentTimeMillis();
        configNameEntry = false; configInput = ""; configStatus = "";
        refreshConfigNames(true);
    }


    @Override
    public void drawScreen(int mx, int my, float pt)
    {
        clicks.clear(); sliders.clear();
        long  now   = System.currentTimeMillis();
        float openP = Math.min(1f, (now - openTime) / 280f);
        float ease  = easeOutCubic(openP);

        updateOrbs(now);
        updateParticles(now);


        drawRect(0, 0, width, height, C_BG);
        drawOrbs();
        drawScanLines();
        drawParticles();
        drawCursorGlow(mx, my);


        int pw    = Math.min(PANE_W, width - 40);
        int ph    = Math.min(PANE_H, height - 60);

        int panX  = (width  - pw) / 2;
        int panY  = (height - ph) / 2;
        int offY  = Math.round((1f - ease) * 40f);

        GL11.glPushMatrix();
        GL11.glTranslatef(0, offY, 0);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);


        drawGlowRect(panX - 2, panY - 2, pw + 4, ph + 4, C_ACCENT1, 0.22f * ease);


        drawRect(panX, panY, panX + pw, panY + ph, C_PANEL);


        drawRect(panX + SIDEBAR_W, panY + HEADER_H,
                panX + pw,        panY + ph - FOOTER_H, C_PANEL_DARK);
        drawPanelMotion(panX, panY, pw, ph, now);


        drawGradientRect(panX,        panY, panX + pw/2, panY + 2, C_ACCENT1, C_ACCENT2);
        drawGradientRect(panX + pw/2, panY, panX + pw,   panY + 2, C_ACCENT2, C_ACCENT1);


        drawGradientRect(panX, panY + 2, panX + pw, panY + 5, 0x22FFFFFF, 0x00FFFFFF);


        drawRect(panX + SIDEBAR_W, panY + HEADER_H,     panX + SIDEBAR_W + 1, panY + ph - FOOTER_H, C_BORDER);
        drawRect(panX,             panY + HEADER_H - 1, panX + pw,            panY + HEADER_H,       C_BORDER);
        drawRect(panX,             panY + ph - FOOTER_H,panX + pw,            panY + ph - FOOTER_H + 1, C_BORDER);

        drawHeader(panX, panY, pw);
        drawSidebar(mx, my - offY, panX, panY + HEADER_H, ph - HEADER_H - FOOTER_H);
        drawContent(mx, my - offY, panX + SIDEBAR_W + 1, panY + HEADER_H, pw - SIDEBAR_W - 1, ph - HEADER_H - FOOTER_H);
        drawFooter(panX, panY + ph - FOOTER_H, pw);

        GL11.glPopMatrix();
        super.drawScreen(mx, my, pt);
    }


    private void drawHeader(int x, int y, int w)
    {

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1f, 1f, 1f, 1f);
        mc.getTextureManager().bindTexture(LOGO);
        Gui.drawModalRectWithCustomSizedTexture(x + 12, y + (HEADER_H - 22) / 2, 0, 0, 22, 22, 22, 22);


        int titleH = font.getFontHeight(ModernFontRenderer.SIZE_LARGE);
        int subH   = font.getFontHeight(ModernFontRenderer.SIZE_SMALL);
        int blockH = titleH + 3 + subH;
        int textY  = y + (HEADER_H - blockH) / 2;
        int titleY = textY + 11;
        int subtitleY = y + 2;
        float titleX = x + 44;
        font.drawString("ChudWare", titleX - 1, titleY, 0x8A4A8CFF, ModernFontRenderer.SIZE_LARGE);
        font.drawString("ChudWare", titleX + 1, titleY, 0x8A4A8CFF, ModernFontRenderer.SIZE_LARGE);
        font.drawString("ChudWare", titleX, titleY - 1, 0x664A8CFF, ModernFontRenderer.SIZE_LARGE);
        font.drawString("ChudWare", titleX, titleY + 1, 0x664A8CFF, ModernFontRenderer.SIZE_LARGE);
        font.drawString("ChudWare", titleX, titleY, C_TEXT, ModernFontRenderer.SIZE_LARGE);
        font.drawCenteredString("billions must cheat!", x + (w / 2f), subtitleY, C_TEXT_DIM, ModernFontRenderer.SIZE_SMALL);


        String ver = "v1.0.5";
        int vw = font.getStringWidth(ver, ModernFontRenderer.SIZE_SMALL) + 16;
        int vx = x + w - vw - 12;
        int vh = 18;
        int vy = y + (HEADER_H - vh) / 2;
        drawRect(vx, vy, vx + vw, vy + vh, 0x881A3050);
        drawBorder(vx, vy, vw, vh, C_ACCENT1, 0.40f);
        int verH = font.getFontHeight(ModernFontRenderer.SIZE_SMALL);
        font.drawCenteredString(ver, vx + vw / 2f, vy + (vh - verH) / 2, C_ACCENT3, ModernFontRenderer.SIZE_SMALL);
    }


    private static final Category[] CATS      = { Category.COMBAT, Category.MOVEMENT, Category.VISUAL, Category.MISC, Category.CONFIGS };
    private static final String[]   CAT_NAMES = { "Combat", "Movement", "Visual", "Misc", "Configs" };

    private void drawSidebar(int mx, int my, int x, int y, int h)
    {
        int tabH  = 38;
        int gap   = 6;
        int tx    = x + 10;
        int tw    = SIDEBAR_W - 20;
        int startY = y + 12;

        for (int i = 0; i < CATS.length; i++)
        {
            Category cat   = CATS[i];
            String   label = CAT_NAMES[i];
            float    act   = animateKey(tabAnims, cat.name(), cat == activeCategory ? 1f : 0f, 0.22f);
            int      ty    = startY + i * (tabH + gap);
            boolean  hover = hit(mx, my, tx, ty, tw, tabH);
            float    ha    = animateKey(hoverAnims, "tab:" + cat.name(), hover ? 1f : 0f, 0.26f);

            if (act > 0.02f)
                drawGlowRect(tx - 2, ty - 2, tw + 4, tabH + 4, C_ACCENT1, 0.12f * act);

            int bg = lerpColor(C_TAB, C_TAB_ON, act);
            bg = lerpColor(bg, C_ROW_HOVER, ha * (1f - act) * 0.5f);
            drawRect(tx, ty, tx + tw, ty + tabH, bg);
            drawGradientRect(tx, ty, tx + tw, ty + 3, 0x20FFFFFF, 0x00FFFFFF);


            if (act > 0.02f)
            {
                int bh = Math.round(tabH * act);
                drawRect(tx, ty + (tabH - bh) / 2, tx + 3, ty + (tabH - bh) / 2 + bh, C_ACCENT1);
            }


            int ly = centeredTextY(ty, tabH, ModernFontRenderer.SIZE_NORMAL);
            int textCol = act > 0.4f ? C_TEXT : (hover ? C_TEXT_DIM : C_TEXT_OFF);
            font.drawString(label, tx + 14, ly, textCol, ModernFontRenderer.SIZE_NORMAL);


            if (act > 0.3f)
                drawRect(tx + tw - 10, ty + (tabH - 6) / 2, tx + tw - 4, ty + (tabH - 6) / 2 + 6, C_ACCENT1);

            final Category fc = cat;
            clicks.add(new ClickRegion(tx, ty, tw, tabH, () -> setActiveCategory(fc), null));
        }
    }


    private void drawContent(int mx, int my, int x, int y, int w, int h)
    {
        String title = categoryTitle(activeCategory);
        int    th    = font.getFontHeight(ModernFontRenderer.SIZE_MEDIUM);
        font.drawString(title, x + 12, y + 8, C_TEXT, ModernFontRenderer.SIZE_MEDIUM);

        int uw = font.getStringWidth(title, ModernFontRenderer.SIZE_MEDIUM) + 4;
        drawGradientRect(x + 12,        y + 8 + th + 2, x + 12 + uw / 2, y + 8 + th + 4, C_ACCENT1, C_ACCENT2);
        drawGradientRect(x + 12 + uw/2, y + 8 + th + 2, x + 12 + uw,    y + 8 + th + 4, C_ACCENT2, C_ACCENT1);

        int contentY = y + 8 + th + 8;
        int maxY     = y + h - 4;

        contentClipX = x + 8;
        contentClipY = contentY;
        contentClipW = Math.max(0, w - 16);
        contentClipH = Math.max(0, maxY - contentY);

        if (activeCategory == Category.CONFIGS)
        {
            drawConfigContent(mx, my, x, contentY, w, maxY);
            return;
        }

        int maxScroll = getMaxScrollForCategory(activeCategory, contentClipH);
        int scroll = getScroll(activeCategory, maxScroll);
        setScroll(activeCategory, scroll, maxScroll);
        drewVisibleContent = false;

        beginScissor(contentClipX, contentClipY, contentClipW, contentClipH);
        int cursorY = contentY - scroll;
        for (Module mod : modulesIn(activeCategory))
        {
            if (cursorY > maxY) break;
            cursorY = drawModuleRow(mx, my, mod, x, cursorY, w, contentY, maxY);
            if (cursorY > maxY) break;
        }
        endScissor();

        if (!drewVisibleContent && scroll > 0)
        {
            setScroll(activeCategory, scroll - 40, maxScroll);
        }

        if (maxScroll > 0)
        {
            drawScrollBar(x + w - 5, contentY, maxY - contentY, maxScroll, scroll);
        }
    }


    private int drawModuleRow(int mx, int my, Module mod, int x, int y, int w, int minY, int maxY)
    {
        if (y > maxY) return maxY + 1;
        int rx = x + 8; int rw = w - 16;
        boolean hover   = hit(mx, my, rx, y, rw, ROW_H);
        boolean autoPot = mod instanceof AutoPotModule;
        boolean enabled = autoPot ? false : mod.isEnabled();
        boolean open    = expanded.contains(mod.getName());
        boolean rowVisible = intersectsContentClip(y, ROW_H);

        float ha = animateKey(hoverAnims, "mod:" + mod.getName(), hover   ? 1f : 0f, 0.24f);
        float ea = animateKey(hoverAnims, "en:"  + mod.getName(), enabled ? 1f : 0f, 0.20f);

        if (rowVisible)
        {
            drewVisibleContent = true;
            int bg = lerpColor(C_ROW, C_ROW_HOVER, ha);
            bg = lerpColor(bg, C_ROW_ON, ea * 0.6f);
            if (ea > 0.1f) drawGlowRect(rx - 1, y - 1, rw + 2, ROW_H + 2, C_ACCENT1, 0.08f * ea);

            drawRect(rx, y, rx + rw, y + ROW_H, bg);
            drawGradientRect(rx, y, rx + rw, y + 2, 0x18FFFFFF, 0x00FFFFFF);


            int barColor = enabled ? lerpColor(C_TEXT_OFF, C_ACCENT1, ea) : C_TEXT_OFF;
            drawRect(rx + 5, y + 7, rx + 8, y + ROW_H - 7, barColor);

            font.drawString(mod.getName(), rx + 16, centeredTextY(y, ROW_H, ModernFontRenderer.SIZE_NORMAL),
                    lerpColor(C_TEXT_DIM, C_TEXT, ha), ModernFontRenderer.SIZE_NORMAL);


            String ss  = autoPot ? "BIND" : (enabled ? "ON" : "OFF");
            int    ph2 = 16;
            int    pw2 = font.getStringWidth(ss, ModernFontRenderer.SIZE_SMALL) + 12;
            int    px2 = rx + rw - pw2 - 6;
            int    py2 = y + (ROW_H - ph2) / 2;
            int statusBg = autoPot ? 0x8822304A : (enabled ? 0x880D2D1A : 0x881E1020);
            int statusBorder = autoPot ? C_ACCENT1 : (enabled ? C_GREEN : C_RED);
            int statusText = autoPot ? C_ACCENT3 : (enabled ? C_GREEN : C_RED);
            drawRect(px2, py2, px2 + pw2, py2 + ph2, statusBg);
            drawBorder(px2, py2, pw2, ph2, statusBorder, 0.5f);
            font.drawCenteredString(ss, px2 + pw2 / 2f, centeredTextY(py2, ph2, ModernFontRenderer.SIZE_SMALL), statusText, ModernFontRenderer.SIZE_SMALL);


            String kb = mod.getKeyBind() == Keyboard.KEY_NONE ? "" : "[" + Keyboard.getKeyName(mod.getKeyBind()) + "]";
            if (keybindTarget == mod) kb = "[?]";
            if (!kb.isEmpty())
            {
                font.drawString(kb, px2 - font.getStringWidth(kb, ModernFontRenderer.SIZE_SMALL) - 8,
                        centeredTextY(y, ROW_H, ModernFontRenderer.SIZE_SMALL), C_TEXT_OFF, ModernFontRenderer.SIZE_SMALL);
            }
        }

        final Module fm = mod;
        if (rowVisible) clicks.add(new ClickRegion(rx, y, rw, ROW_H, () -> {
            if (fm instanceof AutoPotModule) toggleExpanded(fm.getName());
            else fm.toggle();
        }, () -> toggleExpanded(fm.getName())));

        int nextY = y + ROW_H + 3;
        if (!open) return nextY;

        int sy = drawKeybind(fm, rx, nextY, rw, minY, maxY);
        if (sy < 0) return maxY + 1;
        int ey = drawModuleSettings(mx, my, mod, rx, sy, rw, minY, maxY);
        return ey < 0 ? maxY + 1 : ey;
    }


    private int drawKeybind(Module mod, int x, int y, int w, int minY, int maxY)
    {
        if (y > maxY) return -1;
        if (intersectsContentClip(y, CTRL_H))
        {
            drewVisibleContent = true;
            String label = keybindTarget == mod
                    ? "Press any key..."
                    : "Keybind: " + (mod.getKeyBind() == Keyboard.KEY_NONE ? "NONE" : Keyboard.getKeyName(mod.getKeyBind()));
            drawRect(x, y, x + w, y + CTRL_H, C_TRACK);
            font.drawString(label, x + 8, centeredTextY(y, CTRL_H, ModernFontRenderer.SIZE_SMALL), C_TEXT_DIM, ModernFontRenderer.SIZE_SMALL);
            final Module fm = mod;
            clicks.add(new ClickRegion(x, y, w, CTRL_H, () -> keybindTarget = fm, null));
        }
        return y + CTRL_H + 2;
    }


    private int drawModuleSettings(int mx, int my, Module mod, int x, int y, int w, int minY, int maxY)
    {
        if (mod instanceof AutoClickerModule)
        {
            AutoClickerModule ac = (AutoClickerModule) mod;
            y = drawSlider(x, y, w, "Min CPS", ac.getMinCps(), S_AC_MIN, 10, 200, Math.round(ac.getMinCps()*10), minY, maxY, mx, my); if (y<0) return -1;
            y = drawSlider(x, y, w, "Max CPS", ac.getMaxCps(), S_AC_MAX, 10, 200, Math.round(ac.getMaxCps()*10), minY, maxY, mx, my); if (y<0) return -1;
            y = drawToggle(x, y, w, "Weapon Only", ac.isWeaponOnly(), () -> ac.setWeaponOnly(!ac.isWeaponOnly()), minY, maxY); if (y<0) return -1;
            y = drawToggle(x, y, w, "Break Blocks", ac.isBreakBlocks(), () -> ac.setBreakBlocks(!ac.isBreakBlocks()), minY, maxY); if (y<0) return -1;
            y = drawToggle(x, y, w, "Inventory Fill", ac.isInventoryFill(), () -> ac.setInventoryFill(!ac.isInventoryFill()), minY, maxY);
        }
        else if (mod instanceof AutoPotModule)
        {
            AutoPotModule ap = (AutoPotModule) mod;
            y = drawSlider(x, y, w, "Action Delay", ap.getActionDelay()+" ms", S_POT_DLY, 20, 500, ap.getActionDelay(), minY, maxY, mx, my); if (y<0) return -1;
            y = drawAutoPotSlotGrid(x, y, w, ap, minY, maxY, mx, my); if (y<0) return -1;
        }
        else if (mod instanceof ReachModule)
        {
            ReachModule rm = (ReachModule) mod;
            y = drawSlider(x, y, w, "Distance", String.format(Locale.US, "%.1f", rm.getReachDistance()), S_REACH, 30, 65, (int) Math.round(rm.getReachDistance() * 10.0D), minY, maxY, mx, my); if (y<0) return -1;
            y = drawSlider(x, y, w, "Activate For", rm.getActivateTicks() + " ticks", S_REACH_TICKS, 1, 10, rm.getActivateTicks(), minY, maxY, mx, my);
        }
        else if (mod instanceof AimAssistModule)
        {
            AimAssistModule aim = (AimAssistModule) mod;
            y = drawSlider(x, y, w, "Speed 1", String.format(Locale.US, "%.0f", aim.getSpeed1()), S_AIM_SPD, 5, 100, (int)Math.round(aim.getSpeed1()), minY, maxY, mx, my); if (y<0) return -1;
            y = drawSlider(x, y, w, "Speed 2", String.format(Locale.US, "%.0f", aim.getSpeed2()), S_AIM_SPD2, 2, 97, (int)Math.round(aim.getSpeed2()), minY, maxY, mx, my); if (y<0) return -1;
            y = drawSlider(x, y, w, "FOV", String.format(Locale.US, "%.0f", aim.getFov()), S_AIM_FOV, 15, 360, (int)Math.round(aim.getFov()), minY, maxY, mx, my); if (y<0) return -1;
            y = drawSlider(x, y, w, "Distance", String.format(Locale.US, "%.1f", aim.getDistance()), S_AIM_DIST, 10, 100, (int)Math.round(aim.getDistance() * 10.0D), minY, maxY, mx, my); if (y<0) return -1;
            y = drawToggle(x, y, w, "Click Aim", aim.isClickAim(), () -> aim.setClickAim(!aim.isClickAim()), minY, maxY); if (y<0) return -1;
            y = drawToggle(x, y, w, "Break Blocks", aim.isBreakBlocks(), () -> aim.setBreakBlocks(!aim.isBreakBlocks()), minY, maxY); if (y<0) return -1;
            y = drawToggle(x, y, w, "Ignore Friends", aim.isIgnoreFriends(), () -> aim.setIgnoreFriends(!aim.isIgnoreFriends()), minY, maxY); if (y<0) return -1;
            y = drawToggle(x, y, w, "Weapon Only", aim.isWeaponOnly(), () -> aim.setWeaponOnly(!aim.isWeaponOnly()), minY, maxY); if (y<0) return -1;
            y = drawToggle(x, y, w, "Aim Invis", aim.isAimInvis(), () -> aim.setAimInvis(!aim.isAimInvis()), minY, maxY); if (y<0) return -1;
            y = drawToggle(x, y, w, "Blatant Mode", aim.isBlatantMode(), () -> aim.setBlatantMode(!aim.isBlatantMode()), minY, maxY); if (y<0) return -1;
            y = drawToggle(x, y, w, "Ignore Naked", aim.isIgnoreNaked(), () -> aim.setIgnoreNaked(!aim.isIgnoreNaked()), minY, maxY); if (y<0) return -1;
            y = drawToggle(x, y, w, "Middle Click Friend", aim.isMiddleClickFriends(), () -> aim.setMiddleClickFriends(!aim.isMiddleClickFriends()), minY, maxY);
        }
        else if (mod instanceof VelocityModule)
        {
            VelocityModule vel = (VelocityModule) mod;
            y = drawToggle(x, y, w, "No Water",    vel.isNoWater(),    () -> vel.setNoWater(!vel.isNoWater()),       minY, maxY); if (y<0) return -1;
            y = drawToggle(x, y, w, "No Lava",     vel.isNoLava(),     () -> vel.setNoLava(!vel.isNoLava()),         minY, maxY); if (y<0) return -1;
            y = drawToggle(x, y, w, "No Ladder",   vel.isNoLadder(),   () -> vel.setNoLadder(!vel.isNoLadder()),     minY, maxY); if (y<0) return -1;
            y = drawToggle(x, y, w, "Buffer Mode", vel.isBufferMode(), () -> vel.setBufferMode(!vel.isBufferMode()), minY, maxY); if (y<0) return -1;
            y = drawSlider(x, y, w, "Chance",      vel.getChance()+"%",                 S_VEL_CH, 10, 100, vel.getChance(),                    minY, maxY, mx, my); if (y<0) return -1;
            y = drawSlider(x, y, w, "Horizontal",  (int)vel.getHorizontalPercent()+"%", S_VEL_H,  0,  100, (int)vel.getHorizontalPercent(),    minY, maxY, mx, my); if (y<0) return -1;
            y = drawSlider(x, y, w, "Vertical",    (int)vel.getVerticalPercent()+"%",   S_VEL_V,  0,  100, (int)vel.getVerticalPercent(),      minY, maxY, mx, my);
        }
        else if (mod instanceof ESPModule)
        {
            ESPModule esp = (ESPModule) mod;
            y = drawSlider(x, y, w, "Line Width", String.valueOf(esp.getLineWidth()),   S_ESP_LINE, 1,   10,  esp.getLineWidth(),   minY, maxY, mx, my); if (y<0) return -1;
            y = drawSlider(x, y, w, "Distance",   String.valueOf(esp.getMaxDistance()), S_ESP_DIST, 4,   96,  esp.getMaxDistance(), minY, maxY, mx, my); if (y<0) return -1;
            y = drawSlider(x, y, w, "Red",         String.valueOf(esp.getRed()),   S_ESP_R, 0, 255, esp.getRed(),   minY, maxY, mx, my); if (y<0) return -1;
            y = drawSlider(x, y, w, "Green",       String.valueOf(esp.getGreen()), S_ESP_G, 0, 255, esp.getGreen(), minY, maxY, mx, my); if (y<0) return -1;
            y = drawSlider(x, y, w, "Blue",        String.valueOf(esp.getBlue()),  S_ESP_B, 0, 255, esp.getBlue(),  minY, maxY, mx, my); if (y<0) return -1;
            y = drawSlider(x, y, w, "Alpha",       String.valueOf(esp.getAlpha()), S_ESP_A, 0, 255, esp.getAlpha(), minY, maxY, mx, my); if (y<0) return -1;
            y = drawToggle(x, y, w, "Show Invisible", esp.isShowInvisible(), () -> esp.setShowInvisible(!esp.isShowInvisible()), minY, maxY);
        }
        else if (mod instanceof ChamsModule)
        {
            ChamsModule ch = (ChamsModule) mod;
            y = drawSlider(x,y,w,"Anim Speed",String.valueOf(ch.getAnimationSpeed()),S_CH_ANIM,0,100,ch.getAnimationSpeed(),minY,maxY,mx,my); if(y<0)return-1;
            y = drawSlider(x,y,w,"Glow",String.valueOf(ch.getGlowStrength()),S_CH_GLOW,0,100,ch.getGlowStrength(),minY,maxY,mx,my); if(y<0)return-1;
            y = drawSlider(x,y,w,"Outline",String.valueOf(ch.getOutlineWidth()),S_CH_OUTLINE,10,100,ch.getOutlineWidth(),minY,maxY,mx,my); if(y<0)return-1;
            y = drawSlider(x,y,w,"Vis Red",  String.valueOf(ch.getVisibleRed()),  S_CH_VR,0,255,ch.getVisibleRed(),  minY,maxY,mx,my); if(y<0)return-1;
            y = drawSlider(x,y,w,"Vis Green",String.valueOf(ch.getVisibleGreen()),S_CH_VG,0,255,ch.getVisibleGreen(),minY,maxY,mx,my); if(y<0)return-1;
            y = drawSlider(x,y,w,"Vis Blue", String.valueOf(ch.getVisibleBlue()), S_CH_VB,0,255,ch.getVisibleBlue(), minY,maxY,mx,my); if(y<0)return-1;
            y = drawSlider(x,y,w,"Vis Alpha",String.valueOf(ch.getVisibleAlpha()),S_CH_VA,0,255,ch.getVisibleAlpha(),minY,maxY,mx,my); if(y<0)return-1;
            y = drawSlider(x,y,w,"Hid Red",  String.valueOf(ch.getHiddenRed()),   S_CH_HR,0,255,ch.getHiddenRed(),  minY,maxY,mx,my); if(y<0)return-1;
            y = drawSlider(x,y,w,"Hid Green",String.valueOf(ch.getHiddenGreen()), S_CH_HG,0,255,ch.getHiddenGreen(),minY,maxY,mx,my); if(y<0)return-1;
            y = drawSlider(x,y,w,"Hid Blue", String.valueOf(ch.getHiddenBlue()),  S_CH_HB,0,255,ch.getHiddenBlue(), minY,maxY,mx,my); if(y<0)return-1;
            y = drawSlider(x,y,w,"Hid Alpha",String.valueOf(ch.getHiddenAlpha()), S_CH_HA,0,255,ch.getHiddenAlpha(),minY,maxY,mx,my);
        }
        return y;
    }


    private int drawToggle(int x, int y, int w, String label, boolean val, Runnable action, int minY, int maxY)
    {
        if (y > maxY) return -1;
        if (intersectsContentClip(y, CTRL_H))
        {
            drewVisibleContent = true;
            drawRect(x, y, x + w, y + CTRL_H, C_TRACK);
            font.drawString(label, x + 8, centeredTextY(y, CTRL_H, ModernFontRenderer.SIZE_SMALL), C_TEXT_DIM, ModernFontRenderer.SIZE_SMALL);

            int pw = 32; int ph = 14;
            int px = x + w - pw - 8;
            int py = y + (CTRL_H - ph) / 2;
            drawRect(px, py, px + pw, py + ph, val ? 0x880D3020 : 0x881A1A2A);
            drawBorder(px, py, pw, ph, val ? C_GREEN : C_TEXT_OFF, 0.5f);
            int kw = ph - 4;
            int kx = val ? px + pw - kw - 2 : px + 2;
            drawRect(kx, py + 2, kx + kw, py + ph - 2, val ? C_GREEN : C_TEXT_OFF);

            clicks.add(new ClickRegion(x, y, w, CTRL_H, action, null));
        }
        return y + CTRL_H + 2;
    }


    private int drawSlider(int x, int y, int w, String label, float dv, int id, int min, int max, int cur, int minY, int maxY, int mx, int my)
    {
        return drawSlider(x, y, w, label, String.format(Locale.US, "%.1f", dv), id, min, max, cur, minY, maxY, mx, my);
    }

    private int drawSlider(int x, int y, int w, String label, String valStr, int id, int min, int max, int cur, int minY, int maxY, int mx, int my)
    {
        if (y > maxY) return -1;
        if (intersectsContentClip(y, SLD_H))
        {
            drewVisibleContent = true;
            drawRect(x, y, x + w, y + SLD_H, C_TRACK);

            int lh     = font.getFontHeight(ModernFontRenderer.SIZE_SMALL);
            int labelY = y + 5;
            font.drawString(label, x + 8, labelY, C_TEXT_DIM, ModernFontRenderer.SIZE_SMALL);
            int vw = font.getStringWidth(valStr, ModernFontRenderer.SIZE_SMALL);
            font.drawString(valStr, x + w - vw - 8, labelY, C_TEXT, ModernFontRenderer.SIZE_SMALL);

            int trackY = labelY + lh + 5;
            int tx     = x + 8;
            int tw     = w - 16;
            int th     = 5;

            drawRect(tx, trackY, tx + tw, trackY + th, 0xFF0A1220);

            float pct   = (max == min) ? 0f : (float)(cur - min) / (float)(max - min);
            float shown = animateKey(sliderAnims, id, pct, 0.28f);
            int   fill  = Math.max(0, Math.min(tw, Math.round(tw * shown)));

            if (fill > 0)
                drawGradientRect(tx, trackY, tx + fill, trackY + th, C_ACCENT1, C_ACCENT2);


            int kx = tx + fill;
            drawRect(kx - 4, trackY - 3, kx + 4, trackY + th + 3, C_TEXT);
            drawRect(kx - 1, trackY,     kx + 1, trackY + th,     C_ACCENT3);

            boolean hover = hit(mx, my, tx - 4, y, tw + 8, SLD_H);
            if (hover) drawGlowRect(tx, trackY, tw, th, C_ACCENT1, 0.12f);

            sliders.add(new SliderRegion(id, tx, y, tw, SLD_H, min, max));
        }
        return y + SLD_H + 2;
    }

    private int drawAutoPotSlotGrid(int x, int y, int w, AutoPotModule module, int minY, int maxY, int mx, int my)
    {
        int boxAreaH = 54;
        if (y > maxY) return -1;
        if (intersectsContentClip(y, boxAreaH))
        {
            drewVisibleContent = true;
            drawRect(x, y, x + w, y + boxAreaH, C_TRACK);
            font.drawString("Hotbar Slots", x + 8, y + 5, C_TEXT_DIM, ModernFontRenderer.SIZE_SMALL);
            font.drawCenteredString("Box = hotbar slot with potion", x + (w / 2f), y + 5, C_TEXT_OFF, ModernFontRenderer.SIZE_SMALL);

            int slots = module.getHotbarSlotCount();
            int gap = 6;
            int boxW = (w - 16 - ((slots - 1) * gap)) / slots;
            int boxH = 24;
            int by = y + 22;
            int bx = x + 8;

            for (int slot = 0; slot < slots; slot++)
            {
                boolean enabled = module.isSlotEnabled(slot);
                int cellX = bx + (slot * (boxW + gap));
                int bg = enabled ? 0xAA1C3C68 : 0x66101824;
                int border = enabled ? C_ACCENT1 : C_TEXT_OFF;
                int text = enabled ? C_TEXT : C_TEXT_OFF;

                drawRect(cellX, by, cellX + boxW, by + boxH, bg);
                drawBorder(cellX, by, boxW, boxH, border, enabled ? 0.60f : 0.35f);
                font.drawCenteredString(String.valueOf(slot + 1), cellX + (boxW / 2f), centeredTextY(by, boxH, ModernFontRenderer.SIZE_SMALL), text, ModernFontRenderer.SIZE_SMALL);

                final int hotbarSlot = slot;
                clicks.add(new ClickRegion(cellX, by, boxW, boxH, () -> module.setSlotEnabled(hotbarSlot, !module.isSlotEnabled(hotbarSlot)), null));
            }
        }
        return y + boxAreaH + 2;
    }


    private void drawConfigContent(int mx, int my, int x, int y, int w, int maxY)
    {
        refreshConfigNamesIfNeeded();
        int cx = x + 10; int cw = w - 20;
        int lh = font.getFontHeight(ModernFontRenderer.SIZE_SMALL);

        font.drawString(configManager != null ? "Dir: " + configManager.getConfigDirectory().getName() : "Unavailable",
                cx, y, C_TEXT_DIM, ModernFontRenderer.SIZE_SMALL);
        y += lh + 6;

        int bw = (cw - 8) / 3;
        drawCfgBtn(cx,        y, bw, "Open Folder", mx, my, () -> { configManager.openConfigFolder(); setStatus("Opened folder"); });
        drawCfgBtn(cx+bw+4,   y, bw, "Create New",  mx, my, this::beginConfigEntry);
        drawCfgBtn(cx+bw*2+8, y, bw, "Reset",       mx, my, () -> { configManager.resetToDefaults(moduleManager); setStatus("Reset done"); refreshConfigNames(true); });
        y += 26;

        if (configNameEntry)
        {
            boolean blink = (System.currentTimeMillis() / 400) % 2 == 0;
            String shown  = configInput.isEmpty() ? "Type name..." : configInput + (blink ? "_" : "");
            int iw = cw - 124;
            drawRect(cx, y, cx + iw, y + 20, C_TRACK);
            drawBorder(cx, y, iw, 20, C_ACCENT1, 0.5f);
            font.drawString(shown, cx + 6, centeredTextY(y, 20, ModernFontRenderer.SIZE_SMALL), configInput.isEmpty() ? C_TEXT_OFF : C_TEXT, ModernFontRenderer.SIZE_SMALL);
            drawCfgBtn(cx + iw + 4,  y, 56, "Save",   mx, my, this::saveConfig);
            drawCfgBtn(cx + iw + 64, y, 56, "Cancel", mx, my, this::cancelConfigEntry);
            y += 26;
        }

        if (System.currentTimeMillis() < configStatusEnd && !configStatus.isEmpty())
        {
            font.drawString(configStatus, cx, y, C_ACCENT3, ModernFontRenderer.SIZE_SMALL);
            y += lh + 4;
        }

        font.drawString("Saved configs:", cx, y, C_TEXT_DIM, ModernFontRenderer.SIZE_SMALL);
        y += lh + 4;

        for (String name : configNames)
        {
            int rh = ROW_H - 4;
            if (y + rh > maxY) break;
            boolean hover = hit(mx, my, cx, y, cw, rh);
            float   ha    = animateKey(hoverAnims, "cfg:"+name, hover ? 1f : 0f, 0.22f);
            drawRect(cx, y, cx + cw, y + rh, lerpColor(C_ROW, C_ROW_HOVER, ha));
            font.drawString(name, cx + 8, centeredTextY(y, rh, ModernFontRenderer.SIZE_NORMAL), C_TEXT, ModernFontRenderer.SIZE_NORMAL);
            final String fn = name;
            clicks.add(new ClickRegion(cx, y, cw, rh,
                    () -> setStatus(configManager.loadConfig(fn, moduleManager) ? "Loaded: "+fn : "Failed: "+fn), null));
            y += rh + 3;
        }
    }

    private void drawCfgBtn(int x, int y, int w, String label, int mx, int my, Runnable r)
    {
        boolean hover = hit(mx, my, x, y, w, 20);
        float   ha    = animateKey(hoverAnims, "cfgbtn:"+label, hover ? 1f : 0f, 0.22f);
        drawRect(x, y, x + w, y + 20, lerpColor(0x880F1D30, 0x991A3050, ha));
        drawBorder(x, y, w, 20, C_ACCENT1, 0.25f + 0.35f * ha);
        font.drawCenteredString(label, x + w / 2f, centeredTextY(y, 20, ModernFontRenderer.SIZE_SMALL), C_TEXT, ModernFontRenderer.SIZE_SMALL);
        clicks.add(new ClickRegion(x, y, w, 20, r, null));
    }


    private void drawFooter(int x, int y, int w)
    {
        drawRect(x, y, x + w, y + FOOTER_H, 0x660C1220);
        font.drawString("Left click: toggle  |  Right click: expand  |  ESC: close",
                x + 12, centeredTextY(y, FOOTER_H, ModernFontRenderer.SIZE_SMALL), C_TEXT_OFF, ModernFontRenderer.SIZE_SMALL);
    }


    private static final int[] ORB_COLORS = {
            0x4A8CFF, 0x7B5CFA, 0x00D4FF, 0xFF4D8C,
            0x2EE89A, 0xFA5C7B, 0xFFB84A, 0x4AFFD4, 0xB84AFF
    };

    private void initOrbs()
    {
        for (int i = 0; i < ORB_COUNT; i++)
        {
            orbX[i]     = rng.nextFloat() * width;
            orbY[i]     = rng.nextFloat() * height;
            orbR[i]     = 55 + rng.nextFloat() * 85;
            orbCol[i]   = ORB_COLORS[i % ORB_COLORS.length];
            orbVx[i]    = (rng.nextFloat() - 0.5f) * 0.45f;
            orbVy[i]    = (rng.nextFloat() - 0.5f) * 0.45f;
            orbPhase[i] = rng.nextFloat() * 6.28f;
        }
        orbsInit = true;
    }

    private void updateOrbs(long now)
    {
        if (!orbsInit) { initOrbs(); return; }
        float t = now / 1000f;
        for (int i = 0; i < ORB_COUNT; i++)
        {
            orbX[i] += orbVx[i] + (float)(Math.sin(t * 0.5  + orbPhase[i]) * 0.6);
            orbY[i] += orbVy[i] + (float)(Math.cos(t * 0.37 + orbPhase[i] * 1.4) * 0.5);
            if (orbX[i] < -orbR[i])         orbX[i] = width  + orbR[i];
            if (orbX[i] > width  + orbR[i]) orbX[i] = -orbR[i];
            if (orbY[i] < -orbR[i])         orbY[i] = height + orbR[i];
            if (orbY[i] > height + orbR[i]) orbY[i] = -orbR[i];
        }
    }

    private void drawOrbs()
    {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        for (int i = 0; i < ORB_COUNT; i++)
        {
            int cx = Math.round(orbX[i]);
            int cy = Math.round(orbY[i]);
            int r  = Math.round(orbR[i]);
            int c  = orbCol[i];
            for (int s = r; s > 0; s -= 5)
            {
                float a = (1f - (float)s / r) * 0.13f;
                drawRect(cx - s, cy - s, cx + s, cy + s, (Math.round(a * 255) << 24) | c);
            }
        }
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void drawScanLines()
    {
        for (int yl = 0; yl < height; yl += 3)
            drawRect(0, yl, width, yl + 1, 0x12AADDFF);
    }

    private void spawnParticle(int i, boolean anyY)
    {
        px[i]  = rng.nextFloat() * width;
        py[i]  = anyY ? rng.nextFloat() * height : height + 4;
        pvx[i] = (rng.nextFloat() - 0.5f) * 0.55f;
        pvy[i] = -0.35f - rng.nextFloat() * 0.9f;
        pal[i] = 0.15f + rng.nextFloat() * 0.5f;
        psz[i] = 1 + rng.nextInt(2);
    }

    private void updateParticles(long now)
    {
        if (!particlesInit)
        {
            for (int i = 0; i < PARTICLE_COUNT; i++) spawnParticle(i, true);
            particlesInit  = true;
            lastParticleMs = now;
            return;
        }
        float dt = Math.min(3f, Math.max(0.1f, (now - lastParticleMs) / 16.667f));
        lastParticleMs = now;
        for (int i = 0; i < PARTICLE_COUNT; i++)
        {
            px[i] += pvx[i] * dt;
            py[i] += pvy[i] * dt;
            if (py[i] < -10 || px[i] < -20 || px[i] > width + 20)
                spawnParticle(i, false);
        }
    }

    private void drawParticles()
    {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        for (int i = 0; i < PARTICLE_COUNT; i++)
        {

            int baseCol = (i % 3 == 0) ? 0x4A8CFF : (i % 3 == 1) ? 0x00D4FF : 0x7B5CFA;
            int col = (Math.round(Math.min(1.0f, pal[i] * 1.8f) * 255) << 24) | baseCol;
            int s   = Math.round(psz[i]);
            drawRect(Math.round(px[i]), Math.round(py[i]), Math.round(px[i]) + s, Math.round(py[i]) + s, col);
        }
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void drawPanelMotion(int panX, int panY, int pw, int ph, long now)
    {
        int innerX = panX + 1;
        int innerY = panY + 1;
        int innerW = pw - 2;
        int innerH = ph - 2;
        if (innerW <= 0 || innerH <= 0) return;

        beginScissor(innerX, innerY, innerW, innerH);
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_POINT_SMOOTH);

        double tn = now / 1000.0;
        float t = (float) (tn * 1.1);
        int segStep = 6;


        for (int layer = 0; layer < 5; layer++)
        {
            float lane = (layer + 1f) / 6f;
            float phase = t * (0.95f + layer * 0.23f);
            float ampA = 8f + layer * 2.2f;
            float ampB = 4.5f + layer * 1.1f;
            float thick = 8.5f + layer * 1.4f;
            float driftFrac = (float) (((now * (0.00006f + layer * 0.000012f)) + layer * 0.19f) % 1.0);
            float baseY = innerY + innerH * (1f - driftFrac) + innerH * (lane - 0.5f) * 0.18f;

            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            for (int dx = 0; dx <= innerW; dx += segStep)
            {
                float nx = dx / (float) Math.max(1, innerW);
                float x = innerX + dx;
                float y = (float) (baseY
                        + Math.sin(nx * 9.5f + phase) * ampA
                        + Math.cos(nx * 24.0f - phase * 1.35f) * ampB);
                float slope = (float) (Math.cos(nx * 9.5f + phase) * ampA * 9.5f / Math.max(1f, innerW)
                        - Math.sin(nx * 24.0f - phase * 1.35f) * ampB * 24.0f / Math.max(1f, innerW));
                float inv = (float) (1.0 / Math.sqrt(1.0 + slope * slope));
                float nxp = -slope * inv;
                float nyp = inv;
                float fade = Math.max(0f, 1f - Math.abs(nx - 0.5f) * 1.3f);
                float pulse = (float) (Math.sin(phase * 1.9f + nx * 7.0f) * 0.5f + 0.5f);
                float rr = 0.22f + pulse * 0.12f;
                float gg = 0.46f + pulse * 0.22f;
                float bb = 0.88f + pulse * 0.10f;
                float alphaCore = (0.16f - layer * 0.018f) * fade;
                float alphaEdge = alphaCore * 0.28f;

                GL11.glColor4f(rr, gg, bb, alphaCore);
                GL11.glVertex2f(x + nxp * (thick * 0.30f), y + nyp * (thick * 0.30f));
                GL11.glColor4f(rr, gg, bb, alphaEdge);
                GL11.glVertex2f(x - nxp * thick, y - nyp * thick);
            }
            GL11.glEnd();
        }


        int starCount = 3;
        for (int s = 0; s < starCount; s++)
        {
            double cycle = 10.8 + s * 1.3;
            double active = 2.7 + s * 0.35;
            double local = (tn + s * 1.35) % cycle;
            if (local > active) continue;
            float prog = (float) (local / active);
            float hx = innerX + innerW * (0.05f + prog * 0.82f);
            float hy = innerY + innerH * (0.93f - prog * 0.82f) + (float) Math.sin(t * 1.4f + s) * 4.5f;

            GL11.glLineWidth(1.8f);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (int k = 0; k < 16; k++)
            {
                float tt = k / 15f;
                float tx = hx - tt * (36f + s * 4f);
                float ty = hy + tt * (20f + s * 2f);
                float a = (1f - tt) * 0.36f;
                GL11.glColor4f(0.62f, 0.84f, 1.0f, a);
                GL11.glVertex2f(tx, ty);
            }
            GL11.glEnd();

            GL11.glPointSize(3.2f);
            GL11.glBegin(GL11.GL_POINTS);
            GL11.glColor4f(0.88f, 0.95f, 1.0f, 0.72f);
            GL11.glVertex2f(hx, hy);
            GL11.glEnd();
        }


        int cx = innerX + innerW / 2;
        int cy = innerY + innerH / 2;
        for (int r = 80; r >= 20; r -= 10)
        {
            float pulse = (float) (Math.sin(t * 1.8f) * 0.5f + 0.5f);
            float a = (0.014f + pulse * 0.012f) * (1f - (r - 20f) / 80f);
            drawRect(cx - r, cy - r, cx + r, cy + r, (Math.round(a * 255) << 24) | 0x4A8CFF);
        }

        GL11.glDisable(GL11.GL_POINT_SMOOTH);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();

        endScissor();
    }

    private void drawCursorGlow(int mx, int my)
    {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        for (int r = 30; r >= 4; r -= 4)
        {
            float a = (1f - (float)r / 34f) * 0.11f;
            drawRect(mx - r, my - r, mx + r, my + r, (Math.round(a * 255) << 24) | 0x4A8CFF);
        }
        drawRect(mx - 18, my, mx + 19, my + 1, 0x264A8CFF);
        drawRect(mx, my - 18, mx + 1, my + 19, 0x264A8CFF);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }


    private void drawGlowRect(int x, int y, int w, int h, int color, float intensity)
    {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        int base = color & 0x00FFFFFF;
        for (int s = 7; s >= 1; s--)
        {
            float a = intensity * (1f - s / 8f);
            drawRect(x - s, y - s, x + w + s, y + h + s, (Math.round(a * 255) << 24) | base);
        }
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void drawBorder(int x, int y, int w, int h, int color, float alpha)
    {
        int col = (Math.round(alpha * 255) << 24) | (color & 0x00FFFFFF);
        drawRect(x,     y,     x + w, y + 1,     col);
        drawRect(x,     y+h-1, x + w, y + h,     col);
        drawRect(x,     y,     x + 1, y + h,     col);
        drawRect(x+w-1, y,     x + w, y + h,     col);
    }


    private float animateKey(Map<String, Float> map, String key, float target, float speed)
    {
        float cur  = map.containsKey(key) ? map.get(key) : target;
        float next = lerp(cur, target, speed);
        if (Math.abs(next - target) < 0.001f) next = target;
        map.put(key, next);
        return next;
    }

    private float animateKey(Map<Integer, Float> map, int key, float target, float speed)
    {
        float cur  = map.containsKey(key) ? map.get(key) : target;
        float next = lerp(cur, target, speed);
        if (Math.abs(next - target) < 0.001f) next = target;
        map.put(key, next);
        return next;
    }

    private float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private float easeOutCubic(float t) { float f = 1f - t; return 1f - f * f * f; }

    private int lerpColor(int from, int to, float t)
    {
        t = Math.max(0f, Math.min(1f, t));
        int fa=(from>>24)&0xFF, fr=(from>>16)&0xFF, fg=(from>>8)&0xFF, fb=from&0xFF;
        int ta=(to  >>24)&0xFF, tr=(to  >>16)&0xFF, tg=(to  >>8)&0xFF, tb=to  &0xFF;
        return (Math.round(fa+(ta-fa)*t)<<24)|(Math.round(fr+(tr-fr)*t)<<16)|(Math.round(fg+(tg-fg)*t)<<8)|Math.round(fb+(tb-fb)*t);
    }


    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException
    {
        super.mouseClicked(mx, my, btn);
        if (btn != 0 && btn != 1) return;
        for (int i = clicks.size() - 1; i >= 0; i--)
        {
            ClickRegion r = clicks.get(i);
            if (!r.hit(mx, my)) continue;
            if (btn == 0 && r.left  != null) { r.left.run();  return; }
            if (btn == 1 && r.right != null) { r.right.run(); return; }
        }
        if (btn == 0)
            for (SliderRegion s : sliders)
                if (s.hit(mx, my)) { activeSliderId = s.id; applySlider(s.id, s.val(mx)); return; }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int btn, long time)
    {
        super.mouseClickMove(mx, my, btn, time);
        if (activeSliderId == -1 || btn != 0) return;
        for (SliderRegion s : sliders)
            if (s.id == activeSliderId) { applySlider(s.id, s.val(mx)); return; }
    }

    @Override protected void mouseReleased(int mx, int my, int s) { super.mouseReleased(mx, my, s); activeSliderId = -1; }

    @Override
    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();

        if (activeCategory == Category.CONFIGS) return;

        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;

        int mx = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int my = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        if (!hit(mx, my, contentClipX, contentClipY, contentClipW, contentClipH)) return;

        int maxScroll = getMaxScrollForCategory(activeCategory, contentClipH);
        if (maxScroll <= 0) return;

        int step = wheel > 0 ? -30 : 30;
        int current = getScroll(activeCategory, maxScroll);
        setScroll(activeCategory, current + step, maxScroll);
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException
    {
        if (configNameEntry)
        {
            if (key == Keyboard.KEY_ESCAPE) { cancelConfigEntry(); return; }
            if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { saveConfig(); return; }
            if (key == Keyboard.KEY_BACK && !configInput.isEmpty()) { configInput = configInput.substring(0, configInput.length()-1); return; }
            if (isValidChar(c) && configInput.length() < MAX_CFG_NAME) { configInput += c; }
            return;
        }
        if (keybindTarget != null)
        {
            if (key == Keyboard.KEY_ESCAPE) { keybindTarget = null; return; }
            if (key == Keyboard.KEY_DELETE || key == Keyboard.KEY_BACK) { keybindTarget.setKeyBind(Keyboard.KEY_NONE); keybindTarget = null; return; }
            keybindTarget.setKeyBind(key); keybindTarget = null; return;
        }
        super.keyTyped(c, key);
    }

    @Override public boolean doesGuiPauseGame() { return false; }


    private void applySlider(int id, int v)
    {
        if (autoClickerModule != null) {
            if (id==S_AC_MIN) { autoClickerModule.setMinCps(v/10f); return; }
            if (id==S_AC_MAX) { autoClickerModule.setMaxCps(v/10f); return; }
        }
        if (autoPotModule != null) {
            if (id==S_POT_DLY) { autoPotModule.setActionDelay(v); return; }
        }
        if (reachModule != null) {
            if (id==S_REACH)       { reachModule.setReachDistance(v / 10.0D); return; }
            if (id==S_REACH_TICKS) { reachModule.setActivateTicks(v); return; }
        }
        if (aimAssistModule != null) {
            if (id==S_AIM_SPD) { aimAssistModule.setSpeed1(v); return; }
            if (id==S_AIM_SPD2) { aimAssistModule.setSpeed2(v); return; }
            if (id==S_AIM_FOV) { aimAssistModule.setFov(v); return; }
            if (id==S_AIM_DIST) { aimAssistModule.setDistance(v/10.0); return; }
        }
        if (velocityModule != null) {
            if (id==S_VEL_CH) { velocityModule.setChance(v); return; }
            if (id==S_VEL_H)  { velocityModule.setHorizontalPercent(v); return; }
            if (id==S_VEL_V)  { velocityModule.setVerticalPercent(v); return; }
        }
        if (espModule != null) {
            if (id==S_ESP_LINE) { espModule.setLineWidth(v); return; }
            if (id==S_ESP_DIST) { espModule.setMaxDistance(v); return; }
            if (id==S_ESP_R)    { espModule.setRed(v);   return; }
            if (id==S_ESP_G)    { espModule.setGreen(v); return; }
            if (id==S_ESP_B)    { espModule.setBlue(v);  return; }
            if (id==S_ESP_A)    { espModule.setAlpha(v); return; }
        }
        if (chamsModule != null) {
            if (id==S_CH_ANIM) { chamsModule.setAnimationSpeed(v); return; }
            if (id==S_CH_GLOW) { chamsModule.setGlowStrength(v); return; }
            if (id==S_CH_OUTLINE) { chamsModule.setOutlineWidth(v); return; }
            if (id==S_CH_VR) { chamsModule.setVisibleRed(v);   return; }
            if (id==S_CH_VG) { chamsModule.setVisibleGreen(v); return; }
            if (id==S_CH_VB) { chamsModule.setVisibleBlue(v);  return; }
            if (id==S_CH_VA) { chamsModule.setVisibleAlpha(v); return; }
            if (id==S_CH_HR) { chamsModule.setHiddenRed(v);    return; }
            if (id==S_CH_HG) { chamsModule.setHiddenGreen(v);  return; }
            if (id==S_CH_HB) { chamsModule.setHiddenBlue(v);   return; }
            if (id==S_CH_HA) { chamsModule.setHiddenAlpha(v);  }
        }
    }


    private int centeredTextY(int y, int h, int fontSize)
    {
        return y + (h - font.getFontHeight(fontSize)) / 2 - 1;
    }

    private boolean intersectsContentClip(int y, int h)
    {
        if (contentClipH <= 0) return false;
        return y + h >= contentClipY && y <= contentClipY + contentClipH;
    }

    private void beginScissor(int x, int y, int w, int h)
    {
        if (w <= 0 || h <= 0) return;
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        int sx = x * scale;
        int sy = mc.displayHeight - (y + h) * scale;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sx, sy, w * scale, h * scale);
    }

    private void endScissor()
    {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void drawScrollBar(int x, int y, int h, int maxScroll, int scroll)
    {
        if (h <= 0 || maxScroll <= 0) return;

        int trackW = 3;
        drawRect(x, y, x + trackW, y + h, 0x660A1320);

        int contentHeight = h + maxScroll;
        int thumbH = Math.max(24, Math.round((h * (float) h) / Math.max(1f, contentHeight)));
        int move = h - thumbH;
        int thumbY = y + Math.round(move * (scroll / (float) maxScroll));

        drawRect(x, thumbY, x + trackW, thumbY + thumbH, 0xCC4A8CFF);
        drawGlowRect(x - 1, thumbY - 1, trackW + 2, thumbH + 2, C_ACCENT1, 0.16f);
    }

    private int getScroll(Category category, int maxScroll)
    {
        int current = scrollOffsets.containsKey(category) ? scrollOffsets.get(category) : 0;
        int clamped = Math.max(0, Math.min(maxScroll, current));
        scrollOffsets.put(category, clamped);
        return clamped;
    }

    private void setScroll(Category category, int value, int maxScroll)
    {
        scrollOffsets.put(category, Math.max(0, Math.min(maxScroll, value)));
    }

    private int getMaxScrollForCategory(Category category, int viewportHeight)
    {
        if (viewportHeight <= 0) return 0;
        int total = 0;
        for (Module module : modulesIn(category))
        {
            total += estimateModuleBlockHeight(module);
        }
        return Math.max(0, total - viewportHeight + 18);
    }

    private int estimateModuleBlockHeight(Module module)
    {
        int h = ROW_H + 3;
        if (!expanded.contains(module.getName())) return h;
        h += CTRL_H + 2;
        h += estimateSettingsHeight(module);
        return h;
    }

    private int estimateSettingsHeight(Module module)
    {
        int slider = SLD_H + 2;
        int toggle = CTRL_H + 2;

        if (module instanceof AutoClickerModule) return (slider * 2) + (toggle * 3);
        if (module instanceof AutoPotModule)     return slider + 58;
        if (module instanceof ReachModule)       return slider * 2;
        if (module instanceof AimAssistModule)   return (slider * 4) + (toggle * 8);
        if (module instanceof VelocityModule)    return (slider * 3) + (toggle * 4);
        if (module instanceof ESPModule)         return (slider * 6) + toggle;
        if (module instanceof ChamsModule)       return slider * 11;
        return 0;
    }

    private List<Module> modulesIn(Category cat)
    {
        List<Module> out = new ArrayList<>();
        for (Module m : moduleManager.getModules()) if (m.getCategory() == cat) out.add(m);
        return out;
    }
    private void toggleExpanded(String n) { if (expanded.contains(n)) expanded.remove(n); else expanded.add(n); }
    private void setActiveCategory(Category c) { if (activeCategory != c) { activeCategory = c; hoverAnims.clear(); } }
    private String categoryTitle(Category c) {
        switch(c) { case COMBAT: return "Combat"; case MOVEMENT: return "Movement"; case VISUAL: return "Visual"; case CONFIGS: return "Profiles"; default: return "Misc"; }
    }
    private boolean hit(int mx, int my, int x, int y, int w, int h) { return mx>=x && mx<=x+w && my>=y && my<=y+h; }
    private void refreshConfigNamesIfNeeded() { if (System.currentTimeMillis() >= configRefreshAt) refreshConfigNames(false); }
    private void refreshConfigNames(boolean force) {
        if (!force && System.currentTimeMillis() < configRefreshAt) return;
        configNames.clear();
        if (configManager != null) configNames.addAll(configManager.listConfigNames());
        configRefreshAt = System.currentTimeMillis() + 1000L;
    }
    private void setStatus(String s) { configStatus = s; configStatusEnd = System.currentTimeMillis() + 2500L; }
    private void beginConfigEntry() { configNameEntry = true; configInput = ""; keybindTarget = null; setStatus("Type a name, press Enter"); }
    private void saveConfig() {
        String n = configInput.trim(); if (n.isEmpty()) { setStatus("Name required"); return; }
        if (configManager.saveAs(n, moduleManager)) { configNameEntry=false; configInput=""; setStatus("Saved: "+n); refreshConfigNames(true); } else setStatus("Save failed");
    }
    private void cancelConfigEntry() { configNameEntry=false; configInput=""; setStatus("Cancelled"); }
    private boolean isValidChar(char c) { return (c>='a'&&c<='z')||(c>='A'&&c<='Z')||(c>='0'&&c<='9')||c==' '||c=='-'||c=='_'; }


    private static class ClickRegion {
        int x,y,w,h; Runnable left,right;
        ClickRegion(int x,int y,int w,int h,Runnable l,Runnable r){this.x=x;this.y=y;this.w=w;this.h=h;left=l;right=r;}
        boolean hit(int mx,int my){return mx>=x&&mx<=x+w&&my>=y&&my<=y+h;}
    }
    private static class SliderRegion {
        int id,x,y,w,h,min,max;
        SliderRegion(int id,int x,int y,int w,int h,int min,int max){this.id=id;this.x=x;this.y=y;this.w=w;this.h=h;this.min=min;this.max=max;}
        boolean hit(int mx,int my){return mx>=x-4&&mx<=x+w+4&&my>=y&&my<=y+h;}
        int val(int mx){int r=Math.max(0,Math.min(w,mx-x));return min+Math.round((max-min)*(float)r/w);}
    }
}
