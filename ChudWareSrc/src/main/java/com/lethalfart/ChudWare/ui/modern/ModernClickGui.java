package com.lethalfart.ChudWare.ui.modern;

import com.lethalfart.ChudWare.config.ConfigManager;
import com.lethalfart.ChudWare.module.Category;
import com.lethalfart.ChudWare.module.Module;
import com.lethalfart.ChudWare.module.ModuleManager;
import com.lethalfart.ChudWare.module.impl.AimAssistModule;
import com.lethalfart.ChudWare.module.impl.AutoClickerModule;
import com.lethalfart.ChudWare.module.impl.AutoPotModule;
import com.lethalfart.ChudWare.module.impl.ChamsModule;
import com.lethalfart.ChudWare.module.impl.ESPModule;
import com.lethalfart.ChudWare.module.impl.RefillModule;
import com.lethalfart.ChudWare.module.impl.RightClickerModule;
import com.lethalfart.ChudWare.module.impl.VelocityModule;
import com.lethalfart.ChudWare.ui.ModernFontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

public class ModernClickGui extends GuiScreen
{
    private static final int OVERLAY = 0x6A0B0F14;
    private static final int PANEL = 0xB413171D;
    private static final int PANEL_SOFT = 0x8A161B22;
    private static final int PANEL_ALT = 0x78181D25;
    private static final int BORDER = 0x402A313B;
    private static final int BORDER_STRONG = 0x5A353E49;
    private static final int TEXT = 0xFFF3F7FF;
    private static final int TEXT_SOFT = 0x889EA7B3;
    private static final int TEXT_MID = 0xB8C8D0D9;
    private static final int ACCENT = 0xFFD4DAE1;
    private static final int TRACK = 0x5A20262E;
    private static final int SHADOW = 0xA0000000;
    private static final ResourceLocation LOGO = new ResourceLocation("chudware", "logo.png");

    private static final int TITLE_SIZE = ModernFontRenderer.SIZE_SMALL;
    private static final int TEXT_SIZE = ModernFontRenderer.SIZE_TINY;
    private static final int LABEL_SIZE = ModernFontRenderer.SIZE_SMALL;

    private final ModuleManager moduleManager;
    private final ConfigManager configManager;

    private final EnumMap<Category, List<ModulePanel>> panels = new EnumMap<>(Category.class);
    private final EnumMap<Category, ScrollState> scrolls = new EnumMap<>(Category.class);

    private ModernFontRenderer font;
    private Category activeCategory = Category.COMBAT;
    private SliderRow draggingSlider;
    private Module keybindTarget;
    private boolean configDirty;
    private String newConfigName = "";
    private boolean namingConfig;
    private List<String> cachedConfigs = new ArrayList<>();
    private long lastFrameAt;

    private int rootX;
    private int rootY;
    private int rootW;
    private int rootH;
    private int sidebarX;
    private int sidebarY;
    private int sidebarW;
    private int sidebarH;
    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;

    private int configButtonY;
    private int configInputY;
    private int configListY;
    private int configListH;
    private static final Category[] CATEGORIES = {Category.COMBAT, Category.MOVEMENT, Category.VISUAL, Category.MISC, Category.CONFIGS};

    public ModernClickGui(ModuleManager moduleManager, ConfigManager configManager)
    {
        this.moduleManager = moduleManager;
        this.configManager = configManager;
    }

    @Override
    public void initGui()
    {
        this.font = new ModernFontRenderer(mc);
        this.lastFrameAt = System.currentTimeMillis();
        this.draggingSlider = null;
        this.keybindTarget = null;
        this.namingConfig = false;
        this.newConfigName = "";
        this.configDirty = false;
        buildPanels();
        refreshConfigs();
        for (Category category : Category.values())
        {
            ScrollState scroll = scrolls.get(category);
            if (scroll == null)
            {
                scroll = new ScrollState();
                scroll.setSpeed(18f);
                scrolls.put(category, scroll);
            }
        }
    }

    private void buildPanels()
    {
        panels.clear();
        for (Category category : Category.values())
        {
            panels.put(category, new ArrayList<ModulePanel>());
        }

        List<Module> modules = new ArrayList<>(moduleManager.getModules());
        Collections.sort(modules, new Comparator<Module>()
        {
            @Override
            public int compare(Module a, Module b)
            {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });

        for (Module module : modules)
        {
            ModulePanel panel = new ModulePanel(module, buildRows(module));
            List<ModulePanel> list = panels.get(module.getCategory());
            if (list != null)
            {
                list.add(panel);
            }
        }
    }

    private List<Row> buildRows(Module module)
    {
        List<Row> rows = new ArrayList<Row>();
        rows.add(new KeybindRow(module));

        if (module instanceof AutoClickerModule)
        {
            AutoClickerModule ac = (AutoClickerModule) module;
            rows.add(new SliderRow("min cps", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return ac.getMinCps();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    ac.setMinCps(value);
                }
            }, 1f, 20f, 0.1f, false));
            rows.add(new SliderRow("max cps", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return ac.getMaxCps();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    ac.setMaxCps(value);
                }
            }, 1f, 20f, 0.1f, false));
            rows.add(new ToggleRow("inventory fill", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return ac.isInventoryFill();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    ac.setInventoryFill(value);
                }
            }));
            rows.add(new ToggleRow("weapon only", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return ac.isWeaponOnly();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    ac.setWeaponOnly(value);
                }
            }));
            rows.add(new ToggleRow("break blocks", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return ac.isBreakBlocks();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    ac.setBreakBlocks(value);
                }
            }));
        }
        else if (module instanceof RightClickerModule)
        {
            RightClickerModule rc = (RightClickerModule) module;
            rows.add(new SliderRow("min cps", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return rc.getMinCps();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    rc.setMinCps(value);
                }
            }, 1f, 20f, 0.1f, false));
            rows.add(new SliderRow("max cps", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return rc.getMaxCps();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    rc.setMaxCps(value);
                }
            }, 1f, 20f, 0.1f, false));
        }
        else if (module instanceof AutoPotModule)
        {
            AutoPotModule ap = (AutoPotModule) module;
            rows.add(new SliderRow("action delay", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return ap.getActionDelay();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    ap.setActionDelay(Math.round(value));
                }
            }, 20f, 500f, 1f, true));
            rows.add(new HotbarRow(ap));
        }
        else if (module instanceof RefillModule)
        {
            RefillModule refill = (RefillModule) module;
            rows.add(new ToggleRow("soup", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return refill.isSoup();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    refill.setSoup(value);
                }
            }));
            rows.add(new ToggleRow("potion", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return refill.isPotion();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    refill.setPotion(value);
                }
            }));
            rows.add(new ToggleRow("use non-health potions", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return refill.isUseNonHealthPotions();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    refill.setUseNonHealthPotions(value);
                }
            }));
            rows.add(new SliderRow("delay after open", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return refill.getDelayAfterOpen();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    refill.setDelayAfterOpen(Math.round(value));
                }
            }, 0f, 1000f, 10f, true));
            rows.add(new SliderRow("delay before close", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return refill.getDelayBeforeClose();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    refill.setDelayBeforeClose(Math.round(value));
                }
            }, 0f, 1000f, 10f, true));
            rows.add(new SliderRow("speed", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return refill.getSpeed();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    refill.setSpeed(value);
                }
            }, 1f, 20f, 0.5f, false));
            rows.add(new ToggleRow("smart speed", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return refill.isSmartSpeed();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    refill.setSmartSpeed(value);
                }
            }));
        }
        else if (module instanceof AimAssistModule)
        {
            AimAssistModule aim = (AimAssistModule) module;
            rows.add(new SliderRow("speed 1", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return (float) aim.getSpeed1();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    aim.setSpeed1(value);
                }
            }, 5f, 100f, 0.5f, false));
            rows.add(new SliderRow("speed 2", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return (float) aim.getSpeed2();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    aim.setSpeed2(value);
                }
            }, 2f, 97f, 0.5f, false));
            rows.add(new SliderRow("fov", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return (float) aim.getFov();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    aim.setFov(value);
                }
            }, 15f, 360f, 1f, true));
            rows.add(new SliderRow("distance", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return (float) aim.getDistance();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    aim.setDistance(value);
                }
            }, 1f, 10f, 0.1f, false));
            rows.add(new ToggleRow("click aim", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return aim.isClickAim();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    aim.setClickAim(value);
                }
            }));
            rows.add(new ToggleRow("break blocks", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return aim.isBreakBlocks();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    aim.setBreakBlocks(value);
                }
            }));
            rows.add(new ToggleRow("ignore friends", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return aim.isIgnoreFriends();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    aim.setIgnoreFriends(value);
                }
            }));
            rows.add(new ToggleRow("weapon only", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return aim.isWeaponOnly();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    aim.setWeaponOnly(value);
                }
            }));
            rows.add(new ToggleRow("aim invisible", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return aim.isAimInvis();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    aim.setAimInvis(value);
                }
            }));
            rows.add(new ToggleRow("blatant mode", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return aim.isBlatantMode();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    aim.setBlatantMode(value);
                }
            }));
            rows.add(new ToggleRow("ignore naked", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return aim.isIgnoreNaked();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    aim.setIgnoreNaked(value);
                }
            }));
            rows.add(new ToggleRow("middle click friends", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return aim.isMiddleClickFriends();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    aim.setMiddleClickFriends(value);
                }
            }));
        }
        else if (module instanceof VelocityModule)
        {
            VelocityModule vel = (VelocityModule) module;
            rows.add(new SliderRow("chance", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return vel.getChance();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    vel.setChance(Math.round(value));
                }
            }, 10f, 100f, 1f, true));
            rows.add(new SliderRow("horizontal %", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return (float) vel.getHorizontalPercent();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    vel.setHorizontalPercent(value);
                }
            }, 0f, 100f, 1f, true));
            rows.add(new SliderRow("vertical %", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return (float) vel.getVerticalPercent();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    vel.setVerticalPercent(value);
                }
            }, 0f, 100f, 1f, true));
            rows.add(new ToggleRow("no water", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return vel.isNoWater();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    vel.setNoWater(value);
                }
            }));
            rows.add(new ToggleRow("no lava", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return vel.isNoLava();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    vel.setNoLava(value);
                }
            }));
            rows.add(new ToggleRow("no ladder", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return vel.isNoLadder();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    vel.setNoLadder(value);
                }
            }));
            rows.add(new ToggleRow("buffer mode", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return vel.isBufferMode();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    vel.setBufferMode(value);
                }
            }));
        }
        else if (module instanceof ESPModule)
        {
            ESPModule esp = (ESPModule) module;
            rows.add(new SectionRow("targets"));
            rows.add(new ToggleRow("players", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return esp.isTargetPlayers();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    esp.setTargetPlayers(value);
                }
            }));
            rows.add(new ToggleRow("animals", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return esp.isTargetAnimals();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    esp.setTargetAnimals(value);
                }
            }));
            rows.add(new ToggleRow("mobs", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return esp.isTargetMobs();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    esp.setTargetMobs(value);
                }
            }));
            rows.add(new ToggleRow("passives", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return esp.isTargetPassives();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    esp.setTargetPassives(value);
                }
            }));
            rows.add(new SliderRow("red", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return esp.getRed();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    esp.setRed(Math.round(value));
                }
            }, 0f, 255f, 1f, true));
            rows.add(new SliderRow("green", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return esp.getGreen();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    esp.setGreen(Math.round(value));
                }
            }, 0f, 255f, 1f, true));
            rows.add(new SliderRow("blue", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return esp.getBlue();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    esp.setBlue(Math.round(value));
                }
            }, 0f, 255f, 1f, true));
            rows.add(new SliderRow("alpha", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return esp.getAlpha();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    esp.setAlpha(Math.round(value));
                }
            }, 50f, 255f, 1f, true));
        }
        else if (module instanceof ChamsModule)
        {
            ChamsModule chams = (ChamsModule) module;
            rows.add(new ToggleRow("colored", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return chams.isColored();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    chams.setColored(value);
                }
            }));
            rows.add(new ToggleRow("material", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return chams.isMaterial();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    chams.setMaterial(value);
                }
            }));
            rows.add(new ToggleRow("rainbow", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return chams.isRainbow();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    chams.setRainbow(value);
                }
            }));
            rows.add(new ToggleRow("pulse", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return chams.isPulse();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    chams.setPulse(value);
                }
            }));
            rows.add(new SliderRow("pulse speed", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return chams.getPulseSpeed();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    chams.setPulseSpeed(value);
                }
            }, 5f, 20f, 0.5f, false));
            rows.add(new ToggleRow("targets only", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return chams.isOnlyTargets();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    chams.setOnlyTargets(value);
                }
            }));
            rows.add(new SectionRow("targets"));
            rows.add(new ToggleRow("players", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return chams.isTargetPlayers();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    chams.setTargetPlayers(value);
                }
            }));
            rows.add(new ToggleRow("animals", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return chams.isTargetAnimals();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    chams.setTargetAnimals(value);
                }
            }));
            rows.add(new ToggleRow("mobs", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return chams.isTargetMobs();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    chams.setTargetMobs(value);
                }
            }));
            rows.add(new ToggleRow("passives", new BoolGetter()
            {
                @Override
                public boolean get()
                {
                    return chams.isTargetPassives();
                }
            }, new BoolSetter()
            {
                @Override
                public void set(boolean value)
                {
                    chams.setTargetPassives(value);
                }
            }));
            rows.add(new SectionRow("visible"));
            rows.add(new SliderRow("red", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return chams.getVisibleRed();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    chams.setVisibleRed(Math.round(value));
                }
            }, 0f, 255f, 1f, true));
            rows.add(new SliderRow("green", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return chams.getVisibleGreen();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    chams.setVisibleGreen(Math.round(value));
                }
            }, 0f, 255f, 1f, true));
            rows.add(new SliderRow("blue", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return chams.getVisibleBlue();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    chams.setVisibleBlue(Math.round(value));
                }
            }, 0f, 255f, 1f, true));
            rows.add(new SliderRow("alpha", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return chams.getVisibleAlpha();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    chams.setVisibleAlpha(Math.round(value));
                }
            }, 50f, 255f, 1f, true));
            rows.add(new SectionRow("hidden"));
            rows.add(new SliderRow("red", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return chams.getHiddenRed();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    chams.setHiddenRed(Math.round(value));
                }
            }, 0f, 255f, 1f, true));
            rows.add(new SliderRow("green", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return chams.getHiddenGreen();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    chams.setHiddenGreen(Math.round(value));
                }
            }, 0f, 255f, 1f, true));
            rows.add(new SliderRow("blue", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return chams.getHiddenBlue();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    chams.setHiddenBlue(Math.round(value));
                }
            }, 0f, 255f, 1f, true));
            rows.add(new SliderRow("alpha", new FloatGetter()
            {
                @Override
                public float get()
                {
                    return chams.getHiddenAlpha();
                }
            }, new FloatSetter()
            {
                @Override
                public void set(float value)
                {
                    chams.setHiddenAlpha(Math.round(value));
                }
            }, 0f, 255f, 1f, true));
        }

        return rows;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        long now = System.currentTimeMillis();
        float dt = lastFrameAt == 0L ? 0f : Math.min(0.05f, (now - lastFrameAt) / 1000f);
        lastFrameAt = now;

        ScaledResolution sr = new ScaledResolution(mc);
        GuiRender.setScreenMetrics(sr.getScaledHeight(), sr.getScaleFactor());
        GuiRender.enableBlend();
        GuiRender.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), OVERLAY);

        layout(sr);
        updateScroll(dt);

        drawFrame();
        drawSidebar(mouseX, mouseY);
        drawContent(sr, mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void layout(ScaledResolution sr)
    {
        rootW = Math.min(900, sr.getScaledWidth() - 80);
        rootH = Math.min(560, sr.getScaledHeight() - 70);
        rootX = (sr.getScaledWidth() - rootW) / 2;
        rootY = (sr.getScaledHeight() - rootH) / 2;

        sidebarW = 150;
        sidebarX = rootX;
        sidebarY = rootY;
        sidebarH = rootH;

        contentX = sidebarX + sidebarW + 10;
        contentY = rootY + 16;
        contentW = rootW - sidebarW - 26;
        contentH = rootH - 32;
    }

    private void updateScroll(float dt)
    {
        ScrollState scroll = scrolls.get(activeCategory);
        if (scroll != null)
        {
            scroll.update(dt);
        }
    }

    private void drawFrame()
    {
        GuiRender.drawSoftShadow(rootX, rootY, rootW, rootH, 18, SHADOW);
        GuiRender.drawRoundedBorder(rootX, rootY, rootW, rootH, 18, BORDER, PANEL);
        GuiRender.drawRoundedRect(sidebarX + 10, sidebarY + 10, sidebarW - 20, sidebarH - 20, 14, PANEL_SOFT);
        GuiRender.drawRoundedRect(contentX, contentY, contentW, contentH, 16, PANEL_ALT);
    }

    private void drawSidebar(int mouseX, int mouseY)
    {
        drawBrandBlock();
        int y = sidebarY + 126;
        for (Category category : CATEGORIES)
        {
            int x = sidebarX + 18;
            int w = sidebarW - 36;
            int h = 28;
            boolean active = category == activeCategory;
            boolean hover = hit(mouseX, mouseY, x, y, w, h);
            int bg = active ? 0x90242B34 : hover ? 0x7021272F : 0x3A1B2027;
            GuiRender.drawRoundedRect(x, y, w, h, 12, bg);
            drawText(fitTextToWidth(category.name(), w - 24, TEXT_SIZE), x + 12, getTextY(y, h, TEXT_SIZE), active ? TEXT : TEXT_MID, TEXT_SIZE, false);
            y += 36;
        }
    }

    private void drawContent(ScaledResolution sr, int mouseX, int mouseY)
    {
        GuiRender.beginScissor(sr, contentX, contentY, contentW, contentH);
        if (activeCategory == Category.CONFIGS)
        {
            drawConfigs(sr, mouseX, mouseY);
        }
        else
        {
            drawModules(sr, mouseX, mouseY);
        }
        GuiRender.endScissor();
    }

    private void drawModules(ScaledResolution sr, int mouseX, int mouseY)
    {
        List<ModulePanel> list = layoutModulePanels();
        if (list == null)
        {
            return;
        }

        int totalHeight = 0;

        for (ModulePanel panel : list)
        {
            int h = panel.getHeight();
            int y = panel.y;
            if (y + h >= contentY - 10 && y <= contentY + contentH + 10)
            {
                panel.render(mouseX, mouseY);
            }
            totalHeight += h + 10;
        }

        int max = Math.max(0, totalHeight - (contentH - 24));
        scrolls.get(activeCategory).setMax(max);
        drawScrollIndicator(contentX + contentW - 7, contentY + 14, 3, contentH - 28, scrolls.get(activeCategory), max);
    }

    private void drawConfigs(ScaledResolution sr, int mouseX, int mouseY)
    {
        int x = contentX + 18;
        int y = contentY + 16;

        String active = fitTextToWidth("active config: " + configManager.getActiveConfigName(), contentW - 36, LABEL_SIZE);
        drawText(active, x, y, TEXT, LABEL_SIZE, true);
        y += getFontHeight(LABEL_SIZE) + 10;

        int buttonW = (contentW - 60) / 4;
        int buttonH = 24;
        configButtonY = y;
        drawMiniButton(x, y, buttonW, buttonH, "save current", mouseX, mouseY);
        drawMiniButton(x + buttonW + 8, y, buttonW, buttonH, "new", mouseX, mouseY);
        drawMiniButton(x + (buttonW + 8) * 2, y, buttonW, buttonH, "reset", mouseX, mouseY);
        drawMiniButton(x + (buttonW + 8) * 3, y, buttonW, buttonH, "folder", mouseX, mouseY);
        y += buttonH + 12;

        configInputY = y;
        int inputH = 26;
        int inputW = contentW - 36;
        int inputBg = namingConfig ? 0x96222931 : 0x5A1E242B;
        GuiRender.drawRoundedRect(x, y, inputW, inputH, 12, inputBg);
        String input = newConfigName.isEmpty() ? "new config name" : newConfigName.toLowerCase(Locale.US);
        int inputColor = newConfigName.isEmpty() ? TEXT_SOFT : TEXT;
        String inputDisplay = input + (namingConfig && ((System.currentTimeMillis() / 400L) % 2L == 0L) ? "_" : "");
        drawText(fitTextToWidth(inputDisplay, inputW - 24, TEXT_SIZE), x + 12, getTextY(y, inputH, TEXT_SIZE), inputColor, TEXT_SIZE, false);
        y += inputH + 14;

        drawText("saved profiles", x, y, TEXT_MID, TEXT_SIZE, false);
        y += getFontHeight(TEXT_SIZE) + 10;

        ScrollState scroll = scrolls.get(Category.CONFIGS);
        configListY = y;
        configListH = contentY + contentH - y - 14;
        int itemY = y - Math.round(scroll.get());
        int totalHeight = 0;
        for (String name : cachedConfigs)
        {
            int itemH = 24;
            boolean activeConfig = lower(name).equals(lower(configManager.getActiveConfigName()));
            boolean hover = hit(mouseX, mouseY, x, itemY, inputW, itemH);
            int bg = activeConfig ? 0x90242B34 : hover ? 0x701F252D : 0x501A1F26;
            GuiRender.drawRoundedRect(x, itemY, inputW, itemH, 11, bg);
            drawText(fitTextToWidth(name, inputW - 24, TEXT_SIZE), x + 12, getTextY(itemY, itemH, TEXT_SIZE), activeConfig ? TEXT : TEXT_MID, TEXT_SIZE, false);
            itemY += itemH + 8;
            totalHeight += itemH + 8;
        }

        int max = Math.max(0, totalHeight - configListH);
        scroll.setMax(max);
        drawScrollIndicator(contentX + contentW - 7, configListY, 3, configListH, scroll, max);
    }

    private void drawScrollIndicator(int x, int y, int w, int h, ScrollState scroll, int max)
    {
        if (scroll == null || max <= 0 || h <= 0)
        {
            return;
        }

        float ratio = h / (float) (h + max);
        int barH = Math.max(20, Math.round(h * ratio));
        float percent = scroll.get() / Math.max(1f, max);
        int barY = y + Math.round((h - barH) * percent);
        GuiRender.drawRoundedRect(x, y, w, h, w, 0x30242A33);
        GuiRender.drawRoundedRect(x, barY, w, barH, w, ACCENT);
    }

    private void drawMiniButton(int x, int y, int w, int h, String label, int mouseX, int mouseY)
    {
        boolean hover = hit(mouseX, mouseY, x, y, w, h);
        int bg = hover ? 0x90242B34 : 0x6620272F;
        GuiRender.drawRoundedRect(x, y, w, h, 12, bg);
        String fitted = fitTextToWidth(label, w - 16, TEXT_SIZE);
        int labelX = x + (w - getTextWidth(fitted, TEXT_SIZE)) / 2;
        drawText(fitted, labelX, getTextY(y, h, TEXT_SIZE), hover ? TEXT : TEXT_MID, TEXT_SIZE, false);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException
    {
        layout(new ScaledResolution(mc));
        if (button == 0)
        {
            if (handleSidebarClick(mouseX, mouseY))
            {
                return;
            }

            if (activeCategory == Category.CONFIGS)
            {
                if (handleConfigClick(mouseX, mouseY))
                {
                    return;
                }
            }
            else
            {
                List<ModulePanel> list = layoutModulePanels();
                if (list != null)
                {
                    for (ModulePanel panel : list)
                    {
                        SliderRow slider = panel.mouseClicked(mouseX, mouseY, button);
                        if (slider != null)
                        {
                            draggingSlider = slider;
                            return;
                        }
                        if (panel.wasInteracted())
                        {
                            return;
                        }
                    }
                }
            }
        }
        else if (button == 1 && activeCategory != Category.CONFIGS)
        {
            List<ModulePanel> list = layoutModulePanels();
            if (list != null)
            {
                for (ModulePanel panel : list)
                {
                    panel.mouseClicked(mouseX, mouseY, button);
                    if (panel.wasInteracted())
                    {
                        return;
                    }
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleSidebarClick(int mouseX, int mouseY)
    {
        int y = sidebarY + 126;
        for (Category category : CATEGORIES)
        {
            int x = sidebarX + 18;
            int w = sidebarW - 36;
            int h = 28;
            if (hit(mouseX, mouseY, x, y, w, h))
            {
                activeCategory = category;
                keybindTarget = null;
                draggingSlider = null;
                namingConfig = false;
                return true;
            }
            y += 36;
        }
        return false;
    }

    private List<ModulePanel> layoutModulePanels()
    {
        List<ModulePanel> list = panels.get(activeCategory);
        if (list == null)
        {
            return null;
        }

        int x = contentX + 18;
        int y = contentY + 16 - Math.round(scrolls.get(activeCategory).get());
        int w = contentW - 36;
        for (ModulePanel panel : list)
        {
            panel.layout(x, y, w);
            y += panel.getHeight() + 10;
        }
        return list;
    }

    private boolean handleConfigClick(int mouseX, int mouseY)
    {
        int x = contentX + 18;
        int buttonW = (contentW - 60) / 4;
        int buttonH = 24;
        if (hit(mouseX, mouseY, x, configButtonY, buttonW, buttonH))
        {
            configManager.save(moduleManager);
            configDirty = false;
            refreshConfigs();
            return true;
        }
        if (hit(mouseX, mouseY, x + buttonW + 8, configButtonY, buttonW, buttonH))
        {
            namingConfig = true;
            return true;
        }
        if (hit(mouseX, mouseY, x + (buttonW + 8) * 2, configButtonY, buttonW, buttonH))
        {
            configManager.resetToDefaults(moduleManager);
            configDirty = false;
            buildPanels();
            refreshConfigs();
            return true;
        }
        if (hit(mouseX, mouseY, x + (buttonW + 8) * 3, configButtonY, buttonW, buttonH))
        {
            configManager.openConfigFolder();
            return true;
        }

        int inputW = contentW - 36;
        if (hit(mouseX, mouseY, x, configInputY, inputW, 26))
        {
            namingConfig = true;
            return true;
        }

        int itemY = configListY - Math.round(scrolls.get(Category.CONFIGS).get());
        for (String name : cachedConfigs)
        {
            if (hit(mouseX, mouseY, x, itemY, inputW, 24))
            {
                if (configManager.loadConfig(name, moduleManager))
                {
                    configDirty = false;
                    buildPanels();
                    refreshConfigs();
                }
                return true;
            }
            itemY += 32;
        }

        namingConfig = false;
        return false;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        if (draggingSlider != null)
        {
            draggingSlider.stopDrag();
            draggingSlider = null;
        }
        flushConfigIfDirty();
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick)
    {
        if (draggingSlider != null)
        {
            draggingSlider.drag(mouseX);
            markDirty();
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0)
        {
            return;
        }

        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        if (!hit(mouseX, mouseY, contentX, contentY, contentW, contentH))
        {
            return;
        }

        ScrollState scroll = scrolls.get(activeCategory);
        if (scroll != null)
        {
            scroll.add(-wheel / 3f);
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
            }
            else
            {
                keybindTarget.setKeyBind(keyCode);
            }
            markDirty();
            flushConfigIfDirty();
            keybindTarget = null;
            return;
        }

        if (activeCategory == Category.CONFIGS && namingConfig)
        {
            if (keyCode == Keyboard.KEY_ESCAPE)
            {
                namingConfig = false;
                return;
            }
            if (keyCode == Keyboard.KEY_RETURN)
            {
                createConfig();
                return;
            }
            if (keyCode == Keyboard.KEY_BACK)
            {
                if (!newConfigName.isEmpty())
                {
                    newConfigName = newConfigName.substring(0, newConfigName.length() - 1);
                }
                return;
            }
            if (typedChar >= 32 && typedChar < 127 && newConfigName.length() < 32)
            {
                newConfigName += typedChar;
                return;
            }
        }

        if (keyCode == Keyboard.KEY_ESCAPE)
        {
            mc.displayGuiScreen(null);
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    private void createConfig()
    {
        String trimmed = newConfigName.trim();
        if (trimmed.isEmpty())
        {
            return;
        }
        if (configManager.createNewConfig(trimmed, moduleManager))
        {
            configDirty = false;
            newConfigName = "";
            namingConfig = false;
            refreshConfigs();
        }
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

    private void refreshConfigs()
    {
        cachedConfigs = configManager == null ? new ArrayList<String>() : new ArrayList<String>(configManager.listConfigNames());
        Collections.sort(cachedConfigs, String.CASE_INSENSITIVE_ORDER);
    }

    private void flushConfigIfDirty()
    {
        if (!configDirty || configManager == null)
        {
            return;
        }
        configManager.save(moduleManager);
        configDirty = false;
    }

    private void markDirty()
    {
        configDirty = true;
    }

    private void drawText(String text, int x, int y, int color, int size, boolean shadow)
    {
        String display = lower(text);
        if (font != null && font.isReady())
        {
            if (shadow)
            {
                font.drawStringWithShadow(display, x, y, color, size);
            }
            else
            {
                font.drawString(display, x, y, color, size);
            }
        }
        else if (shadow)
        {
            mc.fontRendererObj.drawStringWithShadow(display, x, y, color);
        }
        else
        {
            mc.fontRendererObj.drawString(display, x, y, color);
        }
    }

    private int getTextWidth(String text, int size)
    {
        String display = lower(text);
        if (font != null && font.isReady())
        {
            return font.getStringWidth(display, size);
        }
        return mc.fontRendererObj.getStringWidth(display);
    }

    private int getTextY(int boxY, int boxH, int size)
    {
        return boxY + Math.max(0, (boxH - getFontHeight(size)) / 2) - 1;
    }

    private String fitTextToWidth(String text, int maxWidth, int size)
    {
        String display = lower(text);
        if (maxWidth <= 0)
        {
            return "";
        }
        if (getTextWidth(display, size) <= maxWidth)
        {
            return display;
        }

        String ellipsis = "...";
        int ellipsisWidth = getTextWidth(ellipsis, size);
        if (ellipsisWidth >= maxWidth)
        {
            return "";
        }

        int end = display.length();
        while (end > 0 && getTextWidth(display.substring(0, end) + ellipsis, size) > maxWidth)
        {
            end--;
        }
        return display.substring(0, end) + ellipsis;
    }

    private int getFontHeight(int size)
    {
        if (font != null && font.isReady())
        {
            return font.getFontHeight(size);
        }
        return mc.fontRendererObj.FONT_HEIGHT;
    }

    private static String lower(String text)
    {
        return text == null ? "" : text.toLowerCase(Locale.US);
    }

    private static boolean hit(int mouseX, int mouseY, int x, int y, int w, int h)
    {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private void drawBrandBlock()
    {
        int boxX = sidebarX + 18;
        int boxY = sidebarY + 18;
        int boxW = sidebarW - 36;
        int boxH = 88;
        GuiRender.drawRoundedBorder(boxX, boxY, boxW, boxH, 14, BORDER, 0x4C1A2027);

        int logoSize = 24;
        int logoX = boxX + (boxW - logoSize) / 2;
        int logoY = boxY + 10;
        drawLogo(logoX, logoY, logoSize, logoSize);

        String title = "chudware";
        String version = "v2.1.0";
        int titleX = boxX + (boxW - getTextWidth(title, TITLE_SIZE)) / 2;
        int versionX = boxX + (boxW - getTextWidth(version, TEXT_SIZE)) / 2;
        drawText(title, titleX, boxY + 40, TEXT, TITLE_SIZE, true);
        drawText(version, versionX, boxY + 58, TEXT_SOFT, TEXT_SIZE, false);
    }

    private void drawLogo(int x, int y, int w, int h)
    {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 0.95f);
        mc.getTextureManager().bindTexture(LOGO);
        drawScaledCustomSizeModalRect(x, y, 0f, 0f, w, h, w, h, w, h);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private interface BoolGetter { boolean get(); }
    private interface BoolSetter { void set(boolean value); }
    private interface FloatGetter { float get(); }
    private interface FloatSetter { void set(float value); }
    private interface IntGetter { int get(); }
    private interface IntSetter { void set(int value); }

    private abstract class Row
    {
        protected final String label;
        protected int x;
        protected int y;
        protected int w;
        protected int h;

        protected Row(String label, int h)
        {
            this.label = label;
            this.h = h;
        }

        public void layout(int x, int y, int w)
        {
            this.x = x;
            this.y = y;
            this.w = w;
        }

        public int getHeight()
        {
            return h;
        }

        public abstract void render(int mouseX, int mouseY);

        public boolean mouseClicked(int mouseX, int mouseY, int button)
        {
            return false;
        }
    }

    private final class SectionRow extends Row
    {
        private SectionRow(String label)
        {
            super(label, 20);
        }

        @Override
        public void render(int mouseX, int mouseY)
        {
            drawText(fitTextToWidth(label, w, TEXT_SIZE), x, y + 4, TEXT_SOFT, TEXT_SIZE, false);
        }
    }

    private final class ToggleRow extends Row
    {
        private final BoolGetter getter;
        private final BoolSetter setter;

        private ToggleRow(String label, BoolGetter getter, BoolSetter setter)
        {
            super(label, 24);
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public void render(int mouseX, int mouseY)
        {
            boolean value = getter.get();
            int pillW = 34;
            int pillH = 16;
            int pillX = x + w - pillW;
            int pillY = y + 4;
            drawText(fitTextToWidth(label, Math.max(20, pillX - x - 10), TEXT_SIZE), x, getTextY(y, h, TEXT_SIZE), TEXT_MID, TEXT_SIZE, false);
            GuiRender.drawRoundedBorder(pillX, pillY, pillW, pillH, 8, value ? BORDER_STRONG : BORDER, value ? 0x90242B34 : TRACK);
            int knobX = value ? pillX + pillW - 14 : pillX + 2;
            GuiRender.drawRoundedRect(knobX, pillY + 2, 12, 12, 6, value ? ACCENT : TEXT_SOFT);
        }

        @Override
        public boolean mouseClicked(int mouseX, int mouseY, int button)
        {
            if (button != 0 || !hit(mouseX, mouseY, x, y, w, h))
            {
                return false;
            }
            setter.set(!getter.get());
            markDirty();
            return true;
        }
    }

    private final class CycleRow extends Row
    {
        private final String[] options;
        private final IntGetter getter;
        private final IntSetter setter;

        private CycleRow(String label, String[] options, IntGetter getter, IntSetter setter)
        {
            super(label, 24);
            this.options = options;
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public void render(int mouseX, int mouseY)
        {
            int valueSpace = Math.max(20, w / 2);
            drawText(fitTextToWidth(label, Math.max(20, w - valueSpace - 14), TEXT_SIZE), x, getTextY(y, h, TEXT_SIZE), TEXT_MID, TEXT_SIZE, false);
            String value = options.length == 0 ? "none" : options[Math.max(0, Math.min(options.length - 1, getter.get()))];
            String fitted = fitTextToWidth(value, valueSpace, TEXT_SIZE);
            int valueW = getTextWidth(fitted, TEXT_SIZE) + 12;
            int px = x + w - valueW;
            GuiRender.drawRoundedBorder(px, y + 4, valueW, 16, 8, BORDER, TRACK);
            drawText(fitted, px + 6, getTextY(y + 4, 16, TEXT_SIZE), TEXT, TEXT_SIZE, false);
        }

        @Override
        public boolean mouseClicked(int mouseX, int mouseY, int button)
        {
            if (button != 0 || !hit(mouseX, mouseY, x, y, w, h) || options.length == 0)
            {
                return false;
            }
            int next = getter.get() + 1;
            if (next >= options.length)
            {
                next = 0;
            }
            setter.set(next);
            markDirty();
            return true;
        }
    }

    private final class SliderRow extends Row
    {
        private final FloatGetter getter;
        private final FloatSetter setter;
        private final float min;
        private final float max;
        private final float step;
        private final boolean integer;
        private boolean dragging;

        private SliderRow(String label, FloatGetter getter, FloatSetter setter, float min, float max, float step, boolean integer)
        {
            super(label, 34);
            this.getter = getter;
            this.setter = setter;
            this.min = min;
            this.max = max;
            this.step = step;
            this.integer = integer;
        }

        @Override
        public void render(int mouseX, int mouseY)
        {
            float value = getter.get();
            String valueText = format(value);
            String fittedValue = fitTextToWidth(valueText, Math.max(20, w / 3), TEXT_SIZE);
            int valueX = x + w - getTextWidth(fittedValue, TEXT_SIZE);
            drawText(fitTextToWidth(label, Math.max(20, valueX - x - 12), TEXT_SIZE), x, y + 2, TEXT_MID, TEXT_SIZE, false);
            drawText(fittedValue, valueX, y + 2, TEXT, TEXT_SIZE, false);

            int trackY = y + 20;
            int trackH = 8;
            GuiRender.drawRoundedRect(x, trackY, w, trackH, 4, TRACK);
            float pct = GuiRender.clamp((value - min) / (max - min), 0f, 1f);
            int fillW = Math.max(0, Math.round(w * pct));
            if (fillW > 0)
            {
                GuiRender.drawRoundedRect(x, trackY, fillW, trackH, 4, ACCENT);
            }
            int knobX = x + fillW - 5;
            GuiRender.drawRoundedRect(Math.max(x, Math.min(x + w - 10, knobX)), trackY - 1, 10, 10, 5, TEXT);
        }

        @Override
        public boolean mouseClicked(int mouseX, int mouseY, int button)
        {
            if (button != 0 || !hit(mouseX, mouseY, x, y, w, h))
            {
                return false;
            }
            dragging = true;
            drag(mouseX);
            markDirty();
            return true;
        }

        private void drag(int mouseX)
        {
            float pct = GuiRender.clamp((mouseX - x) / (float) w, 0f, 1f);
            float value = min + (max - min) * pct;
            if (step > 0f)
            {
                value = Math.round(value / step) * step;
            }
            if (integer)
            {
                value = Math.round(value);
            }
            value = GuiRender.clamp(value, min, max);
            setter.set(value);
        }

        private void stopDrag()
        {
            dragging = false;
        }

        private String format(float value)
        {
            if (integer)
            {
                return String.valueOf(Math.round(value));
            }
            return String.format(Locale.US, "%.1f", value);
        }
    }

    private final class KeybindRow extends Row
    {
        private final Module module;

        private KeybindRow(Module module)
        {
            super("keybind", 24);
            this.module = module;
        }

        @Override
        public void render(int mouseX, int mouseY)
        {
            int valueSpace = Math.max(20, w / 2);
            drawText(fitTextToWidth(label, Math.max(20, w - valueSpace - 14), TEXT_SIZE), x, getTextY(y, h, TEXT_SIZE), TEXT_MID, TEXT_SIZE, false);
            String value = keybindTarget == module ? "press key" : keyName(module.getKeyBind());
            String fitted = fitTextToWidth(value, valueSpace, TEXT_SIZE);
            int valueW = getTextWidth(fitted, TEXT_SIZE) + 12;
            int px = x + w - valueW;
            GuiRender.drawRoundedBorder(px, y + 4, valueW, 16, 8, keybindTarget == module ? BORDER_STRONG : BORDER, TRACK);
            drawText(fitted, px + 6, getTextY(y + 4, 16, TEXT_SIZE), keybindTarget == module ? TEXT : TEXT_MID, TEXT_SIZE, false);
        }

        @Override
        public boolean mouseClicked(int mouseX, int mouseY, int button)
        {
            if (button != 0 || !hit(mouseX, mouseY, x, y, w, h))
            {
                return false;
            }
            keybindTarget = module;
            return true;
        }

        private String keyName(int key)
        {
            if (key <= Keyboard.KEY_NONE)
            {
                return "none";
            }
            String name = Keyboard.getKeyName(key);
            return name == null ? "unknown" : lower(name);
        }
    }

    private final class HotbarRow extends Row
    {
        private final AutoPotModule module;

        private HotbarRow(AutoPotModule module)
        {
            super("hotbar slots", 42);
            this.module = module;
        }

        @Override
        public void render(int mouseX, int mouseY)
        {
            drawText(fitTextToWidth(label, w, TEXT_SIZE), x, y + 2, TEXT_MID, TEXT_SIZE, false);
            int slots = module.getHotbarSlotCount();
            int gap = 4;
            int top = y + 18;
            int boxW = (w - gap * (slots - 1)) / slots;
            for (int i = 0; i < slots; i++)
            {
                int bx = x + i * (boxW + gap);
                boolean enabled = module.isSlotEnabled(i);
                GuiRender.drawRoundedBorder(bx, top, boxW, 18, 8, enabled ? BORDER_STRONG : BORDER, enabled ? 0x90242B34 : TRACK);
                String index = String.valueOf(i + 1);
                drawText(index, bx + (boxW - getTextWidth(index, TEXT_SIZE)) / 2, getTextY(top, 18, TEXT_SIZE), enabled ? TEXT : TEXT_SOFT, TEXT_SIZE, false);
            }
        }

        @Override
        public boolean mouseClicked(int mouseX, int mouseY, int button)
        {
            if (button != 0 || !hit(mouseX, mouseY, x, y, w, h))
            {
                return false;
            }
            int slots = module.getHotbarSlotCount();
            int gap = 4;
            int top = y + 18;
            int boxW = (w - gap * (slots - 1)) / slots;
            for (int i = 0; i < slots; i++)
            {
                int bx = x + i * (boxW + gap);
                if (hit(mouseX, mouseY, bx, top, boxW, 18))
                {
                    module.setSlotEnabled(i, !module.isSlotEnabled(i));
                    markDirty();
                    return true;
                }
            }
            return false;
        }
    }

    private final class ModulePanel
    {
        private final Module module;
        private final List<Row> rows;
        private boolean expanded;
        private boolean interacted;
        private int x;
        private int y;
        private int w;

        private ModulePanel(Module module, List<Row> rows)
        {
            this.module = module;
            this.rows = rows;
        }

        private void layout(int x, int y, int w)
        {
            this.x = x;
            this.y = y;
            this.w = w;
        }

        private int getHeight()
        {
            int total = 30;
            if (expanded)
            {
                total += 8;
                for (Row row : rows)
                {
                    total += row.getHeight() + 4;
                }
                total += 6;
            }
            return total;
        }

        private void render(int mouseX, int mouseY)
        {
            int h = getHeight();
            boolean hover = hit(mouseX, mouseY, x, y, w, 30);
            int fill = module.isEnabled() ? 0x90242B34 : hover ? 0x7A20262F : 0x621A2028;
            GuiRender.drawRoundedRect(x, y, w, h, 14, fill);

            String state = module.isEnabled() ? "on" : "off";
            int stateW = getTextWidth(state, TEXT_SIZE) + 12;
            int stateX = x + w - stateW - 12;
            GuiRender.drawRoundedBorder(stateX, y + 7, stateW, 16, 8, module.isEnabled() ? BORDER_STRONG : BORDER, TRACK);
            drawText(state, stateX + 6, getTextY(y + 7, 16, TEXT_SIZE), module.isEnabled() ? TEXT : TEXT_SOFT, TEXT_SIZE, false);
            String fittedName = fitTextToWidth(module.getName(), Math.max(20, stateX - x - 28), LABEL_SIZE);
            drawText(fittedName, x + 12, getTextY(y, 30, LABEL_SIZE), module.isEnabled() ? TEXT : TEXT_MID, LABEL_SIZE, false);

            if (!expanded)
            {
                return;
            }

            int rowY = y + 36;
            for (Row row : rows)
            {
                row.layout(x + 14, rowY, w - 28);
                row.render(mouseX, mouseY);
                rowY += row.getHeight() + 4;
            }
        }

        private SliderRow mouseClicked(int mouseX, int mouseY, int button)
        {
            interacted = false;
            if (!hit(mouseX, mouseY, x, y, w, getHeight()))
            {
                return null;
            }

            if (hit(mouseX, mouseY, x, y, w, 30))
            {
                if (button == 0)
                {
                    module.toggle();
                    markDirty();
                    interacted = true;
                }
                else if (button == 1)
                {
                    expanded = !expanded;
                    interacted = true;
                }
                return null;
            }

            if (!expanded || button != 0)
            {
                return null;
            }

            for (Row row : rows)
            {
                if (row.mouseClicked(mouseX, mouseY, button))
                {
                    interacted = true;
                    if (row instanceof SliderRow)
                    {
                        return (SliderRow) row;
                    }
                    return null;
                }
            }
            return null;
        }

        private boolean wasInteracted()
        {
            return interacted;
        }
    }
}
