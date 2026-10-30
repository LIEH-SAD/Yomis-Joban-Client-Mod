package com.jsblock.mixin;

import mtr.data.Train;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;
import java.util.UUID;

/**
 * Exposes the set of entities currently riding a train, so that client side rendering
 * can skip the train the local player is riding on.
 * @since 1.2.12
 */
@Mixin(value = Train.class, remap = false)
public interface MixinTrainAccessor {

	@Accessor("ridingEntities")
	Set<UUID> getRidingEntities();
}
