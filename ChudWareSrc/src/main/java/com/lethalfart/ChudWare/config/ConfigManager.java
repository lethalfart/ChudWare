package com.lethalfart.ChudWare.config;

import com.lethalfart.ChudWare.module.Module;
import com.lethalfart.ChudWare.module.ModuleManager;
import com.lethalfart.ChudWare.module.impl.AimAssistModule;
import com.lethalfart.ChudWare.module.impl.AutoClickerModule;
import com.lethalfart.ChudWare.module.impl.ChamsModule;
import com.lethalfart.ChudWare.module.impl.ESPModule;
import com.lethalfart.ChudWare.module.impl.ReachModule;
import com.lethalfart.ChudWare.module.impl.VelocityModule;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ConfigManager
{
    private static final String EXT = ".properties";
    private final File configDirectory;
    private File activeConfigFile;

    public ConfigManager()
    {
        Minecraft mc = Minecraft.getMinecraft();
        File baseDir = mc != null && mc.mcDataDir != null ? mc.mcDataDir : new File(".");
        File configDir = new File(baseDir, "config");
        if (!configDir.exists())
        {
            configDir.mkdirs();
        }
        configDirectory = new File(configDir, "ChudWareConfigs");
        if (!configDirectory.exists())
        {
            configDirectory.mkdirs();
        }
        activeConfigFile = new File(configDirectory, "default" + EXT);
    }

    public File getConfigDirectory()
    {
        return configDirectory;
    }

    public String getActiveConfigName()
    {
        return stripExtension(activeConfigFile.getName());
    }

    public List<String> listConfigNames()
    {
        File[] files = configDirectory.listFiles();
        if (files == null || files.length == 0)
        {
            return new ArrayList<>();
        }

        Arrays.sort(files);
        List<String> names = new ArrayList<>(files.length);
        for (File file : files)
        {
            if (file.isFile() && file.getName().endsWith(EXT))
            {
                names.add(stripExtension(file.getName()));
            }
        }
        return names;
    }

    public void load(ModuleManager moduleManager)
    {
        loadFromFile(activeConfigFile, moduleManager);
    }

    public void save(ModuleManager moduleManager)
    {
        saveToFile(activeConfigFile, moduleManager);
    }

    public boolean saveAs(String configName, ModuleManager moduleManager)
    {
        File file = resolveConfigFile(configName);
        if (file == null || moduleManager == null)
        {
            return false;
        }
        boolean ok = saveToFile(file, moduleManager);
        if (ok)
        {
            activeConfigFile = file;
        }
        return ok;
    }

    public boolean loadConfig(String configName, ModuleManager moduleManager)
    {
        File file = resolveConfigFile(configName);
        if (file == null || !file.exists() || moduleManager == null)
        {
            return false;
        }
        boolean ok = loadFromFile(file, moduleManager);
        if (ok)
        {
            activeConfigFile = file;
        }
        return ok;
    }

    public void resetToDefaults(ModuleManager moduleManager)
    {
        if (moduleManager == null)
        {
            return;
        }
        for (Module module : moduleManager.getModules())
        {
            module.setEnabled(false);
        }
        save(moduleManager);
    }

    public void openConfigFolder()
    {
        try
        {
            if (Desktop.isDesktopSupported())
            {
                Desktop.getDesktop().open(configDirectory);
                return;
            }
        }
        catch (Exception ignored)
        {
        }

        try
        {
            Runtime.getRuntime().exec(new String[]{"explorer.exe", configDirectory.getAbsolutePath()});
        }
        catch (Exception ignored)
        {
        }
    }

    private File resolveConfigFile(String configName)
    {
        if (configName == null)
        {
            return null;
        }
        String normalized = configName.trim();
        if (normalized.isEmpty())
        {
            return null;
        }
        normalized = normalized.replaceAll("[^A-Za-z0-9_\\- ]", "_");
        if (!normalized.toLowerCase().endsWith(EXT))
        {
            normalized = normalized + EXT;
        }
        return new File(configDirectory, normalized);
    }

    private String stripExtension(String fileName)
    {
        if (fileName.toLowerCase().endsWith(EXT))
        {
            return fileName.substring(0, fileName.length() - EXT.length());
        }
        return fileName;
    }

    private boolean loadFromFile(File configFile, ModuleManager moduleManager)
    {
        if (moduleManager == null || configFile == null || !configFile.exists())
        {
            return false;
        }

        Properties props = new Properties();
        FileInputStream in = null;
        try
        {
            in = new FileInputStream(configFile);
            props.load(in);
        }
        catch (IOException ignored)
        {
            return false;
        }
        finally
        {
            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (IOException ignored)
                {
                }
            }
        }

        for (Module module : moduleManager.getModules())
        {
            String moduleKey = "module." + module.getName() + ".";
            String enabledRaw = props.getProperty(moduleKey + "enabled");
            if (enabledRaw != null)
            {
                module.setEnabled(Boolean.parseBoolean(enabledRaw));
            }
            module.setKeyBind(parseKeyBind(props.getProperty(moduleKey + "keyBind"), module.getKeyBind()));

            if (module instanceof AutoClickerModule)
            {
                AutoClickerModule autoClicker = (AutoClickerModule) module;
                String minRaw = props.getProperty(moduleKey + "minCps");
                String maxRaw = props.getProperty(moduleKey + "maxCps");
                float fallbackCps = parseFloat(props.getProperty(moduleKey + "cps"), autoClicker.getCps());
                if (minRaw == null && maxRaw == null)
                {
                    autoClicker.setCps(fallbackCps);
                }
                else
                {
                    autoClicker.setMinCps(parseFloat(minRaw, fallbackCps));
                    autoClicker.setMaxCps(parseFloat(maxRaw, fallbackCps));
                }
            }
            else if (module instanceof ReachModule)
            {
                ReachModule reach = (ReachModule) module;
                reach.setReachMax((float) parseFloat(props.getProperty(moduleKey + "reach"), reach.getReachMax()));
            }
            else if (module instanceof AimAssistModule)
            {
                AimAssistModule aimAssist = (AimAssistModule) module;
                aimAssist.setSpeed(parseInt(props.getProperty(moduleKey + "speed"), aimAssist.getSpeed()));
                aimAssist.setSmoothness(parseInt(props.getProperty(moduleKey + "smoothness"), aimAssist.getSmoothness()));
                aimAssist.setMaxDistance(parseDouble(props.getProperty(moduleKey + "maxDistance"), aimAssist.getMaxDistance()));
                aimAssist.setOnClickOnly(parseBoolean(props.getProperty(moduleKey + "onClickOnly"), aimAssist.isOnClickOnly()));
            }
            else if (module instanceof VelocityModule)
            {
                VelocityModule velocity = (VelocityModule) module;
                velocity.setChance(parseInt(props.getProperty(moduleKey + "chance"), velocity.getChance()));
                velocity.setHorizontalPercent(parseDouble(props.getProperty(moduleKey + "horizontal"), velocity.getHorizontalPercent()));
                velocity.setVerticalPercent(parseDouble(props.getProperty(moduleKey + "vertical"), velocity.getVerticalPercent()));
                velocity.setNoWater(parseBoolean(props.getProperty(moduleKey + "noWater"), velocity.isNoWater()));
                velocity.setNoLava(parseBoolean(props.getProperty(moduleKey + "noLava"), velocity.isNoLava()));
                velocity.setNoLadder(parseBoolean(props.getProperty(moduleKey + "noLadder"), velocity.isNoLadder()));
                velocity.setBufferMode(parseBoolean(props.getProperty(moduleKey + "bufferMode"), velocity.isBufferMode()));
            }
            else if (module instanceof ESPModule)
            {
                ESPModule esp = (ESPModule) module;
                esp.setLineWidth(parseInt(props.getProperty(moduleKey + "lineWidth"), esp.getLineWidth()));
                esp.setMaxDistance(parseInt(props.getProperty(moduleKey + "maxDistance"), esp.getMaxDistance()));
                esp.setRed(parseInt(props.getProperty(moduleKey + "red"), esp.getRed()));
                esp.setGreen(parseInt(props.getProperty(moduleKey + "green"), esp.getGreen()));
                esp.setBlue(parseInt(props.getProperty(moduleKey + "blue"), esp.getBlue()));
                esp.setAlpha(parseInt(props.getProperty(moduleKey + "alpha"), esp.getAlpha()));
                esp.setShowInvisible(parseBoolean(props.getProperty(moduleKey + "showInvisible"), esp.isShowInvisible()));
            }
            else if (module instanceof ChamsModule)
            {
                ChamsModule chams = (ChamsModule) module;
                chams.setVisibleRed(parseInt(props.getProperty(moduleKey + "visibleRed"), chams.getVisibleRed()));
                chams.setVisibleGreen(parseInt(props.getProperty(moduleKey + "visibleGreen"), chams.getVisibleGreen()));
                chams.setVisibleBlue(parseInt(props.getProperty(moduleKey + "visibleBlue"), chams.getVisibleBlue()));
                chams.setVisibleAlpha(parseInt(props.getProperty(moduleKey + "visibleAlpha"), chams.getVisibleAlpha()));
                chams.setHiddenRed(parseInt(props.getProperty(moduleKey + "hiddenRed"), chams.getHiddenRed()));
                chams.setHiddenGreen(parseInt(props.getProperty(moduleKey + "hiddenGreen"), chams.getHiddenGreen()));
                chams.setHiddenBlue(parseInt(props.getProperty(moduleKey + "hiddenBlue"), chams.getHiddenBlue()));
                chams.setHiddenAlpha(parseInt(props.getProperty(moduleKey + "hiddenAlpha"), chams.getHiddenAlpha()));
            }
        }
        return true;
    }

    private boolean saveToFile(File configFile, ModuleManager moduleManager)
    {
        if (moduleManager == null || configFile == null)
        {
            return false;
        }

        Properties props = new Properties();
        for (Module module : moduleManager.getModules())
        {
            String moduleKey = "module." + module.getName() + ".";
            props.setProperty(moduleKey + "enabled", String.valueOf(module.isEnabled()));
            props.setProperty(moduleKey + "keyBind", String.valueOf(module.getKeyBind()));

            if (module instanceof AutoClickerModule)
            {
                AutoClickerModule autoClicker = (AutoClickerModule) module;
                props.setProperty(moduleKey + "cps", String.valueOf(autoClicker.getCps()));
                props.setProperty(moduleKey + "minCps", String.valueOf(autoClicker.getMinCps()));
                props.setProperty(moduleKey + "maxCps", String.valueOf(autoClicker.getMaxCps()));
            }
            else if (module instanceof ReachModule)
            {
                ReachModule reach = (ReachModule) module;
                props.setProperty(moduleKey + "reach", String.valueOf(reach.getReach()));
            }
            else if (module instanceof AimAssistModule)
            {
                AimAssistModule aimAssist = (AimAssistModule) module;
                props.setProperty(moduleKey + "speed", String.valueOf(aimAssist.getSpeed()));
                props.setProperty(moduleKey + "smoothness", String.valueOf(aimAssist.getSmoothness()));
                props.setProperty(moduleKey + "maxDistance", String.valueOf(aimAssist.getMaxDistance()));
                props.setProperty(moduleKey + "onClickOnly", String.valueOf(aimAssist.isOnClickOnly()));
            }
            else if (module instanceof VelocityModule)
            {
                VelocityModule velocity = (VelocityModule) module;
                props.setProperty(moduleKey + "chance", String.valueOf(velocity.getChance()));
                props.setProperty(moduleKey + "horizontal", String.valueOf(velocity.getHorizontalPercent()));
                props.setProperty(moduleKey + "vertical", String.valueOf(velocity.getVerticalPercent()));
                props.setProperty(moduleKey + "noWater", String.valueOf(velocity.isNoWater()));
                props.setProperty(moduleKey + "noLava", String.valueOf(velocity.isNoLava()));
                props.setProperty(moduleKey + "noLadder", String.valueOf(velocity.isNoLadder()));
                props.setProperty(moduleKey + "bufferMode", String.valueOf(velocity.isBufferMode()));
            }
            else if (module instanceof ESPModule)
            {
                ESPModule esp = (ESPModule) module;
                props.setProperty(moduleKey + "lineWidth", String.valueOf(esp.getLineWidth()));
                props.setProperty(moduleKey + "maxDistance", String.valueOf(esp.getMaxDistance()));
                props.setProperty(moduleKey + "red", String.valueOf(esp.getRed()));
                props.setProperty(moduleKey + "green", String.valueOf(esp.getGreen()));
                props.setProperty(moduleKey + "blue", String.valueOf(esp.getBlue()));
                props.setProperty(moduleKey + "alpha", String.valueOf(esp.getAlpha()));
                props.setProperty(moduleKey + "showInvisible", String.valueOf(esp.isShowInvisible()));
            }
            else if (module instanceof ChamsModule)
            {
                ChamsModule chams = (ChamsModule) module;
                props.setProperty(moduleKey + "visibleRed", String.valueOf(chams.getVisibleRed()));
                props.setProperty(moduleKey + "visibleGreen", String.valueOf(chams.getVisibleGreen()));
                props.setProperty(moduleKey + "visibleBlue", String.valueOf(chams.getVisibleBlue()));
                props.setProperty(moduleKey + "visibleAlpha", String.valueOf(chams.getVisibleAlpha()));
                props.setProperty(moduleKey + "hiddenRed", String.valueOf(chams.getHiddenRed()));
                props.setProperty(moduleKey + "hiddenGreen", String.valueOf(chams.getHiddenGreen()));
                props.setProperty(moduleKey + "hiddenBlue", String.valueOf(chams.getHiddenBlue()));
                props.setProperty(moduleKey + "hiddenAlpha", String.valueOf(chams.getHiddenAlpha()));
            }
        }

        FileOutputStream out = null;
        try
        {
            out = new FileOutputStream(configFile);
            props.store(out, "ChudWare config");
            return true;
        }
        catch (IOException ignored)
        {
            return false;
        }
        finally
        {
            if (out != null)
            {
                try
                {
                    out.close();
                }
                catch (IOException ignored)
                {
                }
            }
        }
    }

    private int parseInt(String value, int fallback)
    {
        if (value == null)
        {
            return fallback;
        }
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException ignored)
        {
            return fallback;
        }
    }

    private double parseDouble(String value, double fallback)
    {
        if (value == null)
        {
            return fallback;
        }
        try
        {
            return Double.parseDouble(value);
        }
        catch (NumberFormatException ignored)
        {
            return fallback;
        }
    }

    private float parseFloat(String value, float fallback)
    {
        if (value == null)
        {
            return fallback;
        }
        try
        {
            return Float.parseFloat(value);
        }
        catch (NumberFormatException ignored)
        {
            return fallback;
        }
    }

    private boolean parseBoolean(String value, boolean fallback)
    {
        if (value == null)
        {
            return fallback;
        }
        return Boolean.parseBoolean(value);
    }

    private int parseKeyBind(String value, int fallback)
    {
        int parsed = parseInt(value, fallback);
        if (parsed < Keyboard.KEY_NONE)
        {
            return Keyboard.KEY_NONE;
        }
        return parsed;
    }

}
