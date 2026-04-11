package server;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStore {

    private final ConcurrentHashMap<String, String> stringStore;
    private final ConcurrentHashMap<String, Set<String>> setStore;

    public InMemoryStore() {
        stringStore = new ConcurrentHashMap<>();
        setStore = new ConcurrentHashMap<>();
    }

    public void set(String key, String value) {
        stringStore.put(key, value);
    }

    public String get(String key) {
        return stringStore.get(key);
    }

    public void delete(String key) {
        stringStore.remove(key);
    }

    public void sAdd(String key, String... values) {
        var set = setStore.computeIfAbsent(key, v -> new HashSet<>());

        set.addAll(Arrays.asList(values));
    }

    public void sRem(String key, String... values) {

        var set = setStore.get(key);

        for (String value : values) {
            if (set != null) set.remove(value);

        }

    }

    public boolean sIsMember(String key, String value) {
        var set = setStore.get(key);
        if (set == null) return false;
        return set.contains(value);
    }

    public Set<String> sMembers(String key) {
        var set = setStore.get(key);
        if (set == null) return new HashSet<>();

        return Set.copyOf(set);
    }

    public void purge(String key) {
        stringStore.remove(key);
        setStore.remove(key);
    }

}
