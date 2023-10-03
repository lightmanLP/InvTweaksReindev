package com.lightmanlp.invtweaks;

import com.fox2code.foxloader.loader.Mod;

import net.minecraft.client.Minecraft;
import net.minecraft.src.client.GameSettings;
import net.minecraft.src.client.KeyBinding;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.input.Keyboard;

import com.fox2code.foxloader.loader.ClientMod;

public class InvTweaksMod extends Mod implements ClientMod {
    public static InvTweaksMod INSTANCE;
    public static InvTweaks invTweaks;

    private long clock = 0L;
    private Map<KeyBinding, Boolean> prevKeysState = new HashMap<>();

    public InvTweaksMod() {
        assert INSTANCE == null;
        INSTANCE = this;
        prevKeysState.put(Const.SORT_KEY_BINDING, false);
    }

    public static void registerCustomKeys(GameSettings settings) {
        KeyBinding[] oldBindings = settings.keyBindings;
        settings.keyBindings = Arrays.copyOf(oldBindings, oldBindings.length + 1);
        settings.keyBindings[oldBindings.length] = Const.SORT_KEY_BINDING;
    }

    public void onStart(Minecraft mc) {
        invTweaks = new InvTweaks(mc);
    }

    public void onTickInstantiated(Minecraft mc) {
        if (mc.theWorld != null) {
            long newclock = mc.theWorld.getWorldTime();

            if (clock != newclock) {
                invTweaks.onTickInGame();

                KeyBinding key = Const.SORT_KEY_BINDING;
                boolean currentState = Keyboard.isKeyDown(key.keyCode);
                boolean prevState = prevKeysState.get(key);
                prevKeysState.put(key, currentState);
                if (currentState && !prevState) {
                    invTweaks.onSortingKeyPressed();
                }
            }

            clock = newclock;
        }

        if (mc.currentScreen != null) {
            invTweaks.onTickInGUI(mc.currentScreen);
        }
    }
}
