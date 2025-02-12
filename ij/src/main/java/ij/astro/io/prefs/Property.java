package ij.astro.io.prefs;

import ij.Prefs;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @see PropertyKey for more control over the property key.
 *
 * @param <T> the type of the property.
 */
public class Property<T> {
    private final Object owner;
    private final Class<?> ownerClass;
    private T value;

    private String propertyKey;
    private boolean hasBuiltKey = false;
    private boolean hasLoaded = false;
    private final Function<String, T> deserializer;
    private final Supplier<String> keySuffix;
    private final Supplier<String> keyPrefix;
    private final Class<T> type;
    final HashSet<PropertyChangeListener<T>> listeners = new HashSet<>();

    public Property(T defaultValue, Object owner) {
        this(defaultValue, "", "", $ -> null, owner);
    }

    public Property(T defaultValue, Function<String, T> deserializer, Object owner) {
        this(defaultValue, "", "", deserializer, owner);
    }

    public Property(T defaultValue, String keyPrefix, String keySuffix, Object owner) {
        this(defaultValue, keyPrefix, keySuffix, $ -> null, owner);
    }

    public Property(T defaultValue, Supplier<String> keyPrefix, Supplier<String> keySuffix, Object owner) {
        this(defaultValue, keyPrefix, keySuffix, $ -> null, owner);
    }

    public Property(T defaultValue, String keyPrefix, String keySuffix, Function<String, T> deserializer, Object owner) {
        this(defaultValue, () -> keyPrefix, () -> keySuffix, deserializer, owner);
    }

    @SuppressWarnings({"unchecked"})
    public Property(T defaultValue, Supplier<String> keyPrefix, Supplier<String> keySuffix,
                    Function<String, T> deserializer, Object owner) {
        this.deserializer = deserializer;
        this.keySuffix = keySuffix;
        this.keyPrefix = keyPrefix;
        this.type = (Class<T>) getType(defaultValue);
        value = defaultValue;
        if (owner instanceof Class<?> clazz) {
            this.ownerClass = clazz;
            this.owner = null;
        } else {
            this.owner = owner;
            this.ownerClass = owner.getClass();
        }
    }

    public T get() {
        loadProperty();
        return value;
    }

    public void set(T value) {
        updatePrefs(value);
        listeners.forEach(l -> l.valueChanged(getPropertyKey(), value));
        this.value = value;
    }

    public String getPropertyKey() {
        return getOrCreatePropertyKey();
    }

    public <X> X ifProp(X truthy) {
        return ifProp(truthy, null);
    }

    public <X> X ifProp(X truthy, X falsy) {
        if (type == Boolean.TYPE || type == Boolean.class) {
            if (((Boolean) get())) {
                return truthy;
            }
        }

        return falsy;
    }

    public void ifProp(Runnable truthy) {
        ifProp(truthy, () -> {});
    }

    public void ifProp(Runnable truthy, Runnable falsy) {
        if (type == Boolean.TYPE || type == Boolean.class) {
            if (((Boolean) get())) {
                truthy.run();
            }
        }

        falsy.run();
    }

    public boolean hasSaved() {
        return Prefs.ijPrefs.containsKey(Prefs.KEY_PREFIX+getPropertyKey());
    }

    public void addListener(PropertyChangeListener<T> listener) {
        listeners.add(listener);
    }

    private void loadProperty() {
        if (!hasLoaded) {
            try {
                value = handleLoad();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!type.isInstance(value)) {
                throw new IllegalStateException("Expected type: %s, received type %s"
                        .formatted(type, value == null ? null : value.getClass()));
            }

            hasLoaded = true;
        }
    }

    private String getOrCreatePropertyKey() {
        if (!hasBuiltKey) {
            for (Field declaredField : ownerClass.getDeclaredFields()) {
                if (Property.class.isAssignableFrom(declaredField.getType())) {
                    try {
                        if (!declaredField.canAccess(owner)) {
                            declaredField.trySetAccessible();
                        }
                        if (this.equals(declaredField.get(owner))) {
                            var pk = declaredField.getAnnotation(PropertyKey.class);
                            var gs = declaredField.toGenericString().split(" ");
                            if (pk != null) {
                                if (pk.ignoreAffixes()) {
                                    propertyKey = pk.value();
                                } else {
                                    propertyKey = keyPrefix.get() + pk.value() + keySuffix.get();
                                }
                            } else {
                                propertyKey = keyPrefix.get() + gs[gs.length - 1] + keySuffix.get();
                            }

                            hasBuiltKey = true;
                            break;
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return propertyKey;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private T handleLoad() {
        //todo add safeties for failed to parse, Optional?
        var nv = Prefs.get(getPropertyKey(), String.valueOf(value));
        if (type == Double.TYPE || type == Double.class) {
            return (T) Double.valueOf(nv);
        } else if (type == Integer.TYPE || type == Integer.class) {
            return (T) Integer.valueOf(nv);
        } else if (type == Boolean.TYPE || type == Boolean.class) {
            return (T) Boolean.valueOf(nv);
        } else if (type == String.class) {
            return (T) nv;
        } else if (type.isEnum()) {
            return (T) Enum.valueOf((Class<? extends Enum>) type, nv);
        } else if (type == Point.class) {
            var v = Prefs.getLocation(getPropertyKey());
            return v == null ? value : (T) v;
        } /*else if (type == EnumSet.class) {
            //todo handle Serializable objects? byte array to string? will it have special chars that break things?
        }*/

        return deserializer.apply(nv);
    }

    private void updatePrefs(T value) {
        if (type == Point.class) {
            Prefs.saveLocation(getPropertyKey(), (Point) value);
            return;
        } else if (type.isEnum()) {
            Prefs.set(getPropertyKey(), ((Enum<?>) value).name());
        }

        Prefs.set(getPropertyKey(), value.toString());
    }

    private static Class<?> getType(Object o) {
        if (o == null) {
            throw new IllegalArgumentException("default value may not be null");
        }

        var c = o.getClass();
        var s = c.getSuperclass();
        if (s != null && s.isEnum()) {
            return s;
        }

        return c;
    }

    @FunctionalInterface
    public interface PropertyChangeListener<T> {
        void valueChanged(String key, T newValue);
    }

    @Override
    public String toString() {
        return "Property{" +
                "propertyKey='" + getPropertyKey() + '\'' +
                ", owner=" + owner +
                ", heldValue=" + value +
                ", type=" + type +
                '}';
    }
}