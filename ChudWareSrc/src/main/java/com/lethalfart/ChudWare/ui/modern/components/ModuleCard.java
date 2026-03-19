package com.lethalfart.ChudWare.ui.modern.components;

import com.lethalfart.ChudWare.module.Module;
import com.lethalfart.ChudWare.ui.ModernFontRenderer;
import com.lethalfart.ChudWare.ui.modern.GuiAnim;
import com.lethalfart.ChudWare.ui.modern.GuiCallbacks;
import com.lethalfart.ChudWare.ui.modern.GuiContext;
import com.lethalfart.ChudWare.ui.modern.GuiRender;
import com.lethalfart.ChudWare.ui.modern.GuiTheme;

import java.util.ArrayList;
import java.util.List;

public class ModuleCard
{
    private final Module module;
    private final List<SettingControl> settings = new ArrayList<>();
    private final GuiAnim.TimedFloat expandAnim = new GuiAnim.TimedFloat(0f);
    private final GuiAnim.SmoothFloat hoverAnim = new GuiAnim.SmoothFloat(0f, 10f);
    private final GuiAnim.SmoothFloat toggleAnim = new GuiAnim.SmoothFloat(0f, 10f);
    private boolean expanded = false;
    private boolean lastEnabled;

    private int x;
    private int y;
    private int w;
    private int h;
    private int settingsVisible;

    public ModuleCard(Module module)
    {
        this.module = module;
        this.lastEnabled = module.isEnabled();
        this.toggleAnim.snap(module.isEnabled() ? 1f : 0f);
    }

    public Module getModule() { return module; }

    public void addSetting(SettingControl control)
    {
        settings.add(control);
    }

    public List<SettingControl> getSettings()
    {
        return settings;
    }

    public void setExpanded(boolean expanded)
    {
        if (this.expanded == expanded) return;
        this.expanded = expanded;
        expandAnim.animateTo(expanded ? 1f : 0f, 220L);
    }

    public boolean isExpanded() { return expanded; }

    public void toggleExpanded()
    {
        setExpanded(!expanded);
    }

    public float getHeight()
    {
        return GuiTheme.ROW_H + getSettingsHeight() * expandAnim.get();
    }

    private int getSettingsHeight()
    {
        if (settings.isEmpty()) return 0;
        int total = 6;
        for (SettingControl control : settings)
        {
            total += Math.round(control.getHeight()) + GuiTheme.GAP;
        }
        total -= GuiTheme.GAP;
        return Math.max(0, total);
    }

    public void render(GuiContext ctx, GuiCallbacks cb, int x, int y, int w)
    {
        this.x = x;
        this.y = y;
        this.w = w;

        boolean hoverHeader = ctx.hit(x, y, w, GuiTheme.ROW_H);
        hoverAnim.setTarget(hoverHeader ? 1f : 0f);
        hoverAnim.update(ctx.delta);

        boolean enabled = module.isEnabled();
        if (enabled != lastEnabled)
        {
            toggleAnim.setTarget(enabled ? 1f : 0f);
            lastEnabled = enabled;
        }
        toggleAnim.update(ctx.delta);
        expandAnim.update(ctx.now);

        settingsVisible = Math.round(getSettingsHeight() * expandAnim.get());
        h = GuiTheme.ROW_H + settingsVisible;

        float hover = hoverAnim.get();
        float toggled = toggleAnim.get();
        int base = GuiRender.lerpColor(GuiTheme.CARD_BG, GuiTheme.CARD_ACTIVE, toggled);
        int bg = GuiRender.lerpColor(base, GuiTheme.CARD_HOVER, hover);
        int radius = GuiTheme.RADIUS_M;
        if (hover > 0.01f || toggled > 0.01f)
        {
            int shadow = GuiRender.withAlpha(0xFF000000, 0.55f + 0.35f * hover);
            GuiRender.drawSoftShadow(x, y, w, h, radius, shadow);
        }
        GuiRender.drawRoundedBorder(x, y, w, h, radius, GuiRender.withAlpha(GuiTheme.BORDER, 0.6f + 0.4f * hover), bg);

        int textCol = GuiTheme.TEXT;
        int nameH = ctx.font.getFontHeight(ModernFontRenderer.SIZE_NORMAL);
        int nameY = y + (GuiTheme.ROW_H - nameH) / 2;
        ctx.font.drawString(module.getName(), x + GuiTheme.PADDING, nameY, textCol, ModernFontRenderer.SIZE_NORMAL);

        renderToggle(ctx, x, y, w);

        int accentW = 3 + Math.round(3 * toggled);
        GuiRender.drawRoundedRect(x + 2, y + 6, accentW, GuiTheme.ROW_H - 12, GuiTheme.RADIUS_S, GuiRender.withAlpha(GuiTheme.ACCENT, 0.7f + 0.3f * toggled));

        if (settingsVisible > 0)
        {
            int sx = x + GuiTheme.PADDING;
            int sy = y + GuiTheme.ROW_H + 6;
            int sw = w - (GuiTheme.PADDING * 2);

            GuiRender.beginScissor(ctx.sr, x, y + GuiTheme.ROW_H, w, settingsVisible);
            for (SettingControl control : settings)
            {
                control.layout(sx, sy, sw);
                control.render(ctx, cb, expandAnim.get());
                sy += control.getH() + GuiTheme.GAP;
            }
            GuiRender.endScissor();
        }
    }

    private void renderToggle(GuiContext ctx, int x, int y, int w)
    {
        int toggleW = 36;
        int toggleH = 18;
        int tx = x + w - toggleW - GuiTheme.PADDING;
        int ty = y + (GuiTheme.ROW_H - toggleH) / 2;
        float t = toggleAnim.get();
        int bg = GuiRender.lerpColor(GuiTheme.TRACK, GuiTheme.ACCENT, t);
        GuiRender.drawRoundedRect(tx, ty, toggleW, toggleH, toggleH / 2, bg);

        int knob = 12;
        int kx = tx + 3 + Math.round((toggleW - knob - 6) * t);
        int ky = ty + 3;
        GuiRender.drawRoundedRect(kx, ky, knob, knob, knob / 2, GuiTheme.TEXT);
    }

    public SettingControl.SliderControl mouseClicked(GuiContext ctx, GuiCallbacks cb, int button)
    {
        if (ctx.mouseX < x || ctx.mouseX > x + w || ctx.mouseY < y || ctx.mouseY > y + h)
        {
            return null;
        }

        if (ctx.mouseY <= y + GuiTheme.ROW_H)
        {
            if (button == 0)
            {
                module.toggle();
                return null;
            }
            if (button == 1)
            {
                toggleExpanded();
                return null;
            }
        }

        if (!expanded || settingsVisible <= 0) return null;
        for (SettingControl control : settings)
        {
            if (ctx.mouseX < control.getX() || ctx.mouseX > control.getX() + control.getW()
                    || ctx.mouseY < control.getY() || ctx.mouseY > control.getY() + control.getH())
            {
                continue;
            }
            SettingControl result = control.mouseClicked(ctx, cb, button);
            if (result instanceof SettingControl.SliderControl)
            {
                return (SettingControl.SliderControl) result;
            }
        }
        return null;
    }

    public void mouseReleased(GuiContext ctx, GuiCallbacks cb, int button)
    {
        for (SettingControl control : settings)
        {
            control.mouseReleased(ctx, cb, button);
        }
    }

    public void mouseDragged(GuiContext ctx, GuiCallbacks cb, int button)
    {
        for (SettingControl control : settings)
        {
            if (control.isDragging())
            {
                control.mouseDragged(ctx, cb, button);
            }
        }
    }
}
