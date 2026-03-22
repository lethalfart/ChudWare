package com.lethalfart.ChudWare.config;

import com.lethalfart.ChudWare.module.Module;
import com.lethalfart.ChudWare.module.ModuleManager;
import com.lethalfart.ChudWare.module.impl.AimAssistModule;
import com.lethalfart.ChudWare.module.impl.AutoBridgeModule;
import com.lethalfart.ChudWare.module.impl.AutoClickerModule;
import com.lethalfart.ChudWare.module.impl.AutoPotModule;
import com.lethalfart.ChudWare.module.impl.ChamsModule;
import com.lethalfart.ChudWare.module.impl.ESPModule;
import com.lethalfart.ChudWare.module.impl.RefillModule;
import com.lethalfart.ChudWare.module.impl.RightClickerModule;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConfigManager
{
    private static final String EXT = ".properties";
    private final File configDirectory;
    private final Object saveStateLock = new Object();
    private final ExecutorService saveExecutor;
    private final Map<String, String> persistedFingerprints = new HashMap<String, String>();
    private final Map<String, String> queuedFingerprints = new HashMap<String, String>();
    private final Map<String, String> persistedStateHashes = new HashMap<String, String>();
    private final Map<String, String> queuedStateHashes = new HashMap<String, String>();
    private File activeConfigFile;
    private Properties defaultProperties;

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
        saveExecutor = Executors.newSingleThreadExecutor(new java.util.concurrent.ThreadFactory()
        {
            @Override
            public Thread newThread(Runnable runnable)
            {
                Thread thread = new Thread(runnable, "ChudWare-ConfigSave");
                thread.setDaemon(true);
                return thread;
            }
        });
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
        if (moduleManager == null)
        {
            return;
        }
        ensureDefaultProperties(moduleManager);
        if (!activeConfigFile.exists())
        {
            savePropertiesToFile(activeConfigFile, copyProperties(defaultProperties));
            rememberPersistedStateHash(activeConfigFile, computeStateHash(moduleManager));
            return;
        }
        loadFromFile(activeConfigFile, moduleManager);
    }

    public void save(ModuleManager moduleManager)
    {
        saveToFile(activeConfigFile, moduleManager, true);
    }

    public void saveBlocking(ModuleManager moduleManager)
    {
        saveToFile(activeConfigFile, moduleManager, false);
    }

    public void flushPendingSaves()
    {
        final CountDownLatch latch = new CountDownLatch(1);
        try
        {
            saveExecutor.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    latch.countDown();
                }
            });
            latch.await();
        }
        catch (InterruptedException ignored)
        {
            Thread.currentThread().interrupt();
        }
    }

    public boolean saveAs(String configName, ModuleManager moduleManager)
    {
        File file = resolveConfigFile(configName);
        if (file == null || moduleManager == null)
        {
            return false;
        }
        boolean ok = saveToFile(file, moduleManager, false);
        if (ok)
        {
            activeConfigFile = file;
        }
        return ok;
    }

    public boolean createNewConfig(String configName, ModuleManager moduleManager)
    {
        File file = resolveConfigFile(configName);
        if (file == null || moduleManager == null || file.exists())
        {
            return false;
        }

        boolean saved = saveToFile(file, moduleManager, false);
        if (saved)
        {
            activeConfigFile = file;
        }
        return saved;
    }

    public boolean loadConfig(String configName, ModuleManager moduleManager)
    {
        File file = resolveConfigFile(configName);
        if (file == null || !file.exists() || moduleManager == null)
        {
            return false;
        }
        if (!file.equals(activeConfigFile))
        {
            save(moduleManager);
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

        ensureDefaultProperties(moduleManager);
        applyProperties(copyProperties(defaultProperties), moduleManager);
        disableAllModules(moduleManager);
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

        ensureDefaultProperties(moduleManager);
        applyProperties(copyProperties(defaultProperties), moduleManager);
        applyProperties(props, moduleManager);
        rememberLoadedState(configFile, props);
        rememberPersistedStateHash(configFile, computeStateHash(moduleManager));
        return true;
    }

    private void applyProperties(Properties props, ModuleManager moduleManager)
    {
        for (Module module : moduleManager.getModules())
        {
            String moduleKey = "module." + module.getName() + ".";
            if (module instanceof AutoBridgeModule)
            {
                String legacyModuleKey = "module.Auto Birgde.";
                if (!hasModuleData(props, moduleKey) && hasModuleData(props, legacyModuleKey))
                {
                    moduleKey = legacyModuleKey;
                }
            }
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
                autoClicker.setInventoryFill(parseBoolean(props.getProperty(moduleKey + "inventoryFill"), autoClicker.isInventoryFill()));
                autoClicker.setWeaponOnly(parseBoolean(props.getProperty(moduleKey + "weaponOnly"), autoClicker.isWeaponOnly()));
                autoClicker.setBreakBlocks(parseBoolean(props.getProperty(moduleKey + "breakBlocks"), autoClicker.isBreakBlocks()));
            }
            else if (module instanceof RightClickerModule)
            {
                RightClickerModule rightClicker = (RightClickerModule) module;
                String minRaw = props.getProperty(moduleKey + "minCps");
                String maxRaw = props.getProperty(moduleKey + "maxCps");
                float fallbackCps = parseFloat(props.getProperty(moduleKey + "cps"), rightClicker.getCps());
                if (minRaw == null && maxRaw == null)
                {
                    rightClicker.setCps(fallbackCps);
                }
                else
                {
                    rightClicker.setMinCps(parseFloat(minRaw, fallbackCps));
                    rightClicker.setMaxCps(parseFloat(maxRaw, fallbackCps));
                }
            }
            else if (module instanceof AutoPotModule)
            {
                AutoPotModule autoPot = (AutoPotModule) module;
                autoPot.setActionDelay(parseInt(props.getProperty(moduleKey + "actionDelay"), autoPot.getActionDelay()));
                for (int slot = 0; slot < autoPot.getHotbarSlotCount(); slot++)
                {
                    autoPot.setSlotEnabled(slot, parseBoolean(props.getProperty(moduleKey + "slot" + slot), autoPot.isSlotEnabled(slot)));
                }
            }
            else if (module instanceof AutoBridgeModule)
            {
            }
            else if (module instanceof AimAssistModule)
            {
                AimAssistModule aimAssist = (AimAssistModule) module;
                aimAssist.setSpeed1(parseDouble(props.getProperty(moduleKey + "speed1"), aimAssist.getSpeed1()));
                aimAssist.setSpeed2(parseDouble(props.getProperty(moduleKey + "speed2"), aimAssist.getSpeed2()));
                aimAssist.setFov(parseDouble(props.getProperty(moduleKey + "fov"), aimAssist.getFov()));
                aimAssist.setDistance(parseDouble(props.getProperty(moduleKey + "distance"), aimAssist.getDistance()));
                aimAssist.setClickAim(parseBoolean(props.getProperty(moduleKey + "clickAim"), aimAssist.isClickAim()));
                aimAssist.setBreakBlocks(parseBoolean(props.getProperty(moduleKey + "breakBlocks"), aimAssist.isBreakBlocks()));
                aimAssist.setIgnoreFriends(parseBoolean(props.getProperty(moduleKey + "ignoreFriends"), aimAssist.isIgnoreFriends()));
                aimAssist.setWeaponOnly(parseBoolean(props.getProperty(moduleKey + "weaponOnly"), aimAssist.isWeaponOnly()));
                aimAssist.setAimInvis(parseBoolean(props.getProperty(moduleKey + "aimInvis"), aimAssist.isAimInvis()));
                aimAssist.setBlatantMode(parseBoolean(props.getProperty(moduleKey + "blatantMode"), aimAssist.isBlatantMode()));
                aimAssist.setIgnoreNaked(parseBoolean(props.getProperty(moduleKey + "ignoreNaked"), aimAssist.isIgnoreNaked()));
                aimAssist.setMiddleClickFriends(parseBoolean(props.getProperty(moduleKey + "middleClickFriends"), aimAssist.isMiddleClickFriends()));
                String friendRaw = props.getProperty(moduleKey + "friends");
                if (friendRaw != null)
                {
                    AimAssistModule.setFriends(Arrays.asList(friendRaw.split(",")));
                }
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
                velocity.setBufferTicks(parseInt(props.getProperty(moduleKey + "bufferTicks"), velocity.getBufferTicks()));
            }
            else if (module instanceof RefillModule)
            {
                RefillModule refill = (RefillModule) module;
                refill.setSoup(parseBoolean(props.getProperty(moduleKey + "soup"), refill.isSoup()));
                refill.setPotion(parseBoolean(props.getProperty(moduleKey + "potion"), refill.isPotion()));
                refill.setUseNonHealthPotions(parseBoolean(props.getProperty(moduleKey + "useNonHealthPotions"), refill.isUseNonHealthPotions()));
                refill.setSimulateButton(parseInt(props.getProperty(moduleKey + "simulateButton"), refill.getSimulateButton()));
                refill.setDelayAfterOpen(parseInt(props.getProperty(moduleKey + "delayAfterOpen"), refill.getDelayAfterOpen()));
                refill.setDelayBeforeClose(parseInt(props.getProperty(moduleKey + "delayBeforeClose"), refill.getDelayBeforeClose()));
                refill.setSpeed(parseFloat(props.getProperty(moduleKey + "speed"), refill.getSpeed()));
                refill.setSmartSpeed(parseBoolean(props.getProperty(moduleKey + "smartSpeed"), refill.isSmartSpeed()));
            }
            else if (module instanceof ESPModule)
            {
                ESPModule esp = (ESPModule) module;
                esp.setRed(parseInt(
                        props.getProperty(moduleKey + "red", props.getProperty(moduleKey + "colorRed")),
                        esp.getRed()
                ));
                esp.setGreen(parseInt(
                        props.getProperty(moduleKey + "green", props.getProperty(moduleKey + "colorGreen")),
                        esp.getGreen()
                ));
                esp.setBlue(parseInt(
                        props.getProperty(moduleKey + "blue", props.getProperty(moduleKey + "colorBlue")),
                        esp.getBlue()
                ));
                esp.setAlpha(parseInt(props.getProperty(moduleKey + "alpha"), esp.getAlpha()));
                esp.setTargetPlayers(parseBoolean(props.getProperty(moduleKey + "targetPlayers"), esp.isTargetPlayers()));
                esp.setTargetAnimals(parseBoolean(props.getProperty(moduleKey + "targetAnimals"), esp.isTargetAnimals()));
                esp.setTargetMobs(parseBoolean(props.getProperty(moduleKey + "targetMobs"), esp.isTargetMobs()));
                esp.setTargetPassives(parseBoolean(props.getProperty(moduleKey + "targetPassives"), esp.isTargetPassives()));
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
                chams.setPulseSpeed(parseFloat(
                        props.getProperty(moduleKey + "pulseSpeed"),
                        parseFloat(props.getProperty(moduleKey + "animationSpeed"), chams.getPulseSpeed())
                ));
                chams.setRainbow(parseBoolean(props.getProperty(moduleKey + "rainbow"), chams.isRainbow()));
                chams.setColored(parseBoolean(props.getProperty(moduleKey + "colored"), chams.isColored()));
                chams.setPulse(parseBoolean(props.getProperty(moduleKey + "pulse"), chams.isPulse()));
                chams.setOnlyTargets(parseBoolean(props.getProperty(moduleKey + "onlyTargets"), chams.isOnlyTargets()));
                chams.setMaterial(parseBoolean(props.getProperty(moduleKey + "material"), chams.isMaterial()));
                chams.setTargetPlayers(parseBoolean(props.getProperty(moduleKey + "targetPlayers"), chams.isTargetPlayers()));
                chams.setTargetAnimals(parseBoolean(props.getProperty(moduleKey + "targetAnimals"), chams.isTargetAnimals()));
                chams.setTargetMobs(parseBoolean(props.getProperty(moduleKey + "targetMobs"), chams.isTargetMobs()));
                chams.setTargetPassives(parseBoolean(props.getProperty(moduleKey + "targetPassives"), chams.isTargetPassives()));
            }
        }
    }

    private boolean saveToFile(File configFile, ModuleManager moduleManager, boolean async)
    {
        if (moduleManager == null || configFile == null)
        {
            return false;
        }
        String stateHash = computeStateHash(moduleManager);
        if (shouldSkipStateSave(configFile, stateHash))
        {
            return true;
        }
        Properties snapshot = createProperties(moduleManager);
        return async
                ? queueSavePropertiesToFile(configFile, snapshot, stateHash)
                : savePropertiesToFile(configFile, snapshot, stateHash);
    }

    private boolean savePropertiesToFile(File configFile, Properties props)
    {
        return savePropertiesToFile(configFile, props, null);
    }

    private boolean savePropertiesToFile(File configFile, Properties props, String stateHash)
    {
        if (props == null || configFile == null)
        {
            return false;
        }
        String fingerprint = fingerprint(props);
        if (shouldSkipWrite(configFile, fingerprint))
        {
            return true;
        }
        return writePropertiesToFile(configFile, props, fingerprint, stateHash);
    }

    private boolean writePropertiesToFile(File configFile, Properties props, String fingerprint, String stateHash)
    {
        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs())
        {
            return false;
        }

        FileOutputStream out = null;
        try
        {
            out = new FileOutputStream(configFile);
            props.store(out, "ChudWare config");
            rememberPersistedFingerprint(configFile, fingerprint);
            rememberPersistedStateHash(configFile, stateHash != null ? stateHash : fingerprint);
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

    private boolean queueSavePropertiesToFile(final File configFile, Properties props, final String stateHash)
    {
        if (props == null || configFile == null)
        {
            return false;
        }

        final Properties snapshot = copyProperties(props);
        final String fingerprint = fingerprint(snapshot);
        final String path = fingerprintKey(configFile);

        synchronized (saveStateLock)
        {
            if (shouldSkipWriteLocked(configFile, fingerprint))
            {
                return true;
            }
            queuedFingerprints.put(path, fingerprint);
            queuedStateHashes.put(path, stateHash);
        }

        saveExecutor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                boolean ok = writePropertiesToFile(configFile, snapshot, fingerprint, stateHash);
                synchronized (saveStateLock)
                {
                    String queued = queuedFingerprints.get(path);
                    if (fingerprint.equals(queued))
                    {
                        queuedFingerprints.remove(path);
                    }
                    String queuedState = queuedStateHashes.get(path);
                    if (stateHash.equals(queuedState))
                    {
                        queuedStateHashes.remove(path);
                    }
                    if (!ok && !fingerprint.equals(queuedFingerprints.get(path)))
                    {
                        queuedFingerprints.remove(path);
                    }
                    if (!ok && !stateHash.equals(queuedStateHashes.get(path)))
                    {
                        queuedStateHashes.remove(path);
                    }
                }
            }
        });
        return true;
    }

    private Properties createProperties(ModuleManager moduleManager)
    {
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
                props.setProperty(moduleKey + "inventoryFill", String.valueOf(autoClicker.isInventoryFill()));
                props.setProperty(moduleKey + "weaponOnly", String.valueOf(autoClicker.isWeaponOnly()));
                props.setProperty(moduleKey + "breakBlocks", String.valueOf(autoClicker.isBreakBlocks()));
            }
            else if (module instanceof RightClickerModule)
            {
                RightClickerModule rightClicker = (RightClickerModule) module;
                props.setProperty(moduleKey + "cps", String.valueOf(rightClicker.getCps()));
                props.setProperty(moduleKey + "minCps", String.valueOf(rightClicker.getMinCps()));
                props.setProperty(moduleKey + "maxCps", String.valueOf(rightClicker.getMaxCps()));
            }
            else if (module instanceof AutoPotModule)
            {
                AutoPotModule autoPot = (AutoPotModule) module;
                props.setProperty(moduleKey + "actionDelay", String.valueOf(autoPot.getActionDelay()));
                for (int slot = 0; slot < autoPot.getHotbarSlotCount(); slot++)
                {
                    props.setProperty(moduleKey + "slot" + slot, String.valueOf(autoPot.isSlotEnabled(slot)));
                }
            }
            else if (module instanceof AutoBridgeModule)
            {
            }
            else if (module instanceof AimAssistModule)
            {
                AimAssistModule aimAssist = (AimAssistModule) module;
                props.setProperty(moduleKey + "speed1", String.valueOf(aimAssist.getSpeed1()));
                props.setProperty(moduleKey + "speed2", String.valueOf(aimAssist.getSpeed2()));
                props.setProperty(moduleKey + "fov", String.valueOf(aimAssist.getFov()));
                props.setProperty(moduleKey + "distance", String.valueOf(aimAssist.getDistance()));
                props.setProperty(moduleKey + "clickAim", String.valueOf(aimAssist.isClickAim()));
                props.setProperty(moduleKey + "breakBlocks", String.valueOf(aimAssist.isBreakBlocks()));
                props.setProperty(moduleKey + "ignoreFriends", String.valueOf(aimAssist.isIgnoreFriends()));
                props.setProperty(moduleKey + "weaponOnly", String.valueOf(aimAssist.isWeaponOnly()));
                props.setProperty(moduleKey + "aimInvis", String.valueOf(aimAssist.isAimInvis()));
                props.setProperty(moduleKey + "blatantMode", String.valueOf(aimAssist.isBlatantMode()));
                props.setProperty(moduleKey + "ignoreNaked", String.valueOf(aimAssist.isIgnoreNaked()));
                props.setProperty(moduleKey + "middleClickFriends", String.valueOf(aimAssist.isMiddleClickFriends()));
                props.setProperty(moduleKey + "friends", String.join(",", AimAssistModule.getFriends()));
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
                props.setProperty(moduleKey + "bufferTicks", String.valueOf(velocity.getBufferTicks()));
            }
            else if (module instanceof RefillModule)
            {
                RefillModule refill = (RefillModule) module;
                props.setProperty(moduleKey + "soup", String.valueOf(refill.isSoup()));
                props.setProperty(moduleKey + "potion", String.valueOf(refill.isPotion()));
                props.setProperty(moduleKey + "useNonHealthPotions", String.valueOf(refill.isUseNonHealthPotions()));
                props.setProperty(moduleKey + "simulateButton", String.valueOf(refill.getSimulateButton()));
                props.setProperty(moduleKey + "delayAfterOpen", String.valueOf(refill.getDelayAfterOpen()));
                props.setProperty(moduleKey + "delayBeforeClose", String.valueOf(refill.getDelayBeforeClose()));
                props.setProperty(moduleKey + "speed", String.valueOf(refill.getSpeed()));
                props.setProperty(moduleKey + "smartSpeed", String.valueOf(refill.isSmartSpeed()));
            }
            else if (module instanceof ESPModule)
            {
                ESPModule esp = (ESPModule) module;
                props.setProperty(moduleKey + "red", String.valueOf(esp.getRed()));
                props.setProperty(moduleKey + "green", String.valueOf(esp.getGreen()));
                props.setProperty(moduleKey + "blue", String.valueOf(esp.getBlue()));
                props.setProperty(moduleKey + "alpha", String.valueOf(esp.getAlpha()));
                props.setProperty(moduleKey + "targetPlayers", String.valueOf(esp.isTargetPlayers()));
                props.setProperty(moduleKey + "targetAnimals", String.valueOf(esp.isTargetAnimals()));
                props.setProperty(moduleKey + "targetMobs", String.valueOf(esp.isTargetMobs()));
                props.setProperty(moduleKey + "targetPassives", String.valueOf(esp.isTargetPassives()));
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
                props.setProperty(moduleKey + "pulseSpeed", String.valueOf(chams.getPulseSpeed()));
                props.setProperty(moduleKey + "rainbow", String.valueOf(chams.isRainbow()));
                props.setProperty(moduleKey + "colored", String.valueOf(chams.isColored()));
                props.setProperty(moduleKey + "pulse", String.valueOf(chams.isPulse()));
                props.setProperty(moduleKey + "onlyTargets", String.valueOf(chams.isOnlyTargets()));
                props.setProperty(moduleKey + "material", String.valueOf(chams.isMaterial()));
                props.setProperty(moduleKey + "targetPlayers", String.valueOf(chams.isTargetPlayers()));
                props.setProperty(moduleKey + "targetAnimals", String.valueOf(chams.isTargetAnimals()));
                props.setProperty(moduleKey + "targetMobs", String.valueOf(chams.isTargetMobs()));
                props.setProperty(moduleKey + "targetPassives", String.valueOf(chams.isTargetPassives()));
            }
        }
        return props;
    }

    private void ensureDefaultProperties(ModuleManager moduleManager)
    {
        if (defaultProperties == null && moduleManager != null)
        {
            defaultProperties = createProperties(moduleManager);
        }
    }

    private Properties copyProperties(Properties source)
    {
        Properties copy = new Properties();
        if (source != null)
        {
            copy.putAll(source);
        }
        return copy;
    }

    private void rememberLoadedState(File configFile, Properties loadedProps)
    {
        Properties canonical = copyProperties(defaultProperties);
        if (loadedProps != null)
        {
            canonical.putAll(loadedProps);
        }
        rememberPersistedFingerprint(configFile, fingerprint(canonical));
    }

    private void rememberPersistedFingerprint(File configFile, String fingerprint)
    {
        synchronized (saveStateLock)
        {
            persistedFingerprints.put(fingerprintKey(configFile), fingerprint);
        }
    }

    private void rememberPersistedStateHash(File configFile, String stateHash)
    {
        synchronized (saveStateLock)
        {
            persistedStateHashes.put(fingerprintKey(configFile), stateHash);
        }
    }

    private boolean shouldSkipStateSave(File configFile, String stateHash)
    {
        synchronized (saveStateLock)
        {
            if (configFile == null || stateHash == null || !configFile.exists())
            {
                return false;
            }

            String path = fingerprintKey(configFile);
            if (stateHash.equals(queuedStateHashes.get(path)))
            {
                return true;
            }
            return stateHash.equals(persistedStateHashes.get(path));
        }
    }

    private boolean shouldSkipWrite(File configFile, String fingerprint)
    {
        synchronized (saveStateLock)
        {
            return shouldSkipWriteLocked(configFile, fingerprint);
        }
    }

    private boolean shouldSkipWriteLocked(File configFile, String fingerprint)
    {
        if (configFile == null || fingerprint == null || !configFile.exists())
        {
            return false;
        }

        String path = fingerprintKey(configFile);
        if (fingerprint.equals(queuedFingerprints.get(path)))
        {
            return true;
        }
        return fingerprint.equals(persistedFingerprints.get(path));
    }

    private String fingerprint(Properties props)
    {
        List<String> names = new ArrayList<String>();
        if (props != null)
        {
            names.addAll(props.stringPropertyNames());
        }
        Collections.sort(names);

        StringBuilder builder = new StringBuilder();
        for (String name : names)
        {
            builder.append(name).append('=').append(props.getProperty(name, "")).append('\n');
        }
        return builder.toString();
    }

    private String fingerprintKey(File configFile)
    {
        return configFile.getAbsolutePath();
    }

    private String computeStateHash(ModuleManager moduleManager)
    {
        if (moduleManager == null)
        {
            return null;
        }

        StringBuilder builder = new StringBuilder(1024);
        List<String> friendNames = null;
        for (Module module : moduleManager.getModules())
        {
            appendState(builder, module.getName());
            appendState(builder, module.isEnabled());
            appendState(builder, module.getKeyBind());

            if (module instanceof AutoClickerModule)
            {
                AutoClickerModule autoClicker = (AutoClickerModule) module;
                appendState(builder, autoClicker.getCps());
                appendState(builder, autoClicker.getMinCps());
                appendState(builder, autoClicker.getMaxCps());
                appendState(builder, autoClicker.isInventoryFill());
                appendState(builder, autoClicker.isWeaponOnly());
                appendState(builder, autoClicker.isBreakBlocks());
            }
            else if (module instanceof RightClickerModule)
            {
                RightClickerModule rightClicker = (RightClickerModule) module;
                appendState(builder, rightClicker.getCps());
                appendState(builder, rightClicker.getMinCps());
                appendState(builder, rightClicker.getMaxCps());
            }
            else if (module instanceof AutoPotModule)
            {
                AutoPotModule autoPot = (AutoPotModule) module;
                appendState(builder, autoPot.getActionDelay());
                for (int slot = 0; slot < autoPot.getHotbarSlotCount(); slot++)
                {
                    appendState(builder, autoPot.isSlotEnabled(slot));
                }
            }
            else if (module instanceof AimAssistModule)
            {
                AimAssistModule aimAssist = (AimAssistModule) module;
                appendState(builder, aimAssist.getSpeed1());
                appendState(builder, aimAssist.getSpeed2());
                appendState(builder, aimAssist.getFov());
                appendState(builder, aimAssist.getDistance());
                appendState(builder, aimAssist.isClickAim());
                appendState(builder, aimAssist.isBreakBlocks());
                appendState(builder, aimAssist.isIgnoreFriends());
                appendState(builder, aimAssist.isWeaponOnly());
                appendState(builder, aimAssist.isAimInvis());
                appendState(builder, aimAssist.isBlatantMode());
                appendState(builder, aimAssist.isIgnoreNaked());
                appendState(builder, aimAssist.isMiddleClickFriends());
                if (friendNames == null)
                {
                    friendNames = new ArrayList<String>(AimAssistModule.getFriends());
                    Collections.sort(friendNames);
                }
                for (String friend : friendNames)
                {
                    appendState(builder, friend);
                }
            }
            else if (module instanceof VelocityModule)
            {
                VelocityModule velocity = (VelocityModule) module;
                appendState(builder, velocity.getChance());
                appendState(builder, velocity.getHorizontalPercent());
                appendState(builder, velocity.getVerticalPercent());
                appendState(builder, velocity.isNoWater());
                appendState(builder, velocity.isNoLava());
                appendState(builder, velocity.isNoLadder());
                appendState(builder, velocity.isBufferMode());
                appendState(builder, velocity.getBufferTicks());
            }
            else if (module instanceof RefillModule)
            {
                RefillModule refill = (RefillModule) module;
                appendState(builder, refill.isSoup());
                appendState(builder, refill.isPotion());
                appendState(builder, refill.isUseNonHealthPotions());
                appendState(builder, refill.getSimulateButton());
                appendState(builder, refill.getDelayAfterOpen());
                appendState(builder, refill.getDelayBeforeClose());
                appendState(builder, refill.getSpeed());
                appendState(builder, refill.isSmartSpeed());
            }
            else if (module instanceof ESPModule)
            {
                ESPModule esp = (ESPModule) module;
                appendState(builder, esp.getRed());
                appendState(builder, esp.getGreen());
                appendState(builder, esp.getBlue());
                appendState(builder, esp.getAlpha());
                appendState(builder, esp.isTargetPlayers());
                appendState(builder, esp.isTargetAnimals());
                appendState(builder, esp.isTargetMobs());
                appendState(builder, esp.isTargetPassives());
            }
            else if (module instanceof ChamsModule)
            {
                ChamsModule chams = (ChamsModule) module;
                appendState(builder, chams.getVisibleRed());
                appendState(builder, chams.getVisibleGreen());
                appendState(builder, chams.getVisibleBlue());
                appendState(builder, chams.getVisibleAlpha());
                appendState(builder, chams.getHiddenRed());
                appendState(builder, chams.getHiddenGreen());
                appendState(builder, chams.getHiddenBlue());
                appendState(builder, chams.getHiddenAlpha());
                appendState(builder, chams.getPulseSpeed());
                appendState(builder, chams.isRainbow());
                appendState(builder, chams.isColored());
                appendState(builder, chams.isPulse());
                appendState(builder, chams.isOnlyTargets());
                appendState(builder, chams.isMaterial());
                appendState(builder, chams.isTargetPlayers());
                appendState(builder, chams.isTargetAnimals());
                appendState(builder, chams.isTargetMobs());
                appendState(builder, chams.isTargetPassives());
            }
        }
        return Integer.toHexString(builder.toString().hashCode());
    }

    private void appendState(StringBuilder builder, boolean value)
    {
        builder.append(value ? '1' : '0').append('|');
    }

    private void appendState(StringBuilder builder, int value)
    {
        builder.append(value).append('|');
    }

    private void appendState(StringBuilder builder, float value)
    {
        builder.append(Float.floatToIntBits(value)).append('|');
    }

    private void appendState(StringBuilder builder, double value)
    {
        builder.append(Double.doubleToLongBits(value)).append('|');
    }

    private void appendState(StringBuilder builder, String value)
    {
        builder.append(value != null ? value : "").append('|');
    }

    private void disableAllModules(ModuleManager moduleManager)
    {
        for (Module module : moduleManager.getModules())
        {
            module.setEnabled(false);
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

    private boolean hasModuleData(Properties props, String moduleKey)
    {
        return props.containsKey(moduleKey + "enabled")
                || props.containsKey(moduleKey + "keyBind")
                || props.containsKey(moduleKey + "waitTimeMs")
                || props.containsKey(moduleKey + "setLook")
                || props.containsKey(moduleKey + "activateOnShift")
                || props.containsKey(moduleKey + "sneakOnly")
                || props.containsKey(moduleKey + "assistRange");
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
