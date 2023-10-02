package com.lightmanlp.invtweaks.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.src.client.gui.GuiContainer;

@Mixin(GuiContainer.class)
public interface MixinGuiContainer {
    @Accessor("xSize")
    int getXSize();

    @Accessor("ySize")
    int getYSize();
}
