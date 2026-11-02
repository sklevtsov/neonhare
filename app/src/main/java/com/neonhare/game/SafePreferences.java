package com.neonhare.game;

import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** A malformed or old preference value cannot crash profile loading with ClassCastException. */
final class SafePreferences implements SharedPreferences {

    private final SharedPreferences delegate;

    SafePreferences(SharedPreferences delegate) {
        this.delegate = delegate;
    }

    private Object value(String key) {
        return delegate.getAll().get(key);
    }

    @Override
    public Map<String, ?> getAll() {
        return delegate.getAll();
    }

    @Override
    public String getString(String key, String fallback) {
        try {
            return delegate.getString(key, fallback);
        } catch (ClassCastException error) {
            return fallback;
        }
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> fallback) {
        Object v = value(key);
        if (!(v instanceof Set<?>)) {
            return fallback;
        }
        Set<String> result = new HashSet<>();
        for (Object entry : (Set<?>) v) {
            if (entry instanceof String) {
                result.add((String) entry);
            }
        }
        return result;
    }

    @Override
    public int getInt(String key, int fallback) {
        try {
            return delegate.getInt(key, fallback);
        } catch (ClassCastException error) {
            return fallback;
        }
    }

    @Override
    public long getLong(String key, long fallback) {
        try {
            return delegate.getLong(key, fallback);
        } catch (ClassCastException error) {
            return fallback;
        }
    }

    @Override
    public float getFloat(String key, float fallback) {
        try {
            float n = delegate.getFloat(key, fallback);
            return Float.isNaN(n) || Float.isInfinite(n) ? fallback : n;
        } catch (ClassCastException error) {
            return fallback;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean fallback) {
        try {
            return delegate.getBoolean(key, fallback);
        } catch (ClassCastException error) {
            return fallback;
        }
    }

    @Override
    public boolean contains(String key) {
        return delegate.contains(key);
    }

    @Override
    public Editor edit() {
        return delegate.edit();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(
            OnSharedPreferenceChangeListener listener) {
        delegate.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(
            OnSharedPreferenceChangeListener listener) {
        delegate.unregisterOnSharedPreferenceChangeListener(listener);
    }
}
