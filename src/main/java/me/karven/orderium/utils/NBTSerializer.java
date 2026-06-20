package me.karven.orderium.utils;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.github.thatsmusic99.configurationmaster.api.ConfigSection;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.item.*;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.DyeColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.*;

import static me.karven.orderium.utils.Values.ERROR_TRACKER;

// Serialize an nbt data to a Map, List, etc. to store it in config file
@SuppressWarnings("UnstableApiUsage")
public abstract class NBTSerializer<T> {
    public static final NBTSerializer<ItemStack> ITEM_STACK = new NBTSerializer<>() {
        @Override
        @NotNull Object serialize(final Object itemObject) {
            final ItemStack item = cast(itemObject);
            if (item.isEmpty()) {
                return Map.of("id", "minecraft:air");
            }
            final Map<String, Object> result = new LinkedHashMap<>();
            ItemType type = item.getType().asItemType();
            if (type == null) type = ItemType.AIR;
            result.put("id", type.getKey().asString());
            result.put("count", item.getAmount());
            final Map<String, Object> components = new LinkedHashMap<>();
            for (final DataComponentType component : item.getDataTypes()) {
                final String componentKey = component.getKey().asString();
                final boolean hasDefault = type.hasDefaultData(component);
                if (component instanceof DataComponentType.NonValued) {
                    if (!hasDefault) components.put(componentKey, true);
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
                if (hasDefault && componentData.equals(type.getDefaultData(valuedComponent))) continue;

                final NBTSerializer<?> serializer = componentSerializers.get(componentKey);
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
                throw new IllegalArgumentException("object to deserialize is not a Map, it is " + value.getClass());
            }
            if (!(data.get("count") instanceof Integer amount))
                throw new IllegalArgumentException("count is not an Integer, it is " + data.get("count").getClass());
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
                final NBTSerializer<?> serializer = componentSerializers.get(componentKey);
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
            final BannerPatternLayers layers = cast(value);
            final List<Map<String, Object>> result = new ArrayList<>();
            for (final Pattern pattern : layers.patterns()) {
                result.add(pattern.serialize());
            }

            return result;
        }

        @Override
        @NonNull BannerPatternLayers deserialize(final @NotNull Object value) {
            if (!(value instanceof List<?> list)) {
                throw new IllegalArgumentException("object to deserialize is not a List, it is " + value.getClass());
            }
            final List<Pattern> patterns = new ArrayList<>();
            for (final Object patternObject : list) {
                if (!(patternObject instanceof Map<?, ?> patternData)) {
                    Log.error("pattern is not a Map, it is " + patternObject.getClass(), new IllegalArgumentException());
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
            final BundleContents bundleContents = cast(value);
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
                throw new IllegalArgumentException("object to deserialize is not a List, it is " + value.getClass());
            }
            final List<ItemStack> contents = new ArrayList<>();
            for (final Object serializedItem : data) {
                contents.add(ITEM_STACK.deserialize(serializedItem));
            }
            return BundleContents.bundleContents(contents);
        }
    };

    public static final NBTSerializer<ChargedProjectiles> CHARGED_PROJECTILES = new NBTSerializer<>() {
        @Override
        @NotNull Object serialize(final Object value) {
            final ChargedProjectiles chargedProjectiles = cast(value);
            final List<Object> serializedProjectiles = new ArrayList<>();
            for (final ItemStack item : chargedProjectiles.projectiles()) {
                serializedProjectiles.add(ITEM_STACK.serialize(item));
            }
            return serializedProjectiles;
        }

        @Override
        @NonNull ChargedProjectiles deserialize(final @NotNull Object value) {
            if (!(value instanceof List<?> data)) {
                throw new IllegalArgumentException("object to deserialize is not a List, it is " + value.getClass());
            }
            final List<ItemStack> projectiles = new ArrayList<>();
            for (final Object serializedItem : data) {
                projectiles.add(ITEM_STACK.deserialize(serializedItem));
            }
            return ChargedProjectiles.chargedProjectiles(projectiles);
        }
    };

    public static final NBTSerializer<ResolvableProfile> PROFILE = new NBTSerializer<>() {
        @Override
        @NotNull Object serialize(final Object value) {
            final ResolvableProfile profile = cast(value);
            final Map<String, Object> serializedProfile = new LinkedHashMap<>(3);
            if (profile.name() != null) serializedProfile.put("name", profile.name());
            if (profile.uuid() != null) serializedProfile.put("id", profile.uuid().toString());
            if (profile.properties().isEmpty()) return serializedProfile;
            final List<Map<String, Object>> serializedProperties = new ArrayList<>();
            for (final ProfileProperty property : profile.properties()) {
                final Map<String, Object> serializedProperty = new LinkedHashMap<>(3);
                serializedProperty.put("name", property.getName());
                serializedProperty.put("value", property.getValue());
                if (property.getSignature() != null) serializedProperty.put("signature", property.getSignature());
                serializedProperties.add(serializedProperty);
            }
            serializedProfile.put("properties", serializedProperties);

            return serializedProfile;
        }

        @Override
        @NonNull ResolvableProfile deserialize(final @NotNull Object value) {
            if (!(value instanceof Map<?, ?> data)) {
                throw new IllegalArgumentException("object to deserialize is not a Map, it is " + value.getClass());
            }
            final ResolvableProfile.Builder builder = ResolvableProfile.resolvableProfile();
            final @Subst("ignored") String name = (String) data.get("name");
            final String uuid = (String) data.get("id");
            final List<?> properties = (List<?>) data.get("properties");
            if (name != null) builder.name(name);
            if (uuid != null) builder.uuid(UUID.fromString(uuid));
            if (properties == null) return builder.build();

            for (final Object propertyObject : properties) {
                if (!(propertyObject instanceof Map<?, ?> propertyData)) {
                    Log.error("property is not a Map, it is " + propertyObject.getClass(), new IllegalArgumentException());
                    continue;
                }
                final String propertyName = (String) propertyData.get("name");
                final String propertyValue = (String) propertyData.get("value");
                final String propertySignature = (String) propertyData.get("signature");
                if (propertyName == null || propertyValue == null) {
                    Log.error("property is missing name or value, skipping it", new IllegalArgumentException());
                    continue;
                }
                builder.addProperty(new ProfileProperty(propertyName, propertyValue, propertySignature));
            }

            return builder.build();
        }
    };

    public static final NBTSerializer<TooltipDisplay> TOOLTIP_DISPLAY = new NBTSerializer<>() {
        @Override
        @NotNull Object serialize(final Object value) {
            final TooltipDisplay tooltipDisplay = cast(value);
            return Map.of(
                    "hide_tooltip", tooltipDisplay.hideTooltip(),
                    "hidden_components", tooltipDisplay.hiddenComponents().stream().map(component -> component.getKey().asString()).toList()
            );
        }

        @Override
        @NonNull TooltipDisplay deserialize(final @NotNull Object value) {
            if (!(value instanceof Map<?, ?> data)) {
                throw new IllegalArgumentException("object to deserialize is not a Map, it is " + value.getClass());
            }
            final TooltipDisplay.Builder builder = TooltipDisplay.tooltipDisplay();
            builder.hideTooltip((boolean) data.get("hide_tooltip"));
            for (final Object componentObject : (List<?>) data.get("hidden_components")) {
                if (!(componentObject instanceof String componentString)) {
                    Log.error("hidden component is not a String, it is " + componentObject.getClass(), new IllegalArgumentException());
                    continue;
                }
                final String[] keyComponents = componentString.split(":");
                final NamespacedKey key = switch (keyComponents.length) {
                    case 1 -> NamespacedKey.minecraft(keyComponents[0]);
                    case 2 -> new NamespacedKey(keyComponents[0], keyComponents[1]);
                    default -> throw new IllegalStateException("Unexpected key: " + componentString);
                };
                final DataComponentType component = Registry.DATA_COMPONENT_TYPE.get(key);
                if (component == null) {
                    throw new IllegalStateException("Unknown component type: " + key);
                }
                builder.addHiddenComponents(component);
            }
            return builder.build();
        }
    };

    public static final NBTSerializer<Boolean> BOOLEAN = new NBTSerializer<>() {
        @Override
        @NotNull Object serialize(final Object value) {
            return value;
        }

        @Override
        @NonNull Boolean deserialize(final @NotNull Object value) {
            if (value instanceof Boolean bool) return bool;
            if (value instanceof String string) return Boolean.valueOf(string);
            throw new IllegalArgumentException("object to deserialize is not a Boolean, it is " + value.getClass());
        }
    };

    public static final NBTSerializer<Key> KEY = new NBTSerializer<>() {
        @Override
        @NotNull Object serialize(final Object value) {
            return cast(value).asString();
        }

        @Override
        @NonNull Key deserialize(final @NotNull Object value) {
            if (!(value instanceof String string)) {
                throw new IllegalArgumentException("object to deserialize is not a String, it is " + value.getClass());
            }
            final String[] keyComponents = string.split(":");
            if (keyComponents.length == 1) return NamespacedKey.minecraft(keyComponents[0]);
            if (keyComponents.length == 2) return new NamespacedKey(keyComponents[0], keyComponents[1]);
            throw new IllegalArgumentException("object to deserialize has invalid format of " + value);
        }
    };

    public static final NBTSerializer<Component> TEXT_COMPONENT = new NBTSerializer<>() {
        @Override
        @NotNull Object serialize(final Object value) {
            final Component component = cast(value);
            return Values.minimessage.serialize(component);
        }

        @Override
        @NonNull Component deserialize(final @NotNull Object value) {
            if (!(value instanceof String text)) {
                throw new IllegalArgumentException("object to deserialize is not a String, it is " + value.getClass());
            }

            return Values.minimessage.deserialize(text);
        }
    };

    public static final NBTSerializer<Object> OBJECT = new NBTSerializer<>() {
        @Override
        @NotNull Object serialize(final Object value) {
            return value;
        }

        @Override
        @NonNull Object deserialize(final @NotNull Object value) {
            return value;
        }
    };

    public static final NBTSerializer<ItemLore> LORE = new NBTSerializer<>() {
        @Override
        @NotNull Object serialize(final Object value) {
            return cast(value).lines().stream().map(Values.minimessage::serialize).toList();
        }

        @Override
        @NonNull ItemLore deserialize(final @NotNull Object value) {
            if (!(value instanceof List<?> list)) {
                throw new IllegalArgumentException("object to deserialize is not a List, it is " + value.getClass());
            }
            final List<Component> lines = new ArrayList<>();
            for (final Object serializedText : list) {
                if (!(serializedText instanceof String text)) {
                    throw new IllegalArgumentException("lore line to deserialize is not a String, it is " + value.getClass());
                }
                lines.add(Values.minimessage.deserialize(text));
            }
            return ItemLore.lore(lines);
        }
    };

    private static final Map<String, NBTSerializer<?>> componentSerializers = new HashMap<>();

    static {
        componentSerializers.put("minecraft:banner_patterns", BANNER_PATTERNS);
        componentSerializers.put("minecraft:base_color", BASE_COLOR);
        componentSerializers.put("minecraft:bundle_contents", BUNDLE_CONTENTS);
        componentSerializers.put("minecraft:charged_projectiles", CHARGED_PROJECTILES);
        componentSerializers.put("minecraft:custom_name", TEXT_COMPONENT);
        componentSerializers.put("minecraft:enchantment_glint_override", BOOLEAN);
        componentSerializers.put("minecraft:item_model", KEY);
        componentSerializers.put("minecraft:item_name", TEXT_COMPONENT);
        componentSerializers.put("minecraft:max_stack_size", OBJECT);
        componentSerializers.put("minecraft:profile", PROFILE);
        componentSerializers.put("minecraft:tooltip_display", TOOLTIP_DISPLAY);
        componentSerializers.put("minecraft:lore", LORE);
    }

    abstract @NotNull Object serialize(final Object value);

    abstract @NotNull T deserialize(final @NotNull Object value);

    protected T cast(final @NotNull Object value) {
        try {
            final @SuppressWarnings("unchecked") T typedValue = (T) value;
            return typedValue;
        } catch (Exception e) {
            final RuntimeException exception = new RuntimeException("Failed to serialize value from type " + value.getClass().getName() + ". This is a bug");
            ERROR_TRACKER.trackError(exception);
            throw exception;
        }
    }

    public static Object serializeItemStack(final @NotNull ItemStack itemStack) {
        return ITEM_STACK.serialize(itemStack);
    }

    public static ItemStack deserializeItemStack(final @NotNull ConfigSection section) {
        return ITEM_STACK.deserialize(resolveSection(section));
    }

    private static Map<String, Object> resolveSection(final ConfigSection section) {
        final Map<String, Object> map = new LinkedHashMap<>();
        for (final String key : section.getKeys(false, true)) {
            final Object value = section.get(key);
            if (value instanceof ConfigSection nestedSection) {
                map.put(key, resolveSection(nestedSection));
            } else {
                map.put(key, value);
            }
        }
        return map;
    }
}
