package dev.muhammedesmer.customjukeboxdiscs.gametest;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class DirectGameTestInstance extends GameTestInstance {
    private final Consumer<GameTestHelper> test;
    private final MapCodec<DirectGameTestInstance> codec;

    DirectGameTestInstance(
            TestData<Holder<TestEnvironmentDefinition<?>>> data,
            Consumer<GameTestHelper> test) {
        super(data);
        this.test = test;
        this.codec = MapCodec.unit(this);
    }

    @Override
    public void run(GameTestHelper helper) {
        test.accept(helper);
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return codec;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Java test");
    }
}
