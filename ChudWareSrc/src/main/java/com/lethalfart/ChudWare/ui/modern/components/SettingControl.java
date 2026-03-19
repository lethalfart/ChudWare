package com.lethalfart.ChudWare.ui.modern.components;

import com.lethalfart.ChudWare.module.Module;
import com.lethalfart.ChudWare.module.impl.AutoPotModule;
import com.lethalfart.ChudWare.ui.modern.GuiAnim;
import com.lethalfart.ChudWare.ui.modern.GuiCallbacks;
import com.lethalfart.ChudWare.ui.modern.GuiContext;
import com.lethalfart.ChudWare.ui.modern.GuiRender;
import com.lethalfart.ChudWare.ui.modern.GuiTheme;
import org.lwjgl.input.Keyboard;

import java.util.Locale;

public abstract class SettingControl
{
    protected final String label;
    protected int x;
    protected int y;
    protected int w;
    protected int h;
    protected final GuiAnim.SmoothFloat hover = new GuiAnim.SmoothFloat(0f, 14f);

    protected SettingControl(String label)
    {
        this.label = label;
    }

    public void layout(int x, int y, int w)
    {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = Math.round(getHeight());
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getW() { return w; }
    public int getH() { return h; }

    public abstract float getHeight();

    public void update(GuiContext ctx)
    {
        boolean isHover = ctx.hit(x, y, w, h);
        hover.setTarget(isHover ? 1f : 0f);
        hover.update(ctx.delta);
    }

    public abstract void render(GuiContext ctx, GuiCallbacks cb, float alpha);

    public SettingControl mouseClicked(GuiContext ctx, GuiCallbacks cb, int button)
    {
        return null;
    }

    public void mouseReleased(GuiContext ctx, GuiCallbacks cb, int button)
    {
    }

    public void mouseDragged(GuiContext ctx, GuiCallbacks cb, int button)
    {
    }

    public boolean isDragging()
    {
        return false;
    }

    protected boolean hit(GuiContext ctx)
    {
        return ctx.hit(x, y, w, h);
    }

    public interface BoolGetter { boolean get(); }
    public interface BoolSetter { void set(boolean v); }
    public interface FloatGetter { float get(); }
    public interface FloatSetter { void set(float v); }
    public interface IntGetter { int get(); }
    public interface IntSetter { void set(int v); }

    public static class LabelControl extends SettingControl
    {
        public LabelControl(String label)
        {
            super(label);
        }

        @Override
        public float getHeight() { return 20f; }

        @Override
        public void render(GuiContext ctx, GuiCallbacks cb, float alpha)
        {
            int color = GuiRender.withAlpha(GuiTheme.TEXT, alpha);
            int fh = ctx.font.getFontHeight(com.lethalfart.ChudWare.ui.ModernFontRenderer.SIZE_SMALL);
            int ty = y + (h - fh) / 2;
            ctx.font.drawString(label, x, ty, color, com.lethalfart.ChudWare.ui.ModernFontRenderer.SIZE_SMALL);
        }
    }

    public static class ToggleControl extends SettingControl
    {
        private final BoolGetter getter;
        private final BoolSetter setter;
        private final GuiAnim.SmoothFloat state = new GuiAnim.SmoothFloat(0f, 10f);

        public ToggleControl(String label, BoolGetter getter, BoolSetter setter)
        {
            super(label);
            this.getter = getter;
            this.setter = setter;
            state.snap(getter.get() ? 1f : 0f);
        }

        @Override
        public float getHeight() { return 24f; }

        @Override
        public void render(GuiContext ctx, GuiCallbacks cb, float alpha)
        {
            update(ctx);
            state.setTarget(getter.get() ? 1f : 0f);
            state.update(ctx.delta);

            int textCol = GuiRender.withAlpha(GuiTheme.TEXT, alpha);
            int fh = ctx.font.getFontHeight(com.lethalfart.ChudWare.ui.ModernFontRenderer.SIZE_SMALL);
            int tyText = y + (h - fh) / 2;
            ctx.font.drawString(label, x, tyText, textCol, com.lethalfart.ChudWare.ui.ModernFontRenderer.SIZE_SMALL);

            int toggleW = 34;
            int toggleH = 18;
            int tx = x + w - toggleW;
            int ty = y + (h - toggleH) / 2;

            float t = state.get();
            int bg = GuiRender.lerpColor(GuiTheme.TRACK, GuiTheme.ACCENT, t);
            GuiRender.drawRoundedRect(tx, ty, toggleW, toggleH, toggleH / 2, GuiRender.withAlpha(bg, alpha));

            int knob = 12;
            int kx = tx + 3 + Math.round((toggleW - knob - 6) * t);
            int ky = ty + 3;
            GuiRender.drawRoundedRect(kx, ky, knob, knob, knob / 2, GuiRender.withAlpha(GuiTheme.TEXT, alpha));
        }

        @Override
        public SettingControl mouseClicked(GuiContext ctx, GuiCallbacks cb, int button)
        {
            if (button != 0) return null;
            if (!hit(ctx)) return null;
            setter.set(!getter.get());
            return null;
        }
    }

    public static class KeybindControl extends SettingControl
    {
        private final Module module;

        public KeybindControl(Module module)
        {
            super("Keybind");
            this.module = module;
        }

        @Override
        public float getHeight() { return 24f; }

        @Override
        public void render(GuiContext ctx, GuiCallbacks cb, float alpha)
        {
            update(ctx);
            int textCol = GuiRender.withAlpha(GuiTheme.TEXT, alpha);
            int fh = ctx.font.getFontHeight(com.lethalfart.ChudWare.ui.ModernFontRenderer.SIZE_SMALL);
            int tyText = y + (h - fh) / 2;
            ctx.font.drawString(label, x, tyText, textCol, com.lethalfart.ChudWare.ui.ModernFontRenderer.SIZE_SMALL);

            boolean capturing = cb.isKeybindTarget(module);
            String key = capturing ? "[Press key]" : keyName(module.getKeyBind());
            int kw = ctx.font.getStringWidth(key, com.lethalfart.ChudWare.ui.ModernFontRenderer.SIZE_SMALL);
            int pillW = kw + 10;
            int pillH = Math.max(16, fh + 6);
            int px = x + w - pillW;
            int py = y + (h - pillH) / 2;
            GuiRender.drawRoundedRect(px, py, pillW, pillH, pillH / 2, GuiRender.withAlpha(GuiTheme.TRACK, alpha));
            int tyKey = py + (pillH - fh) / 2;
            ctx.font.drawString(key, px + 5, tyKey, textCol, com.lethalfart.ChudWare.ui.ModernFontRenderer.SIZE_SMALL);
        }

        @Override
        public SettingControl mouseClicked(GuiContext ctx, GuiCallbacks cb, int button)
        {
            if (button != 0) return null;
            if (!hit(ctx)) return null;
            cb.requestKeybind(module);
            return null;
        }

        private String keyName(int key)
        {
            if (key <= Keyboard.KEY_NONE) return "None";
            String name = Keyboard.getKeyName(key);
            return name == null ? "Unknown" : name;
        }
    }

    public static class CycleControl extends SettingControl
    {
        private final String[] options;
        private final IntGetter getter;
        private final IntSetter setter;

        public CycleControl(String label, String[] options, IntGetter getter, IntSetter setter)
        {
            super(label);
            this.options = options == null ? new String[0] : options.clone();
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public float getHeight() { return 24f; }

        @Override
        public void render(GuiContext ctx, GuiCallbacks cb, float alpha)
        {
            update(ctx);
            int textCol = GuiRender.withAlpha(GuiTheme.TEXT, alpha);
            int fh = ctx.font.getFontHeight(com.lethalfart.ChudWare.ui.ModernFontRenderer.SIZE_SMALL);
            int tyText = y + (h - fh) / 2;
            ctx.font.drawString(label, x, tyText, textCol, com.lethalfart.ChudWare.ui.ModernFontRenderer.SIZE_SMALL);

            String value = optionLabel(getter.get());
            int kw = ctx.font.getStringWidth(value, com.lethalfart.ChudWare.ui.ModernFontRenderer.SIZE_SMALL);
            int pillW = kw + 12;
            int pillH = Math.max(16, fh + 6);
            int px = x + w - pillW;
            int py = y + (h - pillH) / 2;
            GuiRender.drawRoundedRect(px, py, pillW, pillH, pillH / 2, GuiRender.withAlpha(GuiTheme.TRACK, alpha));
            int tyKey = py + (pillH - fh) / 2;
            ctx.font.drawString(value, px + 6, tyKey, textCol, com.lethalfart.ChudWare.ui.ModernFontRenderer.SIZE_SMALL);
        }

        @Override
        public SettingControl mouseClicked(GuiContext ctx, GuiCallbacks cb, int button)
        {
            if (button != 0) return null;
            if (!hit(ctx)) return null;
            if (options.length == 0) return null;
            int current = getter.get();
            if (current < 0 || current >= options.length)
            {
                current = 0;
            }
            int next = (current + 1) % options.length;
            setter.set(next);
            return null;
        }

        private String optionLabel(int index)
        {
            if (options.length == 0)
            {
                return "None";
            }
            if (index < 0 || index >= options.length)
            {
                return options[0];
            }
            return options[index];
        }
    }

    public static class SliderControl extends SettingControl
    {
        private final FloatGetter getter;
        private final FloatSetter setter;
        private final float min;
        private final float max;
        private final float step;
        private final boolean integer;
        private boolean dragging;

        public SliderControl(String label, FloatGetter getter, FloatSetter setter, float min, float max, float step, boolean integer)
        {
            super(label);
            this.getter = getter;
            this.setter = setter;
            this.min = min;
            this.max = max;
            this.step = step;
            this.integer = integer;
        }

        @Override
        public float getHeight() { return 36f; }

        @Override
        public void render(GuiContext ctx, GuiCallbacks cb, float alpha)
        {
            update(ctx);
            int textCol = GuiRender.withAlpha(GuiTheme.TEXT, alpha);
            int fh = ctx.font.getFontHeight(com.lethalfart.ChudWare.ui.ModernFontRenderer.SIZE_SMALL);
            int tyText = y + 2;
            ctx.font.drawString(label, x, tyText, textCol, com.lethalfart.ChudWare.ui.ModernFontRenderer.SIZE_SMALL);

            String val = format(getter.get());
            int vw = ctx.font.getStringWidth(val, com.lethalfart.ChudWare.ui.ModernFontRenderer.SIZE_SMALL);
            ctx.font.drawString(val, x + w - vw, tyText, textCol, com.lethalfart.ChudWare.ui.ModernFontRenderer.SIZE_SMALL);

            int trackH = 8;
            int trackY = y + h - trackH - 4;
            int trackX = x;
            int trackW = w;

            GuiRender.drawRoundedRect(trackX, trackY, trackW, trackH, trackH / 2, GuiRender.withAlpha(GuiTheme.TRACK, alpha));

            float percent = (getter.get() - min) / (max - min);
            percent = GuiRender.clamp(percent, 0f, 1f);
            int fillW = Math.round(trackW * percent);
            if (fillW > 0)
            {
                GuiRender.drawRoundedRect(trackX, trackY, fillW, trackH, trackH / 2, GuiRender.withAlpha(GuiTheme.ACCENT, alpha));
            }

            int knob = 12;
            int kx = trackX + fillW - knob / 2;
            int ky = trackY + (trackH - knob) / 2;
            GuiRender.drawRoundedRect(kx, ky, knob, knob, knob / 2, GuiRender.withAlpha(GuiTheme.TEXT, alpha));
        }

        @Override
        public SettingControl mouseClicked(GuiContext ctx, GuiCallbacks cb, int button)
        {
            if (button != 0) return null;
            if (!hit(ctx)) return null;
            dragging = true;
            updateFromMouse(ctx.mouseX);
            cb.startSliderDrag(this);
            return this;
        }

        @Override
        public void mouseReleased(GuiContext ctx, GuiCallbacks cb, int button)
        {
            if (dragging)
            {
                dragging = false;
                cb.stopSliderDrag(this);
            }
        }

        @Override
        public void mouseDragged(GuiContext ctx, GuiCallbacks cb, int button)
        {
            if (!dragging) return;
            updateFromMouse(ctx.mouseX);
        }

        @Override
        public boolean isDragging() { return dragging; }

        private void updateFromMouse(int mx)
        {
            float percent = (mx - x) / (float)w;
            percent = GuiRender.clamp(percent, 0f, 1f);
            float val = min + (max - min) * percent;
            if (step > 0f)
            {
                val = Math.round(val / step) * step;
            }
            if (integer)
            {
                val = Math.round(val);
            }
            val = GuiRender.clamp(val, min, max);
            setter.set(val);
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

    public static class HotbarControl extends SettingControl
    {
        private final AutoPotModule module;

        public HotbarControl(AutoPotModule module)
        {
            super("Hotbar Slots");
            this.module = module;
        }

        @Override
        public float getHeight() { return 46f; }

        @Override
        public void render(GuiContext ctx, GuiCallbacks cb, float alpha)
        {
            update(ctx);
            int textCol = GuiRender.withAlpha(GuiTheme.TEXT, alpha);
            int fh = ctx.font.getFontHeight(com.lethalfart.ChudWare.ui.ModernFontRenderer.SIZE_SMALL);
            int tyText = y + 2;
            ctx.font.drawString(label, x, tyText, textCol, com.lethalfart.ChudWare.ui.ModernFontRenderer.SIZE_SMALL);

            int slots = module.getHotbarSlotCount();
            int gap = 4;
            int boxW = (w - (gap * (slots - 1))) / slots;
            int boxH = 18;
            int by = y + fh + 6;

            for (int i = 0; i < slots; i++)
            {
                boolean on = module.isSlotEnabled(i);
                int bx = x + i * (boxW + gap);
                int bg = on ? GuiTheme.CARD_ACTIVE : GuiTheme.TRACK;
                int border = on ? GuiTheme.ACCENT : GuiTheme.BORDER;
                int text = GuiTheme.TEXT;
                GuiRender.drawRoundedBorder(bx, by, boxW, boxH, 5, GuiRender.withAlpha(border, alpha), GuiRender.withAlpha(bg, alpha));
                String label = String.valueOf(i + 1);
                int tw = ctx.font.getStringWidth(label, com.lethalfart.ChudWare.ui.ModernFontRenderer.SIZE_SMALL);
                int tx = bx + (boxW - tw) / 2;
                int ty = by + (boxH - ctx.font.getFontHeight(com.lethalfart.ChudWare.ui.ModernFontRenderer.SIZE_SMALL)) / 2;
                ctx.font.drawString(label, tx, ty, GuiRender.withAlpha(text, alpha), com.lethalfart.ChudWare.ui.ModernFontRenderer.SIZE_SMALL);
            }
        }

        @Override
        public SettingControl mouseClicked(GuiContext ctx, GuiCallbacks cb, int button)
        {
            if (button != 0) return null;
            int slots = module.getHotbarSlotCount();
            int gap = 4;
            int boxW = (w - (gap * (slots - 1))) / slots;
            int boxH = 18;
            int by = y + 18;
            if (ctx.mouseY < by || ctx.mouseY > by + boxH) return null;

            for (int i = 0; i < slots; i++)
            {
                int bx = x + i * (boxW + gap);
                if (ctx.mouseX >= bx && ctx.mouseX <= bx + boxW)
                {
                    module.setSlotEnabled(i, !module.isSlotEnabled(i));
                    return null;
                }
            }
            return null;
        }
    }
}
