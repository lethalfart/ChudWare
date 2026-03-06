package com.lethalfart.ChudWare.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleManager
{
    private final List<Module> modules = new ArrayList<>();
    private final Map<Class<?>, Module> moduleTypeCache = new HashMap<>();

    public void registerModule(Module module)
    {
        modules.add(module);
        
        moduleTypeCache.put(module.getClass(), module);
    }

    public List<Module> getModules()
    {
        return Collections.unmodifiableList(modules);
    }

    public <T extends Module> T getModule(Class<T> moduleClass)
    {
        Module cached = moduleTypeCache.get(moduleClass);
        if (cached != null)
        {
            return moduleClass.cast(cached);
        }
        
        for (Module module : modules)
        {
            if (moduleClass.isInstance(module))
            {
                moduleTypeCache.put(moduleClass, module);
                return moduleClass.cast(module);
            }
        }

        return null;
    }
}
