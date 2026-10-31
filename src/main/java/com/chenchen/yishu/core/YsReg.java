package com.chenchen.yishu.core;

import com.chenchen.yishu.YishuMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;

/**
 * 易枢 · 注册工具。
 *
 * <p>26.x 里所有 {@code Item.Properties} / {@code BlockBehaviour.Properties}
 * 都必须显式 {@code setId(ResourceKey)}，否则启动即崩。这里把
 * 「造 key → 注册 → 返回实例」收成一个方法，避免每个内容类里重复样板。
 */
public final class YsReg {

    private YsReg() {
    }

    public static Identifier id(String path) {
        return YishuMod.id(path);
    }

    public static ResourceKey<Block> blockKey(String path) {
        return ResourceKey.create(Registries.BLOCK, id(path));
    }

    public static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, id(path));
    }

    public static <T> T register(Registry<T> registry, String path, T value) {
        return Registry.register(registry, id(path), value);
    }

    public static Item registerItem(String path, Item item) {
        return register(BuiltInRegistries.ITEM, path, item);
    }
}
