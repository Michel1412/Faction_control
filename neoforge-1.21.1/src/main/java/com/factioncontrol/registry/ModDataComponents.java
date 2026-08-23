package com.factioncontrol.registry;

import com.factioncontrol.FactionControlMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, FactionControlMod.MODID);

    public record SelectedChunk(int x, int z) {
        public static final Codec<SelectedChunk> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("x").forGetter(SelectedChunk::x),
                Codec.INT.fieldOf("z").forGetter(SelectedChunk::z)
        ).apply(instance, SelectedChunk::new));

        public static final StreamCodec<ByteBuf, SelectedChunk> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                SelectedChunk::x,
                ByteBufCodecs.VAR_INT,
                SelectedChunk::z,
                SelectedChunk::new
        );
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SelectedChunk>> SELECTED_CHUNK =
            COMPONENTS.registerComponentType(
                    "selected_chunk",
                    builder -> builder.persistent(SelectedChunk.CODEC).networkSynchronized(SelectedChunk.STREAM_CODEC)
            );

    private ModDataComponents() {
    }
}
