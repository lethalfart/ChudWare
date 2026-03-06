package com.lethalfart.ChudWare.ui;

import com.lethalfart.ChudWare.config.ConfigManager;
import com.lethalfart.ChudWare.module.Category;
import com.lethalfart.ChudWare.module.Module;
import com.lethalfart.ChudWare.module.ModuleManager;
import com.lethalfart.ChudWare.module.impl.AimAssistModule;
import com.lethalfart.ChudWare.module.impl.AutoClickerModule;
import com.lethalfart.ChudWare.module.impl.ChamsModule;
import com.lethalfart.ChudWare.module.impl.ESPModule;
import com.lethalfart.ChudWare.module.impl.FullBrightModule;
import com.lethalfart.ChudWare.module.impl.ReachModule;
import com.lethalfart.ChudWare.module.impl.VelocityModule;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ModuleGuiScreen extends GuiScreen
{
    private static final int BG_TOP = 0x5A111B28;
    private static final int BG_BOTTOM = 0x2C090F18;
    private static final int VEIL = 0x26050A12;
    private static final int PANE = 0xAB0E1724;
    private static final int PANE_BORDER = 0xC03F5E80;
    private static final int HEADER = 0xAF142338;
    private static final int ROW = 0x65162438;
    private static final int ROW_HOVER = 0x9C203855;
    private static final int CONTROL = 0x7A314A68;
    private static final int ACCENT_A = 0xFF77F0FF;
    private static final int ACCENT_B = 0xFF4FD4FF;
    private static final int ACCENT_C = 0xFF67B8FF;
    private static final int TAB = 0x6B152337;
    private static final int TAB_ACTIVE = 0xC0315379;
    private static final int TEXT = 0xFFF2F7FF;
    private static final int TEXT_MUTED = 0xFF9FB3C9;
    private static final int TEXT_OFF = 0xFF768EA6;

    private static final int PANE_WIDTH = 620;
    private static final int PANE_HEIGHT = 334;
    private static final int PANE_TOP = 50;
    private static final int TAB_HEIGHT = 20;
    private static final int HEADER_HEIGHT = 22;
    private static final int MODULE_ROW_HEIGHT = 16;
    private static final int MODULE_ROW_STEP = 22;
    private static final int CONTROL_STEP = 22;
    private static final int CONTROL_TEXT_STEP = 13;
    private static final int MAX_CONFIG_NAME_LENGTH = 32;

    private static final int ID_AUTOCLICKER_MIN_CPS = 1;
    private static final int ID_AUTOCLICKER_MAX_CPS = 2;
    private static final int ID_REACH = 3;
    private static final int ID_AIM_SPEED = 4;
    private static final int ID_AIM_SMOOTH = 5;
    private static final int ID_AIM_DIST = 6;
    private static final int ID_ESP_LINE = 7;
    private static final int ID_ESP_DIST = 8;
    private static final int ID_ESP_R = 9;
    private static final int ID_ESP_G = 10;
    private static final int ID_ESP_B = 11;
    private static final int ID_ESP_A = 12;
    private static final int ID_CHAMS_VR = 13;
    private static final int ID_CHAMS_VG = 14;
    private static final int ID_CHAMS_VB = 15;
    private static final int ID_CHAMS_VA = 16;
    private static final int ID_CHAMS_HR = 17;
    private static final int ID_CHAMS_HG = 18;
    private static final int ID_CHAMS_HB = 19;
    private static final int ID_CHAMS_HA = 20;
    private static final int ID_VELOCITY_CHANCE = 21;
    private static final int ID_VELOCITY_HORIZONTAL = 22;
    private static final int ID_VELOCITY_VERTICAL = 23;
    private static final ResourceLocation MENU_LOGO = new ResourceLocation("chudware", "logo.png");

    private final ModuleManager moduleManager;
    private final ConfigManager configManager;

    private AutoClickerModule autoClickerModule;
    private ReachModule reachModule;
    private AimAssistModule aimAssistModule;
    private ESPModule espModule;
    private ChamsModule chamsModule;
    private VelocityModule velocityModule;
    @SuppressWarnings("unused")
    private FullBrightModule fullBrightModule;

    private ModernFontRenderer modernFont;
    private final Set<String> expandedModules = new HashSet<String>();
    private final List<ClickRegion> clickRegions = new ArrayList<ClickRegion>();
    private final List<SliderRegion> sliderRegions = new ArrayList<SliderRegion>();
    private final List<Raindrop> raindrops = new ArrayList<Raindrop>();
    private final Map<String, Float> moduleHoverAnim = new HashMap<String, Float>();
    private final Map<Integer, Float> sliderVisuals = new HashMap<Integer, Float>();
    private final Random rainRandom = new Random();

    private Category activeCategory = Category.COMBAT;
    private int activeSliderId = -1;
    private float contentAnim = 1.0F;
    private float combatTabAnim = 0.0F;
    private float movementTabAnim = 0.0F;
    private float visualTabAnim = 0.0F;
    private float miscTabAnim = 0.0F;
    private float configTabAnim = 0.0F;
    private long lastRainUpdateMs = 0L;
    private long nextConfigRefreshAtMs = 0L;
    private long configStatusUntilMs = 0L;
    private String configStatus = "";
    private final List<String> configNames = new ArrayList<String>();
    private boolean configNameEntryActive = false;
    private String configNameInput = "";
    private Module keybindCaptureModule;

    public ModuleGuiScreen(ModuleManager moduleManager, ConfigManager configManager)
    {
        this.moduleManager = moduleManager;
        this.configManager = configManager;
    }

    @Override
    public void initGui()
    {
        autoClickerModule = moduleManager.getModule(AutoClickerModule.class);
        reachModule = moduleManager.getModule(ReachModule.class);
        aimAssistModule = moduleManager.getModule(AimAssistModule.class);
        espModule = moduleManager.getModule(ESPModule.class);
        chamsModule = moduleManager.getModule(ChamsModule.class);
        velocityModule = moduleManager.getModule(VelocityModule.class);
        fullBrightModule = moduleManager.getModule(FullBrightModule.class);
        modernFont = new ModernFontRenderer(mc);
        clickRegions.clear();
        sliderRegions.clear();
        moduleHoverAnim.clear();
        activeSliderId = -1;
        raindrops.clear();
        lastRainUpdateMs = 0L;
        nextConfigRefreshAtMs = 0L;
        configStatusUntilMs = 0L;
        configStatus = "";
        configNameEntryActive = false;
        configNameInput = "";
        refreshConfigNames(true);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        clickRegions.clear();
        sliderRegions.clear();
        long time = System.currentTimeMillis();

        contentAnim = animate(contentAnim, 1.0F, 0.18F);
        combatTabAnim = animate(combatTabAnim, activeCategory == Category.COMBAT ? 1.0F : 0.0F, 0.24F);
        movementTabAnim = animate(movementTabAnim, activeCategory == Category.MOVEMENT ? 1.0F : 0.0F, 0.24F);
        visualTabAnim = animate(visualTabAnim, activeCategory == Category.VISUAL ? 1.0F : 0.0F, 0.24F);
        miscTabAnim = animate(miscTabAnim, activeCategory == Category.MISC ? 1.0F : 0.0F, 0.24F);
        configTabAnim = animate(configTabAnim, activeCategory == Category.CONFIGS ? 1.0F : 0.0F, 0.24F);

        drawBackdrop(mouseX, mouseY, time);
        drawModernCenteredText("ChudWare", width / 2, 11, TEXT, 1.03F);
        drawModernCenteredText("Modules", width / 2, 23, TEXT_MUTED, 0.66F);
        drawCategoryTabs(mouseX, mouseY);
        Pane mainPane = getMainPane();
        drawPane(mouseX, mouseY, mainPane, categoryTitle(activeCategory), activeCategory, contentAnim);
        drawMenuBrand(mainPane);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0 && mouseButton != 1)
        {
            return;
        }

        for (int i = clickRegions.size() - 1; i >= 0; i--)
        {
            ClickRegion region = clickRegions.get(i);
            if (!region.hit(mouseX, mouseY))
            {
                continue;
            }
            if (mouseButton == 0 && region.left != null)
            {
                region.left.run();
                return;
            }
            if (mouseButton == 1 && region.right != null)
            {
                region.right.run();
                return;
            }
        }

        if (mouseButton == 0)
        {
            for (SliderRegion slider : sliderRegions)
            {
                if (!slider.hit(mouseX, mouseY))
                {
                    continue;
                }
                activeSliderId = slider.id;
                applySliderValue(activeSliderId, slider.valueFromMouse(mouseX));
                return;
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick)
    {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (activeSliderId == -1 || clickedMouseButton != 0)
        {
            return;
        }

        for (SliderRegion slider : sliderRegions)
        {
            if (slider.id == activeSliderId)
            {
                applySliderValue(activeSliderId, slider.valueFromMouse(mouseX));
                return;
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        super.mouseReleased(mouseX, mouseY, state);
        activeSliderId = -1;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (configNameEntryActive)
        {
            if (keyCode == Keyboard.KEY_ESCAPE)
            {
                cancelConfigNameEntry();
                return;
            }
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER)
            {
                saveConfigFromInput();
                return;
            }
            if (keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK)
            {
                if (!configNameInput.isEmpty())
                {
                    configNameInput = configNameInput.substring(0, configNameInput.length() - 1);
                }
                return;
            }
            if (isAllowedConfigNameChar(typedChar) && configNameInput.length() < MAX_CONFIG_NAME_LENGTH)
            {
                configNameInput = configNameInput + typedChar;
            }
            return;
        }

        if (keybindCaptureModule != null)
        {
            if (keyCode == Keyboard.KEY_ESCAPE)
            {
                keybindCaptureModule = null;
                return;
            }
            if (keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK)
            {
                keybindCaptureModule.setKeyBind(Keyboard.KEY_NONE);
                keybindCaptureModule = null;
                return;
            }
            if (keyCode != Keyboard.KEY_NONE)
            {
                keybindCaptureModule.setKeyBind(keyCode);
                keybindCaptureModule = null;
            }
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }

    private void drawPane(int mouseX, int mouseY, Pane pane, String title, Category category, float openAnim)
    {
        drawShadow(pane.x - 6, pane.y + 4, pane.w + 12, pane.h + 2, 0x32000000);
        drawRoundedRect(pane.x, pane.y, pane.x + pane.w, pane.y + pane.h, 22, PANE_BORDER);
        drawRoundedRect(pane.x + 1, pane.y + 1, pane.x + pane.w - 1, pane.y + pane.h - 1, 20, PANE);

        int hx = pane.x + 10;
        int hy = pane.y + 8;
        int hw = pane.w - 20;
        int hh = HEADER_HEIGHT;
        drawRoundedRect(hx, hy, hx + hw, hy + hh, 11, HEADER);
        int accentW = Math.round((hw - 8) * openAnim);
        if (accentW > 0)
        {
            drawRoundedRect(hx + 4, hy + hh - 3, hx + 4 + accentW, hy + hh - 1, 1, ACCENT_B);
        }
        drawModernText(title, hx + 8, hy + 7, TEXT, 0.80F);
        drawRoundedRect(hx + 8, hy + hh + 7, hx + hw - 8, hy + hh + 8, 1, 0x26FFFFFF);

        List<Module> modules = modulesIn(category);
        int y = pane.y + 44;
        int maxY = pane.y + 38 + Math.round((pane.h - 52) * openAnim);
        if (category == Category.CONFIGS)
        {
            drawConfigPane(mouseX, mouseY, pane, y, maxY);
            return;
        }

        for (Module module : modules)
        {
            if (y + MODULE_ROW_HEIGHT > maxY)
            {
                break;
            }

            int rowX = pane.x + 10;
            int rowW = pane.w - 20;
            boolean rowHover = hit(mouseX, mouseY, rowX, y, rowW, MODULE_ROW_HEIGHT);
            String key = category.name() + ":" + module.getName();
            float hoverAnim = animate(moduleHoverAnim.containsKey(key) ? moduleHoverAnim.get(key) : 0.0F, rowHover ? 1.0F : 0.0F, 0.30F);
            moduleHoverAnim.put(key, hoverAnim);
            int rowBase = blendColor(ROW, ROW_HOVER, hoverAnim);
            int borderColor = 0x604FD4FF;
            int hoverColor = 0x80FFFFFF;
            drawRect(rowX - 1, y - 1, rowX + rowW + 1, y + MODULE_ROW_HEIGHT + 1, blendColor(borderColor, hoverColor, hoverAnim));
            drawRoundedRect(rowX, y, rowX + rowW, y + MODULE_ROW_HEIGHT, 9, rowBase);
            drawRoundedRect(rowX + 4, y + 3, rowX + 7, y + MODULE_ROW_HEIGHT - 3, 2, module.isEnabled() ? ACCENT_A : 0xFF4C5F70);
            
            drawModernText(module.getName(), rowX + 11, y + 4, TEXT, 0.70F);

            String status = module.isEnabled() ? "ON" : "OFF";
            if (hasSettings(module))
            {
                status = status + (expandedModules.contains(module.getName()) ? " [-]" : " [+]");
            }
            String keyName = module.getKeyBind() == Keyboard.KEY_NONE ? "NONE" : Keyboard.getKeyName(module.getKeyBind());
            if (keybindCaptureModule == module)
            {
                keyName = "...";
            }
            status = status + " [" + keyName + "]";
            int statusX = rowX + rowW - modernFont.getStringWidth(status, 0.68F) - 8;
            
            drawModernText(status, statusX, y + 4, module.isEnabled() ? ACCENT_B : TEXT_OFF, 0.66F);

            clickRegions.add(new ClickRegion(rowX, y, rowW, MODULE_ROW_HEIGHT, new Runnable()
            {
                @Override
                public void run()
                {
                    module.toggle();
                }
            }, new Runnable()
            {
                @Override
                public void run()
                {
                    toggleExpanded(module.getName());
                }
            }));

            y += MODULE_ROW_STEP;

            if (!expandedModules.contains(module.getName()))
            {
                continue;
            }

            int next = drawSettings(mouseX, mouseY, pane, module, y, maxY);
            if (next == y)
            {
                break;
            }
            y = next;
        }
    }

    private void drawConfigPane(int mouseX, int mouseY, Pane pane, int startY, int maxY)
    {
        int x = pane.x + 12;
        int w = pane.w - 24;
        int y = startY;

        if (configManager == null)
        {
            drawModernText("Config manager unavailable.", x + 2, y, TEXT_MUTED, 0.68F);
            return;
        }

        refreshConfigNamesIfNeeded();

        drawModernText("Folder: " + configManager.getConfigDirectory().getAbsolutePath(), x + 2, y, TEXT_MUTED, 0.60F);
        y += CONTROL_TEXT_STEP + 2;
        drawModernText("Active: " + configManager.getActiveConfigName(), x + 2, y, TEXT, 0.66F);
        y += CONTROL_TEXT_STEP + 3;

        int buttonW = (w - 8) / 3;
        int row1Y = y;
        drawConfigButton(x, row1Y, buttonW, "Open Folder", mouseX, mouseY, new Runnable()
        {
            @Override
            public void run()
            {
                configManager.openConfigFolder();
                setConfigStatus("Opened config folder");
            }
        });
        drawConfigButton(x + buttonW + 4, row1Y, buttonW, "Create New", mouseX, mouseY, new Runnable()
        {
            @Override
            public void run()
            {
                beginConfigNameEntry();
            }
        });
        drawConfigButton(x + (buttonW * 2) + 8, row1Y, buttonW, "Reset Default", mouseX, mouseY, new Runnable()
        {
            @Override
            public void run()
            {
                configManager.resetToDefaults(moduleManager);
                setConfigStatus("All modules disabled");
                refreshConfigNames(true);
            }
        });
        y += 22;

        if (configNameEntryActive)
        {
            int actionW = 58;
            int inputW = w - ((actionW * 2) + 8);
            boolean showCursor = (System.currentTimeMillis() / 400L) % 2L == 0L;
            String shownInput = configNameInput.isEmpty() ? "Type config name..." : configNameInput;
            if (showCursor && shownInput.length() < MAX_CONFIG_NAME_LENGTH)
            {
                shownInput = shownInput + "_";
            }

            drawRoundedRect(x, y, x + inputW, y + 16, 6, CONTROL);
            drawModernText(shownInput, x + 6, y + 5, configNameInput.isEmpty() ? TEXT_MUTED : TEXT, 0.62F);

            drawConfigButton(x + inputW + 4, y, actionW, "Save", mouseX, mouseY, new Runnable()
            {
                @Override
                public void run()
                {
                    saveConfigFromInput();
                }
            });
            drawConfigButton(x + inputW + actionW + 8, y, actionW, "Cancel", mouseX, mouseY, new Runnable()
            {
                @Override
                public void run()
                {
                    cancelConfigNameEntry();
                }
            });
            y += 22;
        }

        if (System.currentTimeMillis() < configStatusUntilMs && configStatus != null && !configStatus.isEmpty())
        {
            drawModernText(configStatus, x + 2, y, ACCENT_B, 0.62F);
            y += CONTROL_TEXT_STEP + 1;
        }

        drawModernText("Configs (click to load):", x + 2, y, TEXT_MUTED, 0.64F);
        y += CONTROL_TEXT_STEP;

        int maxRows = Math.max(0, (maxY - y) / MODULE_ROW_STEP);
        int shown = Math.min(maxRows, configNames.size());
        for (int i = 0; i < shown; i++)
        {
            final String configName = configNames.get(i);
            int rowY = y + (i * MODULE_ROW_STEP);
            boolean hover = hit(mouseX, mouseY, x, rowY, w, MODULE_ROW_HEIGHT);
            int bg = hover ? ROW_HOVER : ROW;
            drawRoundedRect(x, rowY, x + w, rowY + MODULE_ROW_HEIGHT, 8, bg);
            drawModernText(configName, x + 8, rowY + 4, TEXT, 0.66F);
            clickRegions.add(new ClickRegion(x, rowY, w, MODULE_ROW_HEIGHT, new Runnable()
            {
                @Override
                public void run()
                {
                    if (configManager.loadConfig(configName, moduleManager))
                    {
                        setConfigStatus("Loaded: " + configName);
                    }
                    else
                    {
                        setConfigStatus("Failed to load: " + configName);
                    }
                }
            }, null));
        }
    }

    private void drawConfigButton(int x, int y, int w, String text, int mouseX, int mouseY, Runnable onClick)
    {
        boolean hover = hit(mouseX, mouseY, x, y, w, 16);
        int bg = hover ? ROW_HOVER : CONTROL;
        drawRoundedRect(x, y, x + w, y + 16, 6, bg);
        drawModernCenteredText(text, x + (w / 2), y + 5, TEXT, 0.62F);
        clickRegions.add(new ClickRegion(x, y, w, 16, onClick, null));
    }

    private void refreshConfigNamesIfNeeded()
    {
        if (System.currentTimeMillis() >= nextConfigRefreshAtMs)
        {
            refreshConfigNames(false);
        }
    }

    private void refreshConfigNames(boolean force)
    {
        if (!force && System.currentTimeMillis() < nextConfigRefreshAtMs)
        {
            return;
        }
        configNames.clear();
        if (configManager != null)
        {
            configNames.addAll(configManager.listConfigNames());
        }
        nextConfigRefreshAtMs = System.currentTimeMillis() + 1000L;
    }

    private void setConfigStatus(String status)
    {
        configStatus = status;
        configStatusUntilMs = System.currentTimeMillis() + 2200L;
    }

    private void beginConfigNameEntry()
    {
        configNameEntryActive = true;
        configNameInput = "";
        keybindCaptureModule = null;
        setConfigStatus("Type a config name and press Enter");
    }

    private void saveConfigFromInput()
    {
        if (!configNameEntryActive)
        {
            return;
        }

        String name = configNameInput.trim();
        if (name.isEmpty())
        {
            setConfigStatus("Config name is required");
            return;
        }

        if (configManager.saveAs(name, moduleManager))
        {
            configNameEntryActive = false;
            configNameInput = "";
            setConfigStatus("Created and saved: " + name);
            refreshConfigNames(true);
        }
        else
        {
            setConfigStatus("Failed to create config");
        }
    }

    private void cancelConfigNameEntry()
    {
        configNameEntryActive = false;
        configNameInput = "";
        setConfigStatus("Create cancelled");
    }

    private boolean isAllowedConfigNameChar(char c)
    {
        return (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9')
                || c == ' '
                || c == '-'
                || c == '_';
    }

    private int drawSettings(int mouseX, int mouseY, Pane pane, Module module, int startY, int maxY)
    {
        int x = pane.x + 12;
        int w = pane.w - 24;
        int y = startY;

        y = drawKeybindControls(module, x, w, y, maxY);
        if (y == -1)
        {
            return startY;
        }

        if (module instanceof AutoClickerModule)
        {
            AutoClickerModule ac = (AutoClickerModule) module;
            y = drawSlider(x, y, w, "Min CPS", ac.getMinCps(), ID_AUTOCLICKER_MIN_CPS, 10, 200, Math.round(ac.getMinCps() * 10.0F), maxY, mouseX, mouseY);
            if (y == -1)
            {
                return startY;
            }

            y = drawSlider(x, y, w, "Max CPS", ac.getMaxCps(), ID_AUTOCLICKER_MAX_CPS, 10, 200, Math.round(ac.getMaxCps() * 10.0F), maxY, mouseX, mouseY);
            if (y == -1)
            {
                return startY;
            }

            return y;
        }

        if (module instanceof ReachModule)
        {
            ReachModule rm = (ReachModule) module;
            int value = (int) Math.round(rm.getReachMax() * 100.0F);
            int min = 300; 
            int max = 600; 
            y = drawSlider(x, y, w, "Reach", String.format(Locale.US, "%.2f", rm.getReachMax()), ID_REACH, min, max, value, maxY, mouseX, mouseY);
            return y == -1 ? startY : y;
        }

        if (module instanceof AimAssistModule)
        {
            AimAssistModule aim = (AimAssistModule) module;
            y = drawSlider(x, y, w, "Speed", String.valueOf(aim.getSpeed()), ID_AIM_SPEED, 1, 20, aim.getSpeed(), maxY, mouseX, mouseY);
            if (y == -1)
            {
                return startY;
            }

            y = drawSlider(x, y, w, "Smoothness", String.valueOf(aim.getSmoothness()), ID_AIM_SMOOTH, 1, 100, aim.getSmoothness(), maxY, mouseX, mouseY);
            if (y == -1)
            {
                return startY;
            }

            int dist = Math.round((float) (aim.getMaxDistance() * 10.0D));
            y = drawSlider(x, y, w, "Distance", String.format(Locale.US, "%.1f", aim.getMaxDistance()), ID_AIM_DIST, 10, 60, dist, maxY, mouseX, mouseY);
            if (y == -1)
            {
                return startY;
            }

            if (y + CONTROL_TEXT_STEP > maxY)
            {
                return startY;
            }
            drawModernText("Click Only: " + (aim.isOnClickOnly() ? "ON" : "OFF") + " (click)", x + 2, y, TEXT_MUTED, 0.64F);
            clickRegions.add(new ClickRegion(x, y - 1, w, CONTROL_TEXT_STEP, new Runnable()
            {
                @Override
                public void run()
                {
                    aim.setOnClickOnly(!aim.isOnClickOnly());
                }
            }, null));
            y += CONTROL_TEXT_STEP;
            return y;
        }

        if (module instanceof VelocityModule)
        {
            VelocityModule vel = (VelocityModule) module;
            if (y + CONTROL_TEXT_STEP > maxY)
            {
                return y;
            }
            drawModernText("No Water: " + (vel.isNoWater() ? "ON" : "OFF") + " (click)", x + 2, y, TEXT_MUTED, 0.64F);
            clickRegions.add(new ClickRegion(x, y - 1, w, CONTROL_TEXT_STEP, new Runnable()
            {
                @Override
                public void run()
                {
                    vel.setNoWater(!vel.isNoWater());
                }
            }, null));
            y += CONTROL_TEXT_STEP;

            if (y + CONTROL_TEXT_STEP > maxY)
            {
                return y;
            }
            drawModernText("No Lava: " + (vel.isNoLava() ? "ON" : "OFF") + " (click)", x + 2, y, TEXT_MUTED, 0.64F);
            clickRegions.add(new ClickRegion(x, y - 1, w, CONTROL_TEXT_STEP, new Runnable()
            {
                @Override
                public void run()
                {
                    vel.setNoLava(!vel.isNoLava());
                }
            }, null));
            y += CONTROL_TEXT_STEP;

            if (y + CONTROL_TEXT_STEP > maxY)
            {
                return y;
            }
            drawModernText("No Ladder: " + (vel.isNoLadder() ? "ON" : "OFF") + " (click)", x + 2, y, TEXT_MUTED, 0.64F);
            clickRegions.add(new ClickRegion(x, y - 1, w, CONTROL_TEXT_STEP, new Runnable()
            {
                @Override
                public void run()
                {
                    vel.setNoLadder(!vel.isNoLadder());
                }
            }, null));
            y += CONTROL_TEXT_STEP;

            if (y + CONTROL_TEXT_STEP > maxY)
            {
                return y;
            }
            drawModernText("Buffer Mode: " + (vel.isBufferMode() ? "ON" : "OFF") + " (click)", x + 2, y, TEXT_MUTED, 0.64F);
            clickRegions.add(new ClickRegion(x, y - 1, w, CONTROL_TEXT_STEP, new Runnable()
            {
                @Override
                public void run()
                {
                    vel.setBufferMode(!vel.isBufferMode());
                }
            }, null));
            y += CONTROL_TEXT_STEP;

            int chance = vel.getChance();
            y = drawSlider(x, y, w, "Chance", String.valueOf(chance) + "%", ID_VELOCITY_CHANCE, 10, 100, chance, maxY, mouseX, mouseY);
            if (y == -1)
            {
                return startY;
            }

            int horizontal = (int) Math.round(vel.getHorizontalPercent());
            y = drawSlider(x, y, w, "Horizontal", String.valueOf(horizontal) + "%", ID_VELOCITY_HORIZONTAL, 0, 100, horizontal, maxY, mouseX, mouseY);
            if (y == -1)
            {
                return startY;
            }

            int vertical = (int) Math.round(vel.getVerticalPercent());
            y = drawSlider(x, y, w, "Vertical", String.valueOf(vertical) + "%", ID_VELOCITY_VERTICAL, 0, 100, vertical, maxY, mouseX, mouseY);
            return y == -1 ? startY : y;
        }

        if (module instanceof ESPModule)
        {
            ESPModule esp = (ESPModule) module;
            y = drawSlider(x, y, w, "Line Width", String.valueOf(esp.getLineWidth()), ID_ESP_LINE, 1, 10, esp.getLineWidth(), maxY, mouseX, mouseY);
            if (y == -1)
            {
                return startY;
            }
            y = drawSlider(x, y, w, "Distance", String.valueOf(esp.getMaxDistance()), ID_ESP_DIST, 4, 96, esp.getMaxDistance(), maxY, mouseX, mouseY);
            if (y == -1)
            {
                return startY;
            }
            y = drawSlider(x, y, w, "Red", String.valueOf(esp.getRed()), ID_ESP_R, 0, 255, esp.getRed(), maxY, mouseX, mouseY);
            if (y == -1)
            {
                return startY;
            }
            y = drawSlider(x, y, w, "Green", String.valueOf(esp.getGreen()), ID_ESP_G, 0, 255, esp.getGreen(), maxY, mouseX, mouseY);
            if (y == -1)
            {
                return startY;
            }
            y = drawSlider(x, y, w, "Blue", String.valueOf(esp.getBlue()), ID_ESP_B, 0, 255, esp.getBlue(), maxY, mouseX, mouseY);
            if (y == -1)
            {
                return startY;
            }
            y = drawSlider(x, y, w, "Alpha", String.valueOf(esp.getAlpha()), ID_ESP_A, 0, 255, esp.getAlpha(), maxY, mouseX, mouseY);
            if (y == -1)
            {
                return startY;
            }
            if (y + CONTROL_TEXT_STEP > maxY)
            {
                return startY;
            }
            drawModernText("Show Invisible: " + (esp.isShowInvisible() ? "ON" : "OFF") + " (click)", x + 2, y, TEXT_MUTED, 0.64F);
            clickRegions.add(new ClickRegion(x, y - 1, w, CONTROL_TEXT_STEP, new Runnable()
            {
                @Override
                public void run()
                {
                    esp.setShowInvisible(!esp.isShowInvisible());
                }
            }, null));
            y += CONTROL_TEXT_STEP;
            return y;
        }

        if (module instanceof ChamsModule)
        {
            ChamsModule ch = (ChamsModule) module;
            if (y + CONTROL_TEXT_STEP > maxY)
            {
                return startY;
            }
            drawModernText("Visible Color", x + 2, y, TEXT_MUTED, 0.63F);
            y += CONTROL_TEXT_STEP;

            y = drawSlider(x, y, w, "V Red", String.valueOf(ch.getVisibleRed()), ID_CHAMS_VR, 0, 255, ch.getVisibleRed(), maxY, mouseX, mouseY);
            if (y == -1)
            {
                return startY;
            }
            y = drawSlider(x, y, w, "V Green", String.valueOf(ch.getVisibleGreen()), ID_CHAMS_VG, 0, 255, ch.getVisibleGreen(), maxY, mouseX, mouseY);
            if (y == -1)
            {
                return startY;
            }
            y = drawSlider(x, y, w, "V Blue", String.valueOf(ch.getVisibleBlue()), ID_CHAMS_VB, 0, 255, ch.getVisibleBlue(), maxY, mouseX, mouseY);
            if (y == -1)
            {
                return startY;
            }
            y = drawSlider(x, y, w, "V Alpha", String.valueOf(ch.getVisibleAlpha()), ID_CHAMS_VA, 0, 255, ch.getVisibleAlpha(), maxY, mouseX, mouseY);
            if (y == -1)
            {
                return startY;
            }

            if (y + CONTROL_TEXT_STEP > maxY)
            {
                return startY;
            }
            drawModernText("Hidden Color", x + 2, y, TEXT_MUTED, 0.63F);
            y += CONTROL_TEXT_STEP;

            y = drawSlider(x, y, w, "H Red", String.valueOf(ch.getHiddenRed()), ID_CHAMS_HR, 0, 255, ch.getHiddenRed(), maxY, mouseX, mouseY);
            if (y == -1)
            {
                return startY;
            }
            y = drawSlider(x, y, w, "H Green", String.valueOf(ch.getHiddenGreen()), ID_CHAMS_HG, 0, 255, ch.getHiddenGreen(), maxY, mouseX, mouseY);
            if (y == -1)
            {
                return startY;
            }
            y = drawSlider(x, y, w, "H Blue", String.valueOf(ch.getHiddenBlue()), ID_CHAMS_HB, 0, 255, ch.getHiddenBlue(), maxY, mouseX, mouseY);
            if (y == -1)
            {
                return startY;
            }
            y = drawSlider(x, y, w, "H Alpha", String.valueOf(ch.getHiddenAlpha()), ID_CHAMS_HA, 0, 255, ch.getHiddenAlpha(), maxY, mouseX, mouseY);
            return y == -1 ? startY : y;
        }

        return y;
    }

    private int drawKeybindControls(final Module module, int x, int w, int y, int maxY)
    {
        if (y + CONTROL_TEXT_STEP > maxY)
        {
            return -1;
        }
        String keyName = module.getKeyBind() == Keyboard.KEY_NONE ? "NONE" : Keyboard.getKeyName(module.getKeyBind());
        String bindText = keybindCaptureModule == module ? "Keybind: ... (press key)" : "Keybind: " + keyName + " (click to set)";
        drawModernText(bindText, x + 2, y, TEXT_MUTED, 0.64F);
        clickRegions.add(new ClickRegion(x, y - 1, w, CONTROL_TEXT_STEP, new Runnable()
        {
            @Override
            public void run()
            {
                keybindCaptureModule = module;
            }
        }, null));
        y += CONTROL_TEXT_STEP;

        if (y + CONTROL_TEXT_STEP > maxY)
        {
            return -1;
        }
        drawModernText("Clear Keybind (click)", x + 2, y, TEXT_MUTED, 0.64F);
        clickRegions.add(new ClickRegion(x, y - 1, w, CONTROL_TEXT_STEP, new Runnable()
        {
            @Override
            public void run()
            {
                module.setKeyBind(Keyboard.KEY_NONE);
            }
        }, null));
        y += CONTROL_TEXT_STEP;
        return y;
    }

    private int drawSlider(int x, int y, int w, String label, float displayValue, int id, int min, int max, int current, int maxY, int mouseX, int mouseY)
    {
        return drawSlider(x, y, w, label, String.format(Locale.US, "%.1f", displayValue), id, min, max, current, maxY, mouseX, mouseY);
    }

    private int drawSlider(int x, int y, int w, String label, String valueText, int id, int min, int max, int current, int maxY, int mouseX, int mouseY)
    {
        if (y + CONTROL_STEP > maxY)
        {
            return -1;
        }

        drawModernText(label, x + 2, y, TEXT_MUTED, 0.64F);
        int valueWidth = modernFont.getStringWidth(valueText, 0.64F);
        drawModernText(valueText, x + w - valueWidth - 2, y, TEXT, 0.64F);

        int trackX = x + 2;
        int trackY = y + 10;
        int trackW = w - 4;
        int trackH = 8;

        drawRoundedRect(trackX, trackY, trackX + trackW, trackY + trackH, 3, CONTROL);
        float pct = (float) (current - min) / (float) (max - min);
        float shown = sliderVisuals.containsKey(id) ? sliderVisuals.get(id) : pct;
        shown = animate(shown, pct, 0.35F);
        sliderVisuals.put(id, shown);
        int fill = Math.max(0, Math.min(trackW, Math.round(trackW * shown)));
        if (fill > 0)
        {
            drawRoundedRect(trackX, trackY, trackX + fill, trackY + trackH, 3, ACCENT_B);
            drawRoundedRect(trackX, trackY, trackX + Math.max(1, fill / 2), trackY + trackH, 3, 0x49FFFFFF);
        }

        int knobX = trackX + fill;
        drawRoundedRect(knobX - 3, trackY - 2, knobX + 3, trackY + trackH + 2, 3, ACCENT_A);
        drawRoundedRect(knobX - 1, trackY, knobX + 1, trackY + trackH, 1, 0x80FFFFFF);

        boolean hover = hit(mouseX, mouseY, trackX - 2, y, trackW + 4, 20);
        if (hover)
        {
            drawRoundedRect(trackX - 1, trackY - 1, trackX + trackW + 1, trackY + trackH + 1, 4, 0x1DFFFFFF);
        }

        sliderRegions.add(new SliderRegion(id, trackX, y, trackW, 20, min, max));
        return y + CONTROL_STEP;
    }

    private void applySliderValue(int id, int value)
    {
        if (autoClickerModule != null)
        {
            if (id == ID_AUTOCLICKER_MIN_CPS)
            {
                autoClickerModule.setMinCps(value / 10.0F);
                return;
            }
            if (id == ID_AUTOCLICKER_MAX_CPS)
            {
                autoClickerModule.setMaxCps(value / 10.0F);
                return;
            }
        }

        if (reachModule != null && id == ID_REACH)
        {
            reachModule.setReachMax(value / 100.0F);
            return;
        }

        if (aimAssistModule != null)
        {
            if (id == ID_AIM_SPEED)
            {
                aimAssistModule.setSpeed(value);
                return;
            }
            if (id == ID_AIM_SMOOTH)
            {
                aimAssistModule.setSmoothness(value);
                return;
            }
            if (id == ID_AIM_DIST)
            {
                aimAssistModule.setMaxDistance(value / 10.0D);
                return;
            }
        }

        if (velocityModule != null)
        {
            if (id == ID_VELOCITY_CHANCE)
            {
                velocityModule.setChance(value);
                return;
            }
            if (id == ID_VELOCITY_HORIZONTAL)
            {
                velocityModule.setHorizontalPercent(value);
                return;
            }
            if (id == ID_VELOCITY_VERTICAL)
            {
                velocityModule.setVerticalPercent(value);
                return;
            }
        }

        if (espModule != null)
        {
            if (id == ID_ESP_LINE)
            {
                espModule.setLineWidth(value);
                return;
            }
            if (id == ID_ESP_DIST)
            {
                espModule.setMaxDistance(value);
                return;
            }
            if (id == ID_ESP_R)
            {
                espModule.setRed(value);
                return;
            }
            if (id == ID_ESP_G)
            {
                espModule.setGreen(value);
                return;
            }
            if (id == ID_ESP_B)
            {
                espModule.setBlue(value);
                return;
            }
            if (id == ID_ESP_A)
            {
                espModule.setAlpha(value);
                return;
            }
        }

        if (chamsModule != null)
        {
            if (id == ID_CHAMS_VR)
            {
                chamsModule.setVisibleRed(value);
                return;
            }
            if (id == ID_CHAMS_VG)
            {
                chamsModule.setVisibleGreen(value);
                return;
            }
            if (id == ID_CHAMS_VB)
            {
                chamsModule.setVisibleBlue(value);
                return;
            }
            if (id == ID_CHAMS_VA)
            {
                chamsModule.setVisibleAlpha(value);
                return;
            }
            if (id == ID_CHAMS_HR)
            {
                chamsModule.setHiddenRed(value);
                return;
            }
            if (id == ID_CHAMS_HG)
            {
                chamsModule.setHiddenGreen(value);
                return;
            }
            if (id == ID_CHAMS_HB)
            {
                chamsModule.setHiddenBlue(value);
                return;
            }
            if (id == ID_CHAMS_HA)
            {
                chamsModule.setHiddenAlpha(value);
            }
        }
    }

    private void drawCategoryTabs(int mouseX, int mouseY)
    {
        int tabW = 90;
        int gap = 8;
        int total = (tabW * 5) + (gap * 4);
        int startX = (width - total) / 2;
        int y = 34;
        drawRoundedRect(startX - 8, y - 3, startX + total + 8, y + TAB_HEIGHT + 4, 9, 0x38132133);
        drawRoundedRect(startX - 7, y - 2, startX + total + 7, y + TAB_HEIGHT + 3, 8, 0x52172A40);

        drawTabChip(mouseX, mouseY, startX, y, tabW, "Combat", Category.COMBAT, combatTabAnim);
        drawTabChip(mouseX, mouseY, startX + tabW + gap, y, tabW, "Movement", Category.MOVEMENT, movementTabAnim);
        drawTabChip(mouseX, mouseY, startX + (tabW + gap) * 2, y, tabW, "Visual", Category.VISUAL, visualTabAnim);
        drawTabChip(mouseX, mouseY, startX + (tabW + gap) * 3, y, tabW, "Misc", Category.MISC, miscTabAnim);
        drawTabChip(mouseX, mouseY, startX + (tabW + gap) * 4, y, tabW, "Configs", Category.CONFIGS, configTabAnim);
    }

    private void drawTabChip(int mouseX, int mouseY, int x, int y, int w, String title, Category category, float activeAnim)
    {
        boolean hover = hit(mouseX, mouseY, x, y, w, TAB_HEIGHT);
        int base = blendColor(TAB, TAB_ACTIVE, activeAnim);
        if (hover)
        {
            base = brighten(base, 10);
        }
        drawRoundedRect(x, y, x + w, y + TAB_HEIGHT, 7, base);
        int accentW = Math.round((w - 8) * (0.30F + (activeAnim * 0.70F)));
        drawRoundedRect(x + 4, y + TAB_HEIGHT - 3, x + 4 + accentW, y + TAB_HEIGHT - 1, 2, activeAnim > 0.5F ? ACCENT_A : ACCENT_C);
        drawModernCenteredText(title, x + (w / 2), y + 7, hover ? TEXT : TEXT_MUTED, 0.70F);

        clickRegions.add(new ClickRegion(x, y, w, TAB_HEIGHT, new Runnable()
        {
            @Override
            public void run()
            {
                setActiveCategory(category);
            }
        }, null));
    }

    private void drawBackdrop(int mouseX, int mouseY, long t)
    {
        drawGradientRect(0, 0, width, height, BG_TOP, BG_BOTTOM);
        drawGradientRect(0, 0, width, height, VEIL, 0x00000000);
        drawRaindropLayer(t);

        float parallaxX = (mouseX - (width / 2.0F)) * 0.015F;
        float parallaxY = (mouseY - (height / 2.0F)) * 0.015F;
        double time = (t % 600000L) / 1000.0D;

        for (int i = 0; i < 6; i++)
        {
            float radius = 44.0F + (i * 8.0F);
            float cx = (float) (width * (0.14D + (i * 0.14D))
                    + (Math.sin(time * 0.26D + i * 1.3D) * (10.0D + (i * 1.1D)))
                    - parallaxX * (0.18F + i * 0.05F));
            float cy = (float) (height * (0.16D + ((i % 3) * 0.22D))
                    + (Math.cos(time * 0.22D + i * 0.9D) * (9.0D + (i * 1.3D)))
                    - parallaxY * (0.14F + i * 0.04F));
            int glow = i % 2 == 0 ? 0x1773D6FF : 0x135E9EDB;
            drawRoundedRect(Math.round(cx - radius), Math.round(cy - radius), Math.round(cx + radius), Math.round(cy + radius), Math.round(radius), glow);
        }
    }

    private void drawRaindropLayer(long nowMs)
    {
        if (width <= 0 || height <= 0)
        {
            return;
        }

        if (lastRainUpdateMs == 0L)
        {
            lastRainUpdateMs = nowMs;
        }

        float dt = (nowMs - lastRainUpdateMs) / 16.6667F;
        if (dt < 0.25F)
        {
            dt = 0.25F;
        }
        if (dt > 3.0F)
        {
            dt = 3.0F;
        }
        lastRainUpdateMs = nowMs;

        int targetCount = Math.max(42, width / 14);
        if (raindrops.size() < targetCount)
        {
            int spawnBurst = Math.min(12, targetCount - raindrops.size());
            for (int i = 0; i < spawnBurst; i++)
            {
                raindrops.add(createRaindrop(true));
            }
        }
        else if (rainRandom.nextFloat() < 0.30F)
        {
            raindrops.add(createRaindrop(false));
        }

        for (int i = raindrops.size() - 1; i >= 0; i--)
        {
            Raindrop drop = raindrops.get(i);
            drop.x += drop.drift * dt;
            drop.y += drop.speed * dt;
            drop.life -= 0.010F * dt;

            int alpha = Math.round(drop.alpha * Math.max(0.12F, drop.life));
            if (alpha > 0)
            {
                int x1 = Math.round(drop.x);
                int y1 = Math.round(drop.y);
                int x2 = x1 + 1;
                int y2 = y1 + Math.max(4, Math.round(drop.length));
                int color = (Math.min(255, alpha) << 24) | 0x7FD9FF;
                drawRect(x1, y1, x2, y2, color);
            }

            if (drop.y > height + 12 || drop.x < -20 || drop.x > width + 20 || drop.life <= 0.0F)
            {
                raindrops.remove(i);
            }
        }
    }

    private Raindrop createRaindrop(boolean startup)
    {
        float x = (rainRandom.nextFloat() * (width + 30)) - 15.0F;
        float y = startup ? (rainRandom.nextFloat() * (height + 80)) - 80.0F : -18.0F - (rainRandom.nextFloat() * 40.0F);
        float speed = 3.6F + (rainRandom.nextFloat() * 4.7F);
        float length = 6.0F + (rainRandom.nextFloat() * 11.0F);
        float drift = -0.35F + (rainRandom.nextFloat() * 0.7F);
        float alpha = 70.0F + (rainRandom.nextFloat() * 95.0F);
        return new Raindrop(x, y, speed, length, drift, alpha);
    }

    private void drawMenuBrand(Pane pane)
    {
        int cardW = 182;
        int cardH = 52;
        int x = pane.x + pane.w - cardW - 12;
        int y = pane.y + pane.h - cardH - 12;
        int logoSize = 34;

        drawRoundedRect(x, y, x + cardW, y + cardH, 8, 0x81243B56);
        drawRoundedRect(x + 1, y + 1, x + cardW - 1, y + cardH - 1, 7, 0xB0162A42);
        drawRoundedRect(x + 10, y + cardH - 6, x + cardW - 10, y + cardH - 4, 2, 0x5A77F0FF);

        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 0.96F);
        mc.getTextureManager().bindTexture(MENU_LOGO);
        Gui.drawModalRectWithCustomSizedTexture(x + 9, y + 9, 0.0F, 0.0F, logoSize, logoSize, logoSize, logoSize);

        drawModernText("ChudWare", x + 50, y + 14, TEXT, 0.84F);
        drawModernText("version 1.0.4", x + 50, y + 30, TEXT_MUTED, 0.60F);
    }

    private void drawShadow(int x, int y, int w, int h, int color)
    {
        drawRoundedRect(x, y, x + w, y + h, 10, color);
    }

    private List<Module> modulesIn(Category category)
    {
        List<Module> list = new ArrayList<Module>();
        for (Module module : moduleManager.getModules())
        {
            if (module.getCategory() == category)
            {
                list.add(module);
            }
        }
        return list;
    }

    private boolean hasSettings(Module module)
    {
        return true;
    }

    private void toggleExpanded(String name)
    {
        if (expandedModules.contains(name))
        {
            expandedModules.remove(name);
        }
        else
        {
            expandedModules.add(name);
        }
    }

    private void setActiveCategory(Category category)
    {
        if (activeCategory != category)
        {
            activeCategory = category;
            contentAnim = 0.55F;
        }
    }

    private String categoryTitle(Category category)
    {
        if (category == Category.COMBAT)
        {
            return "Combat Modules";
        }
        if (category == Category.MOVEMENT)
        {
            return "Movement Modules";
        }
        if (category == Category.VISUAL)
        {
            return "Visual Modules";
        }
        if (category == Category.CONFIGS)
        {
            return "Config Profiles";
        }
        return "Misc Modules";
    }

    private Pane getMainPane()
    {
        int w = Math.min(PANE_WIDTH, Math.max(380, width - 70));
        int h = Math.min(PANE_HEIGHT, Math.max(240, height - 90));
        int x = (width - w) / 2;
        int y = Math.max(PANE_TOP, (height - h) / 2);
        return new Pane(x, y, w, h);
    }

    private boolean hit(int mx, int my, int x, int y, int w, int h)
    {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private int brighten(int color, int plus)
    {
        int a = (color >>> 24) & 0xFF;
        int r = Math.min(255, ((color >>> 16) & 0xFF) + plus);
        int g = Math.min(255, ((color >>> 8) & 0xFF) + plus);
        int b = Math.min(255, (color & 0xFF) + plus);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private float animate(float current, float target, float speed)
    {
        float delta = target - current;
        if (Math.abs(delta) < 0.001F)
        {
            return target;
        }
        return current + (delta * speed);
    }

    private int blendColor(int from, int to, float t)
    {
        float clamped = Math.max(0.0F, Math.min(1.0F, t));
        int a = Math.round(lerp((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, clamped));
        int r = Math.round(lerp((from >>> 16) & 0xFF, (to >>> 16) & 0xFF, clamped));
        int g = Math.round(lerp((from >>> 8) & 0xFF, (to >>> 8) & 0xFF, clamped));
        int b = Math.round(lerp(from & 0xFF, to & 0xFF, clamped));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private float lerp(float a, float b, float t)
    {
        return a + ((b - a) * t);
    }

    private void drawModernText(String text, int x, int y, int color, float scale)
    {
        if (modernFont == null)
        {
            modernFont = new ModernFontRenderer(mc);
        }
        modernFont.drawString(text, x, y, color, true, scale);
    }

    private void drawModernCenteredText(String text, int centerX, int y, int color, float scale)
    {
        if (modernFont == null)
        {
            modernFont = new ModernFontRenderer(mc);
        }
        modernFont.drawCenteredString(text, centerX, y, color, true, scale);
    }

    private void drawRoundedRect(int left, int top, int right, int bottom, int radius, int color)
    {
        if (right <= left || bottom <= top)
        {
            return;
        }
        drawRect(left, top, right, bottom, color);
    }

    private static class Pane
    {
        private final int x;
        private final int y;
        private final int w;
        private final int h;

        private Pane(int x, int y, int w, int h)
        {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    private static class ClickRegion
    {
        private final int x;
        private final int y;
        private final int w;
        private final int h;
        private final Runnable left;
        private final Runnable right;

        private ClickRegion(int x, int y, int w, int h, Runnable left, Runnable right)
        {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.left = left;
            this.right = right;
        }

        private boolean hit(int mx, int my)
        {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    private static class SliderRegion
    {
        private final int id;
        private final int x;
        private final int y;
        private final int w;
        private final int h;
        private final int min;
        private final int max;

        private SliderRegion(int id, int x, int y, int w, int h, int min, int max)
        {
            this.id = id;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.min = min;
            this.max = max;
        }

        private boolean hit(int mx, int my)
        {
            return mx >= x - 2 && mx <= x + w + 2 && my >= y && my <= y + h;
        }

        private int valueFromMouse(int mx)
        {
            int relative = mx - x;
            if (relative < 0)
            {
                relative = 0;
            }
            if (relative > w)
            {
                relative = w;
            }
            float pct = (float) relative / (float) w;
            return min + Math.round((max - min) * pct);
        }
    }

    private static class Raindrop
    {
        private float x;
        private float y;
        private final float speed;
        private final float length;
        private final float drift;
        private final float alpha;
        private float life = 1.0F;

        private Raindrop(float x, float y, float speed, float length, float drift, float alpha)
        {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.length = length;
            this.drift = drift;
            this.alpha = alpha;
        }
    }
}
