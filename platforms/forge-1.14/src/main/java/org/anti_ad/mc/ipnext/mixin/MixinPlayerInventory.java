/*
 * Inventory Profiles Next
 *
 *   Copyright (c) 2019-2020 jsnimda <7615255+jsnimda@users.noreply.github.com>
 *   Copyright (c) 2021-2022 Plamen K. Kosseff <p.kosseff@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.anti_ad.mc.ipnext.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.anti_ad.mc.ipnext.config.Debugs;
import org.anti_ad.mc.ipnext.config.LockedSlotsSettings;
import org.anti_ad.mc.ipnext.config.ModSettings;
import org.anti_ad.mc.ipnext.event.LockSlotsHandler;
import org.anti_ad.mc.ipnext.event.LockedSlotKeeper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public abstract class MixinPlayerInventory {


    @Shadow @Final public NonNullList<ItemStack> mainInventory;

    private ItemStack addedStack = null;

    private boolean skipChecks = false;

    @Inject(at = @At(value = "HEAD", target = "Lnet/minecraft/entity/player/PlayerInventory;getFirstEmptyStack()I"),
            method = "getFirstEmptyStack",
            cancellable = true)
    public void getEmptySlot(CallbackInfoReturnable<Integer> info) {
        if (!skipChecks) {
            if (Minecraft.getInstance().player != null) {
                if (ModSettings.INSTANCE.getENABLE_LOCK_SLOTS().getValue() &&
                        !LockedSlotsSettings.INSTANCE.getLOCKED_SLOTS_ALLOW_PICKUP_INTO_EMPTY().getValue()
                        && !Debugs.INSTANCE.getFORCE_SERVER_METHOD_FOR_LOCKED_SLOTS().getValue()) {
                    for (int i = 0; i < this.mainInventory.size(); ++i) {
                        if (LockedSlotsSettings.INSTANCE.getLOCKED_SLOTS_EMPTY_HOTBAR_AS_SEMI_LOCKED().getValue()
                                && PlayerInventory.isHotbar(i)
                                && !LockedSlotKeeper.INSTANCE.isOnlyHotbarFree()
                                && !LockedSlotKeeper.INSTANCE.isIgnored(addedStack)
                                && LockedSlotKeeper.INSTANCE.isHotBarSlotEmpty(i)) {
                            continue;
                        }
                        if (!LockSlotsHandler.INSTANCE.isSlotLocked(i)) {
                            if ((this.mainInventory.get(i)).isEmpty()) {

                                info.setReturnValue(i);
                                return;
                            }
                        }
                    }
                    info.setReturnValue(-1);
                }
            }
        }
    }

    @Inject(at = @At(value = "HEAD", target = "Lnet/minecraft/entity/player/PlayerInventory;addItemStackToInventory(Lnet/minecraft/item/ItemStack;)Z"),
            method = "addItemStackToInventory")
    public void addStackPre(ItemStack itemStackIn, CallbackInfoReturnable<Boolean> cir) {
        addedStack = itemStackIn;
    }

    @Inject(at = @At(value = "TAIL", target = "Lnet/minecraft/entity/player/PlayerInventory;addItemStackToInventory(Lnet/minecraft/item/ItemStack;)Z"),
            method = "addItemStackToInventory")
    public void addStackPost(ItemStack itemStackIn, CallbackInfoReturnable<Boolean> cir) {
        addedStack = null;
    }


    @Inject(at = @At(value = "HEAD", target = "Lnet/minecraft/entity/player/PlayerInventory;setPickedItemStack(Lnet/minecraft/item/ItemStack;)V"),
            method = "setPickedItemStack")
    public void setPickedItemPre(ItemStack stack, CallbackInfo ci) {
        skipChecks = true;
    }

    @Inject(at = @At(value = "TAIL", target = "Lnet/minecraft/entity/player/PlayerInventory;setPickedItemStack(Lnet/minecraft/item/ItemStack;)V"),
            method = "setPickedItemStack")
    public void setPickedItemPost(ItemStack stack, CallbackInfo ci) {
        skipChecks = false;
    }

}
