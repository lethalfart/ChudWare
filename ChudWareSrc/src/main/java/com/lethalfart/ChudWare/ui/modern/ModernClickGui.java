package com.lethalfart.ChudWare.ui.modern;

import com.lethalfart.ChudWare.ChudWare;
import com.lethalfart.ChudWare.config.ConfigManager;
import com.lethalfart.ChudWare.module.Category;
import com.lethalfart.ChudWare.module.Module;
import com.lethalfart.ChudWare.module.ModuleManager;
import com.lethalfart.ChudWare.module.impl.*;
import com.lethalfart.ChudWare.ui.ModernFontRenderer;
import com.lethalfart.ChudWare.ui.modern.components.ConfigPanel;
import com.lethalfart.ChudWare.ui.modern.components.ModuleCard;
import com.lethalfart.ChudWare.ui.modern.components.SettingControl;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;

public class ModernClickGui extends GuiScreen implements GuiCallbacks
{
    private final ModuleManager moduleManager;
    private final ConfigManager configManager;

    private ModernFontRenderer font;

    private final EnumMap<Category, List<ModuleCard>> cards = new EnumMap<>(Category.class);
    private final EnumMap<Category, ScrollState> scrolls = new EnumMap<>(Category.class);
    private final EnumMap<Category, GuiAnim.SmoothFloat> categoryHover = new EnumMap<>(Category.class);

    private Category activeCategory = Category.COMBAT;
    private Category previousCategory = null;

    private final GuiAnim.TimedFloat openAnim = new GuiAnim.TimedFloat(0f);
    private final GuiAnim.SmoothFloat categoryAnim = new GuiAnim.SmoothFloat(1f, 12f);
    private static final ResourceLocation LOGO = new ResourceLocation("chudware", "logo.png");

    private long lastFrame = 0L;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int sidebarX;
    private int sidebarY;
    private int sidebarW;
    private int sidebarH;
    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;

    private SettingControl.SliderControl activeSlider;
    private Module keybindTarget;
    private boolean configDirty;

    private ConfigPanel configPanel;

    public ModernClickGui(ModuleManager moduleManager, ConfigManager configManager)
    {
        this.moduleManager = moduleManager;
        this.configManager = configManager;
    }

    @Override
    public void initGui()
    {
        font = new ModernFontRenderer(mc);
        buildCards();
        configPanel = new ConfigPanel(configManager, moduleManager);
        for (Category c : Category.values())
        {
            scrolls.put(c, new ScrollState());
            categoryHover.put(c, new GuiAnim.SmoothFloat(0f, 14f));
        }
        openAnim.snap(0f);
        openAnim.animateTo(1f, 260L);
        categoryAnim.snap(1f);
        lastFrame = System.currentTimeMillis();
    }

    private void buildCards()
    {
        cards.clear();
        for (Category c : Category.values())
        {
            cards.put(c, new ArrayList<ModuleCard>());
        }
        for (Module module : moduleManager.getModules())
        {
            ModuleCard card = new ModuleCard(module);
            buildSettingsFor(card, module);
            List<ModuleCard> list = cards.get(module.getCategory());
            if (list != null)
            {
                list.add(card);
            }
        }
        for (List<ModuleCard> list : cards.values())
        {
            list.sort(Comparator.comparing(a -> a.getModule().getName().toLowerCase()));
        }
    }

    private void buildSettingsFor(ModuleCard card, Module module)
    {
        card.addSetting(new SettingControl.KeybindControl(module));

        if (module instanceof AutoClickerModule)
        {
            AutoClickerModule ac = (AutoClickerModule) module;
            card.addSetting(new SettingControl.SliderControl("Min CPS", () -> ac.getMinCps(), v -> ac.setMinCps(v), 1f, 20f, 0.1f, false));
            card.addSetting(new SettingControl.SliderControl("Max CPS", () -> ac.getMaxCps(), v -> ac.setMaxCps(v), 1f, 20f, 0.1f, false));
            card.addSetting(new SettingControl.ToggleControl("Inventory Fill", ac::isInventoryFill, ac::setInventoryFill));
            card.addSetting(new SettingControl.ToggleControl("Weapon Only", ac::isWeaponOnly, ac::setWeaponOnly));
            card.addSetting(new SettingControl.ToggleControl("Break Blocks", ac::isBreakBlocks, ac::setBreakBlocks));
        }
        else if (module instanceof AutoPotModule)
        {
            AutoPotModule ap = (AutoPotModule) module;
            card.addSetting(new SettingControl.SliderControl("Action Delay", () -> ap.getActionDelay(), v -> ap.setActionDelay(Math.round(v)), 20f, 500f, 1f, true));
            card.addSetting(new SettingControl.HotbarControl(ap));
        }
        else if (module instanceof RefillModule)
        {
            RefillModule refill = (RefillModule) module;
            card.addSetting(new SettingControl.ToggleControl("Soup", refill::isSoup, refill::setSoup));
            card.addSetting(new SettingControl.ToggleControl("Potion", refill::isPotion, refill::setPotion));
            card.addSetting(new SettingControl.ToggleControl("Use Non-Health Potions", refill::isUseNonHealthPotions, refill::setUseNonHealthPotions));
            card.addSetting(new SettingControl.CycleControl(
                    "Simulate Button",
                    new String[]{"Left", "Right", "Left + Right"},
                    refill::getSimulateButton,
                    refill::setSimulateButton));
            card.addSetting(new SettingControl.SliderControl("Delay After Open", () -> refill.getDelayAfterOpen(), v -> refill.setDelayAfterOpen(Math.round(v)), 0f, 1000f, 10f, true));
            card.addSetting(new SettingControl.SliderControl("Delay Before Close", () -> refill.getDelayBeforeClose(), v -> refill.setDelayBeforeClose(Math.round(v)), 0f, 1000f, 10f, true));
            card.addSetting(new SettingControl.SliderControl("Speed", refill::getSpeed, refill::setSpeed, 1f, 20f, 0.5f, false));
            card.addSetting(new SettingControl.ToggleControl("Smart Speed", refill::isSmartSpeed, refill::setSmartSpeed));
        }
        else if (module instanceof ReachModule)
        {
            ReachModule reach = (ReachModule) module;
            card.addSetting(new SettingControl.SliderControl("Reach Distance", () -> (float) reach.getReachDistance(), v -> reach.setReachDistance(v), 3f, 6f, 0.1f, false));
            card.addSetting(new SettingControl.SliderControl("Activate Ticks", () -> reach.getActivateTicks(), v -> reach.setActivateTicks(Math.round(v)), 1f, 20f, 1f, true));
        }
        else if (module instanceof AimAssistModule)
        {
            AimAssistModule aim = (AimAssistModule) module;
            card.addSetting(new SettingControl.SliderControl("Speed 1", () -> (float) aim.getSpeed1(), v -> aim.setSpeed1(v), 5f, 100f, 0.5f, false));
            card.addSetting(new SettingControl.SliderControl("Speed 2", () -> (float) aim.getSpeed2(), v -> aim.setSpeed2(v), 2f, 97f, 0.5f, false));
            card.addSetting(new SettingControl.SliderControl("FOV", () -> (float) aim.getFov(), v -> aim.setFov(v), 15f, 360f, 1f, true));
            card.addSetting(new SettingControl.SliderControl("Distance", () -> (float) aim.getDistance(), v -> aim.setDistance(v), 1f, 10f, 0.1f, false));
            card.addSetting(new SettingControl.ToggleControl("Click Aim", aim::isClickAim, aim::setClickAim));
            card.addSetting(new SettingControl.ToggleControl("Break Blocks", aim::isBreakBlocks, aim::setBreakBlocks));
            card.addSetting(new SettingControl.ToggleControl("Ignore Friends", aim::isIgnoreFriends, aim::setIgnoreFriends));
            card.addSetting(new SettingControl.ToggleControl("Weapon Only", aim::isWeaponOnly, aim::setWeaponOnly));
            card.addSetting(new SettingControl.ToggleControl("Aim Invisible", aim::isAimInvis, aim::setAimInvis));
            card.addSetting(new SettingControl.ToggleControl("Blatant Mode", aim::isBlatantMode, aim::setBlatantMode));
            card.addSetting(new SettingControl.ToggleControl("Ignore Naked", aim::isIgnoreNaked, aim::setIgnoreNaked));
            card.addSetting(new SettingControl.ToggleControl("Middle Click Friends", aim::isMiddleClickFriends, aim::setMiddleClickFriends));
        }
        else if (module instanceof VelocityModule)
        {
            VelocityModule vel = (VelocityModule) module;
            card.addSetting(new SettingControl.SliderControl("Chance", () -> vel.getChance(), v -> vel.setChance(Math.round(v)), 10f, 100f, 1f, true));
            card.addSetting(new SettingControl.SliderControl("Horizontal %", () -> (float) vel.getHorizontalPercent(), v -> vel.setHorizontalPercent(v), 0f, 100f, 1f, true));
            card.addSetting(new SettingControl.SliderControl("Vertical %", () -> (float) vel.getVerticalPercent(), v -> vel.setVerticalPercent(v), 0f, 100f, 1f, true));
            card.addSetting(new SettingControl.ToggleControl("No Water", vel::isNoWater, vel::setNoWater));
            card.addSetting(new SettingControl.ToggleControl("No Lava", vel::isNoLava, vel::setNoLava));
            card.addSetting(new SettingControl.ToggleControl("No Ladder", vel::isNoLadder, vel::setNoLadder));
            card.addSetting(new SettingControl.ToggleControl("Buffer Mode", vel::isBufferMode, vel::setBufferMode));
        }
        else if (module instanceof ESPModule)
        {
            ESPModule esp = (ESPModule) module;
            card.addSetting(new SettingControl.SliderControl("Line Width", () -> esp.getLineWidth(), v -> esp.setLineWidth(Math.round(v)), 1f, 10f, 1f, true));
            card.addSetting(new SettingControl.SliderControl("Max Distance", () -> esp.getMaxDistance(), v -> esp.setMaxDistance(Math.round(v)), 4f, 96f, 1f, true));
            card.addSetting(new SettingControl.SliderControl("Red", () -> esp.getRed(), v -> esp.setRed(Math.round(v)), 0f, 255f, 1f, true));
            card.addSetting(new SettingControl.SliderControl("Green", () -> esp.getGreen(), v -> esp.setGreen(Math.round(v)), 0f, 255f, 1f, true));
            card.addSetting(new SettingControl.SliderControl("Blue", () -> esp.getBlue(), v -> esp.setBlue(Math.round(v)), 0f, 255f, 1f, true));
            card.addSetting(new SettingControl.SliderControl("Alpha", () -> esp.getAlpha(), v -> esp.setAlpha(Math.round(v)), 0f, 255f, 1f, true));
            card.addSetting(new SettingControl.ToggleControl("Show Invis", esp::isShowInvisible, esp::setShowInvisible));
        }
        else if (module instanceof ChamsModule)
        {
            ChamsModule chams = (ChamsModule) module;
            card.addSetting(new SettingControl.LabelControl("Visible"));
            card.addSetting(new SettingControl.SliderControl("Red", () -> chams.getVisibleRed(), v -> chams.setVisibleRed(Math.round(v)), 0f, 255f, 1f, true));
            card.addSetting(new SettingControl.SliderControl("Green", () -> chams.getVisibleGreen(), v -> chams.setVisibleGreen(Math.round(v)), 0f, 255f, 1f, true));
            card.addSetting(new SettingControl.SliderControl("Blue", () -> chams.getVisibleBlue(), v -> chams.setVisibleBlue(Math.round(v)), 0f, 255f, 1f, true));
            card.addSetting(new SettingControl.LabelControl("Hidden"));
            card.addSetting(new SettingControl.SliderControl("Red", () -> chams.getHiddenRed(), v -> chams.setHiddenRed(Math.round(v)), 0f, 255f, 1f, true));
            card.addSetting(new SettingControl.SliderControl("Green", () -> chams.getHiddenGreen(), v -> chams.setHiddenGreen(Math.round(v)), 0f, 255f, 1f, true));
            card.addSetting(new SettingControl.SliderControl("Blue", () -> chams.getHiddenBlue(), v -> chams.setHiddenBlue(Math.round(v)), 0f, 255f, 1f, true));
            card.addSetting(new SettingControl.SliderControl("Glow Strength", () -> chams.getGlowStrength(), v -> chams.setGlowStrength(Math.round(v)), 0f, 100f, 1f, true));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0L ? 0f : Math.min(0.05f, (now - lastFrame) / 1000f);
        lastFrame = now;

        openAnim.update(now);
        categoryAnim.update(dt);

        ScaledResolution sr = new ScaledResolution(mc);
        GuiContext ctx = new GuiContext(mc, font, sr, mouseX, mouseY, now, dt);
        GuiRender.setScreenMetrics(ctx.screenH, ctx.sr.getScaleFactor());

        GuiRender.enableBlend();
        GuiRender.drawRect(0, 0, ctx.screenW, ctx.screenH, GuiRender.withAlpha(GuiTheme.OVERLAY, openAnim.get()));

        float open = openAnim.get();
        float openEase = GuiEasing.easeOutQuint(open);
        panelW = Math.min(880, ctx.screenW - 80);
        panelH = Math.min(520, ctx.screenH - 80);
        panelX = (ctx.screenW - panelW) / 2;
        panelY = (ctx.screenH - panelH) / 2 + Math.round((1f - openEase) * 20f);

        int panelRadius = GuiTheme.RADIUS_L;
        GuiRender.drawSoftShadow(panelX, panelY, panelW, panelH, panelRadius, 0xFF000000);
        GuiRender.drawRoundedRect(panelX, panelY, panelW, panelH, panelRadius, GuiTheme.PANEL_BG);
        GuiRender.drawRoundedRect(panelX, panelY, panelW, GuiTheme.HEADER_H, panelRadius, GuiTheme.PANEL_TOP);
        GuiRender.drawRect(panelX + 12, panelY + GuiTheme.HEADER_H - 2, panelX + panelW - 12, panelY + GuiTheme.HEADER_H, GuiTheme.BORDER);

        sidebarX = panelX;
        sidebarY = panelY + GuiTheme.HEADER_H;
        sidebarW = GuiTheme.SIDEBAR_W;
        sidebarH = panelH - GuiTheme.HEADER_H;
        GuiRender.drawRoundedRect(sidebarX + 6, sidebarY + 6, sidebarW - 12, sidebarH - 12, GuiTheme.RADIUS_M, GuiTheme.SIDEBAR_BG);

        contentX = panelX + sidebarW + 1;
        contentY = panelY + GuiTheme.HEADER_H;
        contentW = panelW - sidebarW - 1;
        contentH = panelH - GuiTheme.HEADER_H;
        GuiRender.drawRoundedRect(contentX + 6, contentY + 6, contentW - 12, contentH - 12, GuiTheme.RADIUS_M, GuiTheme.CONTENT_BG);

        drawHeader(ctx);
        drawSidebar(ctx);
        drawContent(ctx);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawHeader(GuiContext ctx)
    {
        String title = ChudWare.NAME;
        int titleX = panelX + GuiTheme.PADDING;
        int titleY = panelY + 10;
        ctx.font.drawString(title, titleX, titleY, GuiTheme.TEXT, ModernFontRenderer.SIZE_LARGE);

        String ver = "v" + ChudWare.VERSION;
        int verH = ctx.font.getFontHeight(ModernFontRenderer.SIZE_SMALL);
        int vw = ctx.font.getStringWidth(ver, ModernFontRenderer.SIZE_SMALL) + 12;
        int vx = panelX + panelW - vw - GuiTheme.PADDING;
        int boxH = verH + 8;
        int vy = panelY + (GuiTheme.HEADER_H - boxH) / 2;
        GuiRender.drawRoundedBorder(vx, vy, vw, boxH, GuiTheme.RADIUS_S, GuiTheme.BORDER, GuiTheme.TRACK);
        int vtx = vx + (vw - ctx.font.getStringWidth(ver, ModernFontRenderer.SIZE_SMALL)) / 2;
        int vty = vy + (boxH - verH) / 2;
        ctx.font.drawString(ver, vtx, vty, GuiTheme.ACCENT_2, ModernFontRenderer.SIZE_SMALL);

        GuiRender.drawRoundedRect(panelX + GuiTheme.PADDING, panelY + GuiTheme.HEADER_H - 7, 120, 3, 2, GuiTheme.ACCENT);
    }

    private void drawSidebar(GuiContext ctx)
    {
        Category[] order = {Category.COMBAT, Category.MOVEMENT, Category.VISUAL, Category.MISC, Category.CONFIGS};
        String[] labels = {"Combat", "Movement", "Visual", "Misc", "Configs"};
        int logoW = 40;
        int logoH = Math.round(logoW * 259f / 194f);
        int logoX = sidebarX + (sidebarW - logoW) / 2;
        int logoY = sidebarY + 10;
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        mc.getTextureManager().bindTexture(LOGO);
        GlStateManager.color(1f, 1f, 1f, 1f);
        drawScaledCustomSizeModalRect(logoX, logoY, 0f, 0f, 194, 259, logoW, logoH, 194f, 259f);

        int startY = logoY + logoH + 14;
        int itemH = 34;
        int itemX = sidebarX + 14;
        int itemW = sidebarW - 28;

        for (int i = 0; i < order.length; i++)
        {
            Category cat = order[i];
            int y = startY + i * (itemH + 8);
            boolean hover = ctx.hit(itemX, y, itemW, itemH);
            GuiAnim.SmoothFloat ha = categoryHover.get(cat);
            if (ha != null)
            {
                ha.setTarget(hover ? 1f : 0f);
                ha.update(ctx.delta);
            }

            boolean active = cat == activeCategory;
            int bg = active ? GuiTheme.CARD_ACTIVE : GuiTheme.CARD_BG;
            if (ha != null)
            {
                bg = GuiRender.lerpColor(bg, GuiTheme.CARD_HOVER, ha.get());
            }
            GuiRender.drawRoundedRect(itemX, y, itemW, itemH, GuiTheme.RADIUS_S, bg);
            if (active)
            {
                GuiRender.drawRoundedRect(itemX + 4, y + 6, 6, itemH - 12, GuiTheme.RADIUS_S, GuiTheme.ACCENT);
            }
            int tx = itemX + 16;
            int ty = y + (itemH - ctx.font.getFontHeight(ModernFontRenderer.SIZE_SMALL)) / 2;
            int col = GuiTheme.TEXT;
            ctx.font.drawString(labels[i], tx, ty, col, ModernFontRenderer.SIZE_SMALL);
        }
    }

    private void drawContent(GuiContext ctx)
    {
        GuiRender.beginScissor(ctx.sr, contentX, contentY, contentW, contentH);

        float raw = categoryAnim.get();
        if (raw > 0.999f)
        {
            categoryAnim.snap(1f);
            raw = 1f;
        }
        float t = GuiEasing.easeInOutCubic(raw);
        float offset = 40f * (1f - t);
        GlStateManager.pushMatrix();
        GlStateManager.translate(offset, 0, 0);
        drawCategoryContent(ctx, activeCategory);
        GlStateManager.popMatrix();

        GuiRender.endScissor();
    }

    private void drawCategoryContent(GuiContext ctx, Category category)
    {
        if (category == Category.CONFIGS)
        {
            configPanel.render(ctx, this, contentX, contentY, contentW, contentH);
            return;
        }

        List<ModuleCard> list = cards.get(category);
        if (list == null) return;

        ScrollState scroll = scrolls.get(category);
        if (scroll == null) return;
        scroll.update(ctx.delta);

        int y = contentY + GuiTheme.PADDING - Math.round(scroll.get());
        int x = contentX + GuiTheme.PADDING;
        int w = contentW - GuiTheme.PADDING * 2;
        int totalHeight = 0;

        for (ModuleCard card : list)
        {
            int cardH = Math.round(card.getHeight());
            if (y + cardH >= contentY && y <= contentY + contentH)
            {
                card.render(ctx, this, x, y, w);
            }
            y += cardH + GuiTheme.GAP;
            totalHeight += cardH + GuiTheme.GAP;
        }

        int maxScroll = Math.max(0, totalHeight - (contentH - GuiTheme.PADDING * 2));
        scroll.setMax(maxScroll);

        drawScrollBar(ctx, contentX + contentW - 6, contentY + 6, 4, contentH - 12, scroll.get(), maxScroll);
    }

    private void drawScrollBar(GuiContext ctx, int x, int y, int w, int h, float value, float max)
    {
        if (max <= 0f) return;
        float view = h / (h + max);
        int barH = Math.max(18, Math.round(h * view));
        float percent = value / max;
        int by = y + Math.round((h - barH) * percent);
        GuiRender.drawRoundedRect(x, by, w, barH, w, GuiTheme.ACCENT);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException
    {
        GuiContext ctx = new GuiContext(mc, font, new ScaledResolution(mc), mouseX, mouseY, System.currentTimeMillis(), 0f);

        if (mouseX >= sidebarX && mouseX <= sidebarX + sidebarW && mouseY >= sidebarY && mouseY <= sidebarY + sidebarH)
        {
            Category[] order = {Category.COMBAT, Category.MOVEMENT, Category.VISUAL, Category.MISC, Category.CONFIGS};
            int logoW = 40;
            int logoH = Math.round(logoW * 259f / 194f);
            int startY = sidebarY + 10 + logoH + 14;
            int itemH = 34;
            int itemX = sidebarX + 14;
            int itemW = sidebarW - 28;
            for (int i = 0; i < order.length; i++)
            {
                int y = startY + i * (itemH + 8);
                if (mouseX >= itemX && mouseX <= itemX + itemW
                        && mouseY >= y && mouseY <= y + itemH)
                {
                    setActiveCategory(order[i]);
                    return;
                }
            }
        }

        if (activeCategory == Category.CONFIGS)
        {
            configPanel.mouseClicked(ctx, this, button);
            return;
        }

        List<ModuleCard> list = cards.get(activeCategory);
        if (list != null)
        {
            for (ModuleCard card : list)
            {
                SettingControl.SliderControl slider = card.mouseClicked(ctx, this, button);
                if (slider != null)
                {
                    activeSlider = slider;
                    return;
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        GuiContext ctx = new GuiContext(mc, font, new ScaledResolution(mc), mouseX, mouseY, System.currentTimeMillis(), 0f);

        if (activeSlider != null)
        {
            activeSlider.mouseReleased(ctx, this, state);
            activeSlider = null;
        }

        List<ModuleCard> list = cards.get(activeCategory);
        if (list != null)
        {
            for (ModuleCard card : list)
            {
                card.mouseReleased(ctx, this, state);
            }
        }

        flushConfigIfDirty();
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick)
    {
        if (activeSlider != null)
        {
            GuiContext ctx = new GuiContext(mc, font, new ScaledResolution(mc), mouseX, mouseY, System.currentTimeMillis(), 0f);
            activeSlider.mouseDragged(ctx, this, clickedMouseButton);
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;

        int mx = Mouse.getEventX() * width / mc.displayWidth;
        int my = height - Mouse.getEventY() * height / mc.displayHeight - 1;

        if (mx < contentX || mx > contentX + contentW || my < contentY || my > contentY + contentH)
        {
            return;
        }

        float delta = -wheel / 3f;
        if (activeCategory == Category.CONFIGS)
        {
            configPanel.handleScroll(delta);
            return;
        }
        ScrollState scroll = scrolls.get(activeCategory);
        if (scroll != null)
        {
            scroll.add(delta);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keybindTarget != null)
        {
            if (keyCode == Keyboard.KEY_ESCAPE)
            {
                keybindTarget = null;
                return;
            }
            if (keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK)
            {
                keybindTarget.setKeyBind(Keyboard.KEY_NONE);
                markConfigDirty();
                flushConfigIfDirty();
                keybindTarget = null;
                return;
            }
            keybindTarget.setKeyBind(keyCode);
            markConfigDirty();
            flushConfigIfDirty();
            keybindTarget = null;
            return;
        }

        if (activeCategory == Category.CONFIGS && configPanel.handleKeyTyped(typedChar, keyCode))
        {
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE)
        {
            mc.displayGuiScreen(null);
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed()
    {
        flushConfigIfDirty();
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }

    private void setActiveCategory(Category category)
    {
        if (category == activeCategory) return;
        previousCategory = activeCategory;
        activeCategory = category;
        categoryAnim.snap(0f);
        categoryAnim.setTarget(1f);
        keybindTarget = null;
    }

    @Override
    public void requestKeybind(Module module)
    {
        keybindTarget = module;
    }

    @Override
    public boolean isKeybindTarget(Module module)
    {
        return keybindTarget == module;
    }

    @Override
    public void startSliderDrag(SettingControl.SliderControl slider)
    {
        activeSlider = slider;
    }

    @Override
    public void stopSliderDrag(SettingControl.SliderControl slider)
    {
        if (activeSlider == slider)
        {
            activeSlider = null;
        }
    }

    @Override
    public void markConfigDirty()
    {
        configDirty = true;
    }

    @Override
    public void clearConfigDirty()
    {
        configDirty = false;
    }

    private void flushConfigIfDirty()
    {
        if (!configDirty || configManager == null || moduleManager == null)
        {
            return;
        }
        configManager.save(moduleManager);
        configDirty = false;
    }
}
