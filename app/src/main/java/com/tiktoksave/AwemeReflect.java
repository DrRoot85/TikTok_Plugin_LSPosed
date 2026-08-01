package com.tiktoksave;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection accessors for TikTok model classes.
 * Field names follow the public modded-TikTok dex (45.5.3) which matches the
 * official 41.3.x codebase; every access is cached and null-safe.
 */
public final class AwemeReflect {

    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Method>> METHOD_CACHE = new ConcurrentHashMap<>();

    private AwemeReflect() {
    }

    /** Find a field by any of the given names walking up the class hierarchy. */
    public static Object getField(Object obj, String... names) {
        if (obj == null) return null;
        try {
            Class<?> cls = obj.getClass();
            ConcurrentHashMap<String, Field> cache = FIELD_CACHE.computeIfAbsent(cls, k -> new ConcurrentHashMap<>());
            for (String name : names) {
                Field f = cache.get(name);
                if (f == null) {
                    f = findField(cls, name);
                    if (f != null) cache.put(name, f);
                }
                if (f != null) {
                    f.setAccessible(true);
                    return f.get(obj);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Field findField(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    /** Call a no-arg getter if it exists. */
    public static Object callGetter(Object obj, String name) {
        if (obj == null) return null;
        try {
            Class<?> cls = obj.getClass();
            ConcurrentHashMap<String, Method> cache = METHOD_CACHE.computeIfAbsent(cls, k -> new ConcurrentHashMap<>());
            Method m = cache.get(name);
            if (m == null) {
                m = findMethod(cls, name);
                if (m != null) cache.put(name, m);
            }
            if (m != null) {
                m.setAccessible(true);
                return m.invoke(obj);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Method findMethod(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                for (Method m : c.getDeclaredMethods()) {
                    if (m.getName().equals(name) && m.getParameterCount() == 0) return m;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    /** Aweme id. */
    public static String awemeId(Object aweme) {
        Object id = getField(aweme, "aid", "awemeId");
        if (id == null) id = callGetter(aweme, "getAid");
        return id != null ? String.valueOf(id) : null;
    }

    /** First usable URL string from a UrlModel (urlList or uri). */
    public static String firstUrl(Object urlModel) {
        if (urlModel == null) return null;
        try {
            Object list = getField(urlModel, "urlList");
            if (list instanceof List) {
                List<?> l = (List<?>) list;
                if (!l.isEmpty() && l.get(0) != null) return String.valueOf(l.get(0));
            }
            Object uri = getField(urlModel, "uri");
            if (uri != null) return String.valueOf(uri);
        } catch (Throwable ignored) {
        }
        return null;
    }
}
