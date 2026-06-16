package me.karven.orderium.utils;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.item.BannerPatternLayers;
import io.papermc.paper.datacomponent.item.BundleContents;
import org.bukkit.DyeColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Serialize an nbt data to a Map<String, Object> to store in config file
@SuppressWarnings("UnstableApiUsage")
public abstract class NBTSerializer<T> {
    public static final NBTSerializer<ItemStack> ITEM_STACK = new NBTSerializer<>() {
        @Override
        @NotNull Object serialize(Object itemObject) {
            if (!(itemObject instanceof ItemStack item)) {
                throw new IllegalStateException("object to serialize is not an ItemStack, it is " + itemObject.getClass() + ". This is a bug");
            }
            if (item.isEmpty()) {
                return Map.of("id", "minecraft:air");
            }
            final Map<String, Object> result = new LinkedHashMap<>();
            final ItemType type = item.getType().asItemType();
            if (type == null) result.put("id", "minecraft:air");
            else result.put("id", type.getKey().asString());
            result.put("count", item.getAmount());
            final Map<String, Object> components = new LinkedHashMap<>();

            for (final DataComponentType component : item.getDataTypes()) {
                final String componentKey = component.getKey().asString();
                if (component instanceof DataComponentType.NonValued) {
                    components.put(componentKey, true);
                    continue;
                }
                if (!(component instanceof DataComponentType.Valued<?> valuedComponent)) {
                    Log.error("Component is neither valued nor non-valued. This is a bug", new IllegalStateException());
                    continue;
                }
                final Object componentData = item.getData(valuedComponent);
                if (componentData == null) {
                    Log.error("Data of component in item is null. This is a bug", new IllegalStateException());
                    continue;
                }
                final NBTSerializer<?> serializer = serializers.get(componentKey);
                if (serializer == null) {
                    Log.error("No serializer for component " + componentKey + ". This is a bug.", new IllegalStateException());
                    continue;
                }
                final Object serializedComponent = serializer.serialize(componentData);
                components.put(componentKey, serializedComponent);
            }
            result.put("components", components);
            return result;
        }

        @Override
        @NonNull ItemStack deserialize(@NotNull Object value) {
            if (!(value instanceof Map<?, ?> data)) {
                throw new IllegalStateException("object to deserialize is not a Map, it is " + value.getClass());
            }
            final int amount;
            if (data.get("amount") instanceof Integer intAmount) amount = intAmount;
            else amount = 1;
            final String typeKey = (String) data.get("id");
            final ItemType type = ConvertUtils.getItemType(typeKey);
            final ItemStack item = typeKey.equals("minecraft:air") ? ItemStack.empty() : type.createItemStack(amount);
            final Object componentsObject = data.get("components");
            if (componentsObject == null) {
                return item;
            }
            if (!(componentsObject instanceof Map<?, ?> components)) {
                throw new IllegalStateException("components is not a Map, it is " + componentsObject.getClass());
            }
            for (final Map.Entry<?, ?> entry : components.entrySet()) {
                final String componentKey = (String) entry.getKey();

                final String[] keyComponents = componentKey.split(":");
                if (keyComponents.length != 2) {
                    Log.error("Invalid component key: " + componentKey, new IllegalStateException());
                    continue;
                }
                final DataComponentType componentType = Registry.DATA_COMPONENT_TYPE.get(new NamespacedKey(keyComponents[0], keyComponents[1]));

                final Object componentValue = entry.getValue();


                // non valued component
                if (componentType instanceof DataComponentType.NonValued nonValuedComponentType) {
                    if (!(componentValue instanceof Boolean set)) {
                        Log.error("Invalid value for component type " + componentKey, new IllegalStateException());
                        continue;
                    }
                    if (set) {
                        item.setData(nonValuedComponentType);
                    } else {
                        item.unsetData(nonValuedComponentType);
                    }
                    continue;
                }
                if (!(componentType instanceof DataComponentType.Valued<?> valuedComponentType)) {
                    Log.error("Component type is neither valued nor non-valued. This is a bug.", new IllegalStateException());
                    continue;
                }
                final NBTSerializer<?> serializer = serializers.get(componentKey);
                if (serializer == null) {
                    Log.error("No serializer for component " + componentKey, new IllegalStateException());
                    continue;
                }
                final Object deserializedComponent = serializer.deserialize(componentValue);
                item.setData(NBTSerializer.cast(valuedComponentType), NBTSerializer.cast(valuedComponentType, deserializedComponent));
            }
            return item;
        }
    };

    private static <T> DataComponentType.Valued<T> cast(final DataComponentType.Valued<?> componentType) {
        @SuppressWarnings("unchecked") final DataComponentType.Valued<T> result = (DataComponentType.Valued<T>) componentType;
        return result;
    }

    private static <T> T cast(final DataComponentType.Valued<T> componentType, final Object value) {
        @SuppressWarnings("unchecked") final T castedValue = (T) value;
        return castedValue;
    }


    public static final NBTSerializer<BannerPatternLayers> BANNER_PATTERNS = new NBTSerializer<>() {

        @Override
        @NotNull Object serialize(final Object value) {
            if (!(value instanceof BannerPatternLayers layers)) {
                throw new IllegalStateException("object to serialize is not a BannerPatternLayers, it is " + value.getClass() + ". This is a bug");
            }
            final List<Map<String, Object>> result = new ArrayList<>();
            for (final Pattern pattern : layers.patterns()) {
                result.add(pattern.serialize());
            }

            return result;
        }

        @Override
        @NonNull BannerPatternLayers deserialize(final @NotNull Object value) {
            if (!(value instanceof List<?> list)) {
                throw new IllegalStateException("object to deserialize is not a List, it is " + value.getClass());
            }
            final List<Pattern> patterns = new ArrayList<>();
            for (final Object patternObject : list) {
                if (!(patternObject instanceof Map<?, ?> patternData)) {
                    Log.error("pattern is not a Map, it is " + patternObject.getClass(), new IllegalStateException());
                    continue;
                }
                @SuppressWarnings("unchecked") final Map<String, Object> serializedPattern = (Map<String, Object>) patternData;
                final Pattern pattern = new Pattern(serializedPattern);
                patterns.add(pattern);
            }
            return BannerPatternLayers.bannerPatternLayers(patterns);
        }
    };

    public static final NBTSerializer<DyeColor> BASE_COLOR = new NBTSerializer<>() {

        @Override
        @NotNull Object serialize(final Object color) {
            return color.toString().toLowerCase();
        }

        @Override
        @NonNull DyeColor deserialize(final @NotNull Object value) {
            return DyeColor.valueOf(value.toString().toUpperCase());
        }
    };


    public static final NBTSerializer<BundleContents> BUNDLE_CONTENTS = new NBTSerializer<>() {
        @Override
        @NotNull Object serialize(final Object value) {
            if (!(value instanceof BundleContents bundleContents))
                throw new IllegalArgumentException("value is not a BundleContents, it is " + value.getClass() + ". This is a bug");
            final List<Map<String, Object>> serializedContents = new ArrayList<>();
            for (final ItemStack item : bundleContents.contents()) {
                @SuppressWarnings("unchecked") final Map<String, Object> serializedItem = (Map<String, Object>) ITEM_STACK.serialize(item);
                serializedContents.add(serializedItem);
            }
            return serializedContents;
        }

        @Override
        @NonNull BundleContents deserialize(final @NotNull Object value) {
            if (!(value instanceof List<?> data)) {
                throw new IllegalStateException("object to deserialize is not a List, it is " + value.getClass());
            }
            final List<ItemStack> contents = new ArrayList<>();
            for (final Object serializedItem : data) {
                contents.add(ITEM_STACK.deserialize(serializedItem));
            }
            return BundleContents.bundleContents(contents);
        }
    };

    private static final Map<String, NBTSerializer<?>> serializers;

    static {
        serializers = Map.of(
                "minecraft:banner_patterns", BANNER_PATTERNS,
                "minecraft:base_color", BASE_COLOR,
                "minecraft:bundle_contents", BUNDLE_CONTENTS
        );
    }

    abstract @NotNull Object serialize(final Object value);

    abstract @NotNull T deserialize(final @NotNull Object value);
}
