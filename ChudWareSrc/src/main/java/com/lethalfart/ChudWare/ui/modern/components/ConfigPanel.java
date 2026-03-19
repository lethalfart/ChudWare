package com.lethalfart.ChudWare.ui.modern.components;

import com.lethalfart.ChudWare.config.ConfigManager;
import com.lethalfart.ChudWare.module.ModuleManager;
import com.lethalfart.ChudWare.ui.ModernFontRenderer;
import com.lethalfart.ChudWare.ui.modern.GuiAnim;
import com.lethalfart.ChudWare.ui.modern.GuiCallbacks;
import com.lethalfart.ChudWare.ui.modern.GuiContext;
import com.lethalfart.ChudWare.ui.modern.GuiRender;
import com.lethalfart.ChudWare.ui.modern.GuiTheme;
import com.lethalfart.ChudWare.ui.modern.ScrollState;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigPanel
{
    private final ConfigManager configManager;
    private final ModuleManager moduleManager;
    private final List<String> configs = new ArrayList<>();
    private final ScrollState scroll = new ScrollState();
    private final Map<String, GuiAnim.SmoothFloat> hover = new HashMap<>();

    private boolean entering = false;
    private String input = "";
    private String status = "";
    private long statusUntil = 0L;
    private long nextRefresh = 0L;

    private int lastX;
    private int lastY;
    private int lastW;
    private int lastH;

    public ConfigPanel(ConfigManager configManager, ModuleManager moduleManager)
    {
        this.configManager = configManager;
        this.moduleManager = moduleManager;
    }

    public void render(GuiContext ctx, GuiCallbacks cb, int x, int y, int w, int h)
    {
        lastX = x;
        lastY = y;
        lastW = w;
        lastH = h;

        refresh(false);
        scroll.update(ctx.delta);

        int cx = x + GuiTheme.PADDING;
        int cw = w - GuiTheme.PADDING * 2;
        int cy = y + GuiTheme.PADDING;

        int lh = ctx.font.getFontHeight(ModernFontRenderer.SIZE_SMALL);
        String dir = configManager != null ? "Dir: " + configManager.getConfigDirectory().getName() : "Dir: Unavailable";
        ctx.font.drawString(dir, cx, cy, GuiTheme.TEXT, ModernFontRenderer.SIZE_SMALL);
        cy += lh + 8;

        int buttonH = 22;
        int gap = 6;
        int bw = (cw - gap * 2) / 3;

        drawButton(ctx, "Open Folder", cx, cy, bw, buttonH, () -> {
            if (configManager != null) configManager.openConfigFolder();
            setStatus("Opened folder");
        });
        drawButton(ctx, "Create New", cx + bw + gap, cy, bw, buttonH, this::beginEntry);
        drawButton(ctx, "Reset", cx + (bw + gap) * 2, cy, bw, buttonH, () -> {
            if (configManager != null) configManager.resetToDefaults(moduleManager);
            cb.clearConfigDirty();
            setStatus("Reset done");
            refresh(true);
        });
        cy += buttonH + 10;

        if (entering)
        {
            int inputH = 22;
            int iw = cw - 120;
            GuiRender.drawRoundedBorder(cx, cy, iw, inputH, GuiTheme.RADIUS_S, GuiTheme.ACCENT, GuiTheme.TRACK);
            boolean blink = (ctx.now / 400L) % 2L == 0L;
            String shown = input.isEmpty() ? "Type name..." : input + (blink ? "_" : "");
            ctx.font.drawString(shown, cx + 6, cy + 6, GuiTheme.TEXT, ModernFontRenderer.SIZE_SMALL);

            drawButton(ctx, "Save", cx + iw + 6, cy, 54, inputH, this::saveEntry);
            drawButton(ctx, "Cancel", cx + iw + 66, cy, 54, inputH, this::cancelEntry);
            cy += inputH + 10;
        }

        if (!status.isEmpty() && ctx.now < statusUntil)
        {
            ctx.font.drawString(status, cx, cy, GuiTheme.ACCENT_2, ModernFontRenderer.SIZE_SMALL);
            cy += lh + 6;
        }

        ctx.font.drawString("Saved profiles", cx, cy, GuiTheme.TEXT, ModernFontRenderer.SIZE_SMALL);
        cy += lh + 6;

        int listY = cy;
        int listH = y + h - GuiTheme.PADDING - listY;
        if (listH <= 0) return;

        int contentH = configs.size() * (GuiTheme.ROW_H - 6 + 6);
        scroll.setMax(Math.max(0, contentH - listH));

        GuiRender.beginScissor(ctx.sr, x, listY, w, listH);
        int itemY = listY - Math.round(scroll.get());
        for (String name : configs)
        {
            int itemH = GuiTheme.ROW_H - 6;
            boolean isHover = ctx.mouseX >= cx && ctx.mouseX <= cx + cw && ctx.mouseY >= itemY && ctx.mouseY <= itemY + itemH;
            GuiAnim.SmoothFloat ha = hover.computeIfAbsent("cfg:" + name, k -> new GuiAnim.SmoothFloat(0f, 16f));
            ha.setTarget(isHover ? 1f : 0f);
            ha.update(ctx.delta);

            int bg = GuiRender.lerpColor(GuiTheme.CARD_BG, GuiTheme.CARD_HOVER, ha.get());
            GuiRender.drawRoundedRect(cx, itemY, cw, itemH, GuiTheme.RADIUS_S, bg);
            int nameH = ctx.font.getFontHeight(ModernFontRenderer.SIZE_NORMAL);
            int nameY = itemY + (itemH - nameH) / 2;
            ctx.font.drawString(name, cx + 10, nameY, GuiTheme.TEXT, ModernFontRenderer.SIZE_NORMAL);

            itemY += itemH + 6;
        }
        GuiRender.endScissor();

        drawScrollBar(ctx, x + w - 6, listY, 4, listH, scroll.get(), scrollMaxSafe(contentH, listH));
    }

    private void drawButton(GuiContext ctx, String label, int x, int y, int w, int h, Runnable action)
    {
        boolean isHover = ctx.hit(x, y, w, h);
        GuiAnim.SmoothFloat ha = hover.computeIfAbsent("btn:" + label, k -> new GuiAnim.SmoothFloat(0f, 16f));
        ha.setTarget(isHover ? 1f : 0f);
        ha.update(ctx.delta);

        int bg = GuiRender.lerpColor(GuiTheme.CARD_BG, GuiTheme.CARD_HOVER, ha.get());
        GuiRender.drawRoundedBorder(x, y, w, h, GuiTheme.RADIUS_S, GuiTheme.BORDER, bg);
        int tx = x + (w - ctx.font.getStringWidth(label, ModernFontRenderer.SIZE_SMALL)) / 2;
        int ty = y + (h - ctx.font.getFontHeight(ModernFontRenderer.SIZE_SMALL)) / 2;
        ctx.font.drawString(label, tx, ty, GuiTheme.TEXT, ModernFontRenderer.SIZE_SMALL);
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

    public void mouseClicked(GuiContext ctx, GuiCallbacks cb, int button)
    {
        if (button != 0) return;
        int x = lastX;
        int y = lastY;
        int w = lastW;
        int h = lastH;

        int cx = x + GuiTheme.PADDING;
        int cw = w - GuiTheme.PADDING * 2;
        int cy = y + GuiTheme.PADDING;
        int lh = ctx.font.getFontHeight(ModernFontRenderer.SIZE_SMALL);
        cy += lh + 8;

        int buttonH = 22;
        int gap = 6;
        int bw = (cw - gap * 2) / 3;

        if (ctx.hit(cx, cy, bw, buttonH))
        {
            if (configManager != null) configManager.openConfigFolder();
            setStatus("Opened folder");
            return;
        }
        if (ctx.hit(cx + bw + gap, cy, bw, buttonH))
        {
            beginEntry();
            return;
        }
        if (ctx.hit(cx + (bw + gap) * 2, cy, bw, buttonH))
        {
            if (configManager != null) configManager.resetToDefaults(moduleManager);
            cb.clearConfigDirty();
            setStatus("Reset done");
            refresh(true);
            return;
        }
        cy += buttonH + 10;

        if (entering)
        {
            int inputH = 22;
            int iw = cw - 120;
            if (ctx.hit(cx + iw + 6, cy, 54, inputH))
            {
                saveEntry();
                return;
            }
            if (ctx.hit(cx + iw + 66, cy, 54, inputH))
            {
                cancelEntry();
                return;
            }
            cy += inputH + 10;
        }

        if (!status.isEmpty() && ctx.now < statusUntil)
        {
            cy += lh + 6;
        }
        cy += lh + 6;

        int listY = cy;
        int listH = y + h - GuiTheme.PADDING - listY;
        if (listH <= 0) return;

        int itemY = listY - Math.round(scroll.get());
        int itemH = GuiTheme.ROW_H - 6;
        for (String name : configs)
        {
            if (ctx.mouseY >= itemY && ctx.mouseY <= itemY + itemH
                    && ctx.mouseX >= cx && ctx.mouseX <= cx + cw)
            {
                if (configManager != null)
                {
                    boolean ok = configManager.loadConfig(name, moduleManager);
                    if (ok)
                    {
                        cb.clearConfigDirty();
                    }
                    setStatus(ok ? "Loaded: " + name : "Failed: " + name);
                }
                return;
            }
            itemY += itemH + 6;
        }
    }

    public void handleScroll(float delta)
    {
        scroll.add(delta);
    }

    public boolean handleKeyTyped(char c, int key)
    {
        if (!entering) return false;
        if (key == Keyboard.KEY_ESCAPE)
        {
            cancelEntry();
            return true;
        }
        if (key == Keyboard.KEY_RETURN)
        {
            saveEntry();
            return true;
        }
        if (key == Keyboard.KEY_BACK && !input.isEmpty())
        {
            input = input.substring(0, input.length() - 1);
            return true;
        }
        if (isValidChar(c) && input.length() < 32)
        {
            input += c;
            return true;
        }
        return false;
    }

    public boolean isEntering()
    {
        return entering;
    }

    private void beginEntry()
    {
        entering = true;
        input = "";
        setStatus("Type a name, press Enter");
    }

    private void saveEntry()
    {
        String name = input.trim();
        if (name.isEmpty())
        {
            setStatus("Name required");
            return;
        }
        boolean ok = configManager != null && configManager.createNewConfig(name, moduleManager);
        if (ok)
        {
            entering = false;
            input = "";
            setStatus("Saved: " + name);
            refresh(true);
        }
        else
        {
            setStatus("Create failed");
        }
    }

    private void cancelEntry()
    {
        entering = false;
        input = "";
        setStatus("Cancelled");
    }

    private void refresh(boolean force)
    {
        long now = System.currentTimeMillis();
        if (!force && now < nextRefresh) return;
        configs.clear();
        if (configManager != null)
        {
            configs.addAll(configManager.listConfigNames());
        }
        nextRefresh = now + 1000L;
    }

    private void setStatus(String msg)
    {
        status = msg;
        statusUntil = System.currentTimeMillis() + 2500L;
    }

    private float scrollMaxSafe(int contentH, int viewH)
    {
        int max = Math.max(0, contentH - viewH);
        return max;
    }

    private boolean isValidChar(char c)
    {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || c == ' ' || c == '-' || c == '_';
    }
}
