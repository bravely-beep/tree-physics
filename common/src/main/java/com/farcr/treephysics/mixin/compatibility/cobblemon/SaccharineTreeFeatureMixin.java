package com.farcr.treephysics.mixin.compatibility.cobblemon;

import com.farcr.treephysics.api.util.TreeUtil;
import com.farcr.treephysics.index.TreePhysicsConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Cobblemon's saccharine tree is a bespoke {@code Feature} that does not extend the vanilla
 * {@code TreeFeature}, so {@code TreeFeatureMixin}/{@code TrunkPlacerMixin} never run for it.
 * We replicate {@link TreeUtil#setDirtUnder} here by wrapping the base-trunk {@code setBlock} call.
 *
 * <p>The target is referenced by string ({@code targets = ...}) instead of a class literal so that
 * Cobblemon is NOT needed on the compile classpath. {@code TreePhysicsMixinPlugin} only applies this
 * mixin when {@code cobblemon} is loaded, so the class always resolves by the time it runs.</p>
 *
 * <p>Inside {@code place} the only direct {@code WorldGenLevel.setBlock} call is the base-trunk loop;
 * every other log/leaf is placed from helper methods ({@code placeBigLeafPattern},
 * {@code placeTopTrunkPattern}, etc.), which {@code @WrapOperation(method = "place")} does not touch.
 * Combined with the {@link TreeUtil#canBeRoots} guard, only the bottom log (sitting on dirt) gets
 * rooted dirt placed beneath it.</p>
 */
@Mixin(targets = "com.cobblemon.mod.common.world.feature.SaccharineTreeFeature", remap = false)
public class SaccharineTreeFeatureMixin {

    @WrapOperation(
            method = "place",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            ),
            remap = true
    )
    private boolean treephysics$setBlock(WorldGenLevel instance, BlockPos pos, BlockState state, int flags, Operation<Boolean> original) {
        if (TreePhysicsConfig.ROOTED_DIRT_GENERATION.getAsBoolean()
                && TreeUtil.isLog(state)
                && TreeUtil.canBeRoots(instance, pos.below())) {
            BlockPos below = pos.below();
            original.call(instance, below, TreeUtil.getRootForState(instance.getBlockState(below)), flags);
        }

        return original.call(instance, pos, state, flags);
    }

}