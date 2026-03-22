package com.lethalfart.ChudWare.ui.modern;

import com.lethalfart.ChudWare.module.Module;
import com.lethalfart.ChudWare.ui.modern.components.SettingControl;

public interface GuiCallbacks
{
    void requestKeybind(Module module);
    boolean isKeybindTarget(Module module);
    void startSliderDrag(SettingControl.SliderControl slider);
    void stopSliderDrag(SettingControl.SliderControl slider);
    void markConfigDirty();
    void clearConfigDirty();
}
