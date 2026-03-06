package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.ChudWare;
import com.lethalfart.ChudWare.eventbus.annotations.EventTarget;
import com.lethalfart.ChudWare.eventbus.impl.misc.TickEvent;
import com.lethalfart.ChudWare.ui.ModuleGuiScreen;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.unix.X11;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoClickerHandler
{
    private static final int LLMHF_INJECTED = 0x00000001;
    private static final int VK_LBUTTON = 0x01;
    private static final int WM_LBUTTONDOWN = 0x0201;
    private static final int WM_LBUTTONUP = 0x0202;
    private final Random random = new Random();
    private final AutoClickerModule autoClickerModule;
    private final Robot robot;
    private final boolean windowsPlatform;
    private final boolean macPlatform;
    private final boolean linuxPlatform;
    private final AtomicBoolean physicalLmbDown = new AtomicBoolean(false);
    private volatile boolean hookReady = false;
    private WinUser.HHOOK mouseHook;
    private WinUser.HOOKPROC mouseProc;
    private Thread hookThread;
    private Thread clickThread;
    private boolean autoDisabledForInventory = false;
    private X11.Display linuxDisplay;
    private boolean linuxDisplayAttempted;

    public AutoClickerHandler(AutoClickerModule autoClickerModule)
    {
        this.autoClickerModule = autoClickerModule;
        this.robot = createRobot();
        this.windowsPlatform = isWindows();
        this.macPlatform = isMac();
        this.linuxPlatform = isLinux();
        if (windowsPlatform)
        {
            installMouseHook();
        }
        startClickThread();
        ChudWare.EVENT_MANAGER.register(this);
    }

    private void startClickThread()
    {
        clickThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while (!Thread.currentThread().isInterrupted())
                {
                    try
                    {
                        if (shouldClick())
                        {
                            float minCps = autoClickerModule.getMinCps();
                            float maxCps = autoClickerModule.getMaxCps();
                            if (maxCps < minCps)
                            {
                                float swap = minCps;
                                minCps = maxCps;
                                maxCps = swap;
                            }

                            float midCps = minCps + ((maxCps - minCps) * 0.5f);
                            float range = (maxCps - minCps) * 0.5f;
                            float cps = midCps + (float)(random.nextGaussian() * range * 0.35f);
                            cps = Math.max(minCps, Math.min(maxCps, cps));

                            long baseDelay = (long)(1000.0F / cps);
                            long jitter;
                            if (random.nextFloat() < 0.85f)
                            {
                                jitter = (long)(random.nextGaussian() * 12);
                            }
                            else
                            {
                                jitter = (long)(random.nextFloat() * 60 + 15);
                            }
                            long delay = Math.max(1L, baseDelay + jitter);

                            sendAutoClick(Minecraft.getMinecraft());
                            Thread.sleep(delay);
                        }
                        else
                        {
                            Thread.sleep(5);
                        }
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }, "ChudWare-AutoClicker");
        clickThread.setDaemon(true);
        clickThread.start();
    }

    private boolean shouldClick()
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (!autoClickerModule.isEnabled()) return false;
        if (windowsPlatform && robot == null) return false;
        if (mc == null || !Display.isActive()) return false;
        if (mc.currentScreen instanceof ModuleGuiScreen) return false;
        if (mc.currentScreen instanceof GuiContainer) return false;
        return isLMBPhysicallyDown();
    }

    
    @EventTarget
    public void onTick(TickEvent event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;

        if (mc.currentScreen instanceof GuiContainer)
        {
            if (autoClickerModule.isEnabled())
            {
                autoClickerModule.setEnabled(false);
                autoDisabledForInventory = true;
            }
            return;
        }
        if (autoDisabledForInventory && mc.currentScreen == null)
        {
            autoClickerModule.setEnabled(true);
            autoDisabledForInventory = false;
        }
    }

    private boolean isLMBPhysicallyDown()
    {
        if (!windowsPlatform)
        {
            return Mouse.isButtonDown(0);
        }
        if (hookReady)
        {
            return physicalLmbDown.get();
        }
        try
        {
            return (User32.INSTANCE.GetAsyncKeyState(VK_LBUTTON) & 0x8000) != 0;
        }
        catch (Throwable ignored)
        {
            return Mouse.isButtonDown(0);
        }
    }

    private void sendHardwareMouseClick()
    {
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    private void sendAutoClick(Minecraft mc)
    {
        if (windowsPlatform)
        {
            if (robot == null) return;
            sendHardwareMouseClick();
            return;
        }
        if (macPlatform || linuxPlatform)
        {
            if (sendNativeMouseClick()) return;
            if (robot != null) sendHardwareMouseClick();
        }
    }

    private boolean sendNativeMouseClick()
    {
        if (macPlatform) return sendMacNativeClick();
        if (linuxPlatform) return sendLinuxNativeClick();
        return false;
    }

    private boolean sendMacNativeClick()
    {
        Pointer baseEvent = null;
        Pointer downEvent = null;
        Pointer upEvent = null;
        try
        {
            baseEvent = Quartz.INSTANCE.CGEventCreate(null);
            if (baseEvent == null) return false;
            CGPoint.ByValue location = Quartz.INSTANCE.CGEventGetLocation(baseEvent);
            downEvent = Quartz.INSTANCE.CGEventCreateMouseEvent(null, Quartz.KCG_EVENT_LEFT_MOUSE_DOWN, location, Quartz.KCG_MOUSE_BUTTON_LEFT);
            upEvent = Quartz.INSTANCE.CGEventCreateMouseEvent(null, Quartz.KCG_EVENT_LEFT_MOUSE_UP, location, Quartz.KCG_MOUSE_BUTTON_LEFT);
            if (downEvent == null || upEvent == null) return false;
            Quartz.INSTANCE.CGEventPost(Quartz.KCG_HID_EVENT_TAP, downEvent);
            Quartz.INSTANCE.CGEventPost(Quartz.KCG_HID_EVENT_TAP, upEvent);
            return true;
        }
        catch (Throwable ignored)
        {
            return false;
        }
        finally
        {
            if (downEvent != null) Quartz.INSTANCE.CFRelease(downEvent);
            if (upEvent != null) Quartz.INSTANCE.CFRelease(upEvent);
            if (baseEvent != null) Quartz.INSTANCE.CFRelease(baseEvent);
        }
    }

    private boolean sendLinuxNativeClick()
    {
        try
        {
            X11.Display display = getLinuxDisplay();
            if (display == null) return false;
            int leftButton = 1;
            if (XTest.INSTANCE.XTestFakeButtonEvent(display, leftButton, true, new NativeLong(0)) == 0) return false;
            if (XTest.INSTANCE.XTestFakeButtonEvent(display, leftButton, false, new NativeLong(0)) == 0) return false;
            X11.INSTANCE.XFlush(display);
            return true;
        }
        catch (Throwable ignored)
        {
            return false;
        }
    }

    private X11.Display getLinuxDisplay()
    {
        if (linuxDisplay != null) return linuxDisplay;
        if (linuxDisplayAttempted) return null;
        linuxDisplayAttempted = true;
        try
        {
            linuxDisplay = X11.INSTANCE.XOpenDisplay(null);
        }
        catch (Throwable ignored)
        {
            linuxDisplay = null;
        }
        return linuxDisplay;
    }

    private Robot createRobot()
    {
        try
        {
            return new Robot();
        }
        catch (AWTException ignored)
        {
            return null;
        }
    }

    private void installMouseHook()
    {
        mouseProc = new WinUser.HOOKPROC()
        {
            public WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, WinDef.LPARAM lParam)
            {
                if (nCode >= 0)
                {
                    int msg = wParam.intValue();
                    int flags = 0;
                    try
                    {
                        com.sun.jna.Pointer p = new com.sun.jna.Pointer(lParam.longValue());
                        flags = p.getInt(12);
                    }
                    catch (Throwable ignored) {}

                    boolean injected = (flags & LLMHF_INJECTED) != 0;
                    if (!injected)
                    {
                        if (msg == WM_LBUTTONDOWN) physicalLmbDown.set(true);
                        else if (msg == WM_LBUTTONUP) physicalLmbDown.set(false);
                    }
                }
                return User32.INSTANCE.CallNextHookEx(mouseHook, nCode, wParam, lParam);
            }
        };

        hookThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                mouseHook = User32.INSTANCE.SetWindowsHookEx(WinUser.WH_MOUSE_LL, mouseProc, null, 0);
                if (mouseHook == null) return;
                hookReady = true;

                WinUser.MSG msg = new WinUser.MSG();
                while (User32.INSTANCE.GetMessage(msg, null, 0, 0) != 0)
                {
                    User32.INSTANCE.TranslateMessage(msg);
                    User32.INSTANCE.DispatchMessage(msg);
                }
                User32.INSTANCE.UnhookWindowsHookEx(mouseHook);
                hookReady = false;
            }
        }, "ChudWare-MouseHook");
        hookThread.setDaemon(true);
        hookThread.start();
    }

    private boolean isWindows()
    {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("win");
    }

    private boolean isMac()
    {
        String osName = System.getProperty("os.name");
        if (osName == null) return false;
        String lower = osName.toLowerCase();
        return lower.contains("mac") || lower.contains("darwin");
    }

    private boolean isLinux()
    {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("linux");
    }

    private interface XTest extends Library
    {
        XTest INSTANCE = (XTest) Native.load("Xtst", XTest.class);
        int XTestFakeButtonEvent(X11.Display display, int button, boolean isPress, NativeLong delay);
    }

    private interface Quartz extends Library
    {
        Quartz INSTANCE = (Quartz) Native.load("ApplicationServices", Quartz.class);
        int KCG_HID_EVENT_TAP = 0;
        int KCG_EVENT_LEFT_MOUSE_DOWN = 1;
        int KCG_EVENT_LEFT_MOUSE_UP = 2;
        int KCG_MOUSE_BUTTON_LEFT = 0;

        Pointer CGEventCreate(Pointer source);
        CGPoint.ByValue CGEventGetLocation(Pointer event);
        Pointer CGEventCreateMouseEvent(Pointer source, int mouseType, CGPoint.ByValue mouseCursorPosition, int mouseButton);
        void CGEventPost(int tap, Pointer event);
        void CFRelease(Pointer pointer);
    }

    public static class CGPoint extends Structure
    {
        public double x;
        public double y;

        @Override
        protected List<String> getFieldOrder()
        {
            return Arrays.asList("x", "y");
        }

        public static class ByValue extends CGPoint implements Structure.ByValue {}
    }
}