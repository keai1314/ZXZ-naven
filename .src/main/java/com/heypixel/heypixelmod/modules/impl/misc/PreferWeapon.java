package com.heypixel.heypixelmod.modules.impl.misc;

import com.heypixel.heypixelmod.Naven;
import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.impl.EventKey;
import com.heypixel.heypixelmod.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;
import com.heypixel.heypixelmod.values.ValueBuilder;
import com.heypixel.heypixelmod.values.impl.ModeValue;

import java.util.HashSet;
import java.util.Set;

@ModuleInfo(
        name = "PreferWeapon",
        description = "Prioritizes a specific weapon for InventoryManager's sword slot",
        category = Category.MISC
)
public class PreferWeapon extends Module {
    private final ModeValue weaponPriority = ValueBuilder.create(this, "Priority")
            .setModes("Sword", "God Axe", "KB Ball", "End Crystal")
            .build()
            .getModeValue();

    private final Set<Integer> pressedKeys = new HashSet<>();

    public PreferWeapon() {
        this.setToggleableWithKey(false);
    }

    @EventTarget
    public void onMotion(EventRunTicks event) {
        this.setSuffix(weaponPriority.getCurrentMode());
    }

    @EventTarget
    public void onKey(EventKey event) {
        if (this.isEnabled() && this.getKey() == event.getKey()) {
            if (event.isState()) {
                if (pressedKeys.add(event.getKey())) {
                    int currentIndex = weaponPriority.getCurrentValue();
                    int nextIndex = (currentIndex + 1) % weaponPriority.getValues().length;
                    weaponPriority.setCurrentValue(nextIndex);
                }
            } else {
                pressedKeys.remove(event.getKey());
            }
        }
    }

    public static String getPriority() {
        PreferWeapon module = (PreferWeapon) Naven.getInstance().getModuleManager().getModule(PreferWeapon.class);

        if (module != null && module.isEnabled()) {
            return module.weaponPriority.getCurrentMode();
        }

        return "Sword";
    }
}
