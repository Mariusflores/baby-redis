package io.babyredis.server.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.babyredis.server.persistence.SnapshotData;
import io.babyredis.server.persistence.SnapshotManager;

/**
 * A simple in-memory store for handling string and set operations.
 * The InMemoryStore class provides methods for setting and getting string key-value pairs, adding and removing members from sets, checking set membership, and retrieving all members of a set.
 * It uses ConcurrentHashMap to store the string and set data, allowing for thread-safe operations in a concurrent environment.
 * The class also includes a method for purging keys from both the string and set stores, as well as methods for writing and reading snapshots of the current state of the in-memory store using the SnapshotManager.
 */
public class InMemoryStore {


    private final ConcurrentHashMap<String, String> stringStore;
    private final ConcurrentHashMap<String, Set<String>> setStore;
    private final SnapshotManager snapshotManager;

    /**
     * Constructs a new InMemoryStore with the specified SnapshotManager. The InMemoryStore uses the SnapshotManager to handle the writing and reading of snapshots of the in-memory data, allowing for persistence of the server's state across restarts.
     * The constructor initializes the string and set stores as ConcurrentHashMaps, providing thread-safe access to the in-memory data.
     *
     * @param snapshotManager the SnapshotManager instance used for handling snapshot operations for the in-memory store
     */
    public InMemoryStore(SnapshotManager snapshotManager) {
        this.snapshotManager = snapshotManager;
        stringStore = new ConcurrentHashMap<>();
        setStore = new ConcurrentHashMap<>();
    }

    /**
     * Sets a string value for the specified key in the in-memory store. This method takes a key and a value as parameters and stores the value in the stringStore map under the given key. If the key already exists, its value will be overwritten with the new value. This method is used to handle the SET command in the Baby Redis server, allowing clients to store string values associated with keys in memory.
     *
     * @param key   the key under which the value will be stored in the in-memory store
     * @param value the string value to be stored in the in-memory store associated with the specified key
     */
    public void set(String key, String value) {
        stringStore.put(key, value);
        
        // If the key exists in the set store, we should remove it to maintain consistency
        setStore.remove(key);
    }

    /**
     * Retrieves the string value associated with the specified key from the in-memory store. This method takes a key as a parameter and returns the corresponding value from the stringStore map. If the key does not exist in the store, it will return null. This method is used to handle the GET command in the Baby Redis server, allowing clients to retrieve string values associated with keys stored in memory.
     *
     * @param key the key for which the associated value will be retrieved from the in-memory store
     * @return the string value associated with the specified key in the in-memory store, or null if the key does not exist
     */
    public String get(String key) {
        return stringStore.get(key);
    }

    /**
     * Deletes the string value associated with the specified key from the in-memory store. This method takes a key as a parameter and removes the corresponding entry from the stringStore map. If the key does not exist in the store, this method will have no effect. This method is used to handle the DEL command in the Baby Redis server, allowing clients to remove string values associated with keys stored in memory.
     *
     * @param key the key for which the associated value will be deleted from the in-memory store
     */

    public void delete(String key) {
        stringStore.remove(key);
    }

    /**
     * Adds one or more values to a set associated with the specified key in the in-memory store. This method takes a key and one or more values as parameters and adds the values to the set stored in the setStore map under the given key. If the key does not exist, a new set will be created for that key. If the key already exists, the new values will be added to the existing set. This method is used to handle the SADD command in the Baby Redis server, allowing clients to add members to sets associated with keys stored in memory.
     *
     * @param key    the key under which the set is stored in the in-memory store
     * @param values one or more values to be added to the set associated with the specified key in the in-memory store
     */
    public void sAdd(String key, String... values) {
        // If the key already exists in the string store, we should remove it to maintain consistency
        stringStore.remove(key);
        var set = setStore.computeIfAbsent(key, v -> new HashSet<>());

        set.addAll(Arrays.asList(values));
    }

    /**
     * Removes one or more values from a set associated with the specified key in the in-memory store. This method takes a key and one or more values as parameters and removes the values from the set stored in the setStore map under the given key. If the key does not exist, this method will have no effect. If the key exists but the values are not present in the set, this method will also have no effect. If all values are removed from the set and it becomes empty, the key will be removed from the setStore map. This method is used to handle the SREM command in the Baby Redis server, allowing clients to remove members from sets associated with keys stored in memory.
     *
     * @param key    the key under which the set is stored in the in-memory store
     * @param values one or more values to be removed from the set associated with the specified key in the in-memory store
     */
    public void sRem(String key, String... values) {
        var set = setStore.get(key);
        if (set != null) {
            set.removeAll(Arrays.asList(values));
            if (set.isEmpty()) {
                setStore.remove(key);
            }
        }
    }

    /**
     * Checks if a value is a member of a set associated with the specified key in the in-memory store. This method takes a key and a value as parameters and checks if the value is present in the set stored in the setStore map under the given key. If the key does not exist, this method will return false. If the key exists but the value is not present in the set, this method will also return false. If the value is present in the set, this method will return true. This method is used to handle the SISMEMBER command in the Baby Redis server, allowing clients to check for membership of values in sets associated with keys stored in memory.
     *
     * @param key   the key under which the set is stored in the in-memory store
     * @param value the value to check for membership in the set associated with the specified key in the in-memory store
     * @return true if the value is a member of the set associated with the specified key in the in-memory store, false otherwise
     */
    public boolean sIsMember(String key, String value) {
        var set = setStore.get(key);
        if (set == null) return false;
        return set.contains(value);
    }

    /**
     * Retrieves all members of a set associated with the specified key from the in-memory store. This method takes a key as a parameter and returns a set of all members associated with that key from the setStore map. If the key does not exist, this method will return an empty set. This method is used to handle the SMEMBERS command in the Baby Redis server, allowing clients to retrieve all members of sets associated with keys stored in memory.
     *
     * @param key the key under which the set is stored in the in-memory store
     * @return a set of all members associated with the specified key in the in-memory store, or an empty set if the key does not exist
     */
    public Set<String> sMembers(String key) {
        var set = setStore.get(key);
        if (set == null) return new HashSet<>();

        return Set.copyOf(set);
    }

    /**
     * Removes all entries associated with the specified key from the in-memory store. This method takes a key as a parameter and removes the key from both the stringStore and setStore maps. If the key does not exist, this method will have no effect. This method is used to handle the DEL command in the Baby Redis server, allowing clients to delete keys and their associated values from memory.
     *
     * @param key the key to be removed from the in-memory store
     */
    public void purge(String key) {
        stringStore.remove(key);
        setStore.remove(key);
    }

    /**
     * Writes a snapshot of the current state of the in-memory store to a file using the SnapshotManager. This method takes a map of expiring keys with their corresponding expiration timestamps as a parameter and passes it along with the current state of the stringStore and setStore to the SnapshotManager's write method. The SnapshotManager will serialize this data and save it to a snapshot file, allowing for persistence of the server's state across restarts.
     *
     * @param expiryMap a map of expiring keys with their corresponding expiration timestamps to be included in the snapshot of the in-memory store
     */
    public void writeSnapshot(Map<String, Long> expiryMap) {
        snapshotManager.save(Map.copyOf(stringStore), Map.copyOf(setStore), expiryMap);
    }

    /**
     * Reads a snapshot of the in-memory store from a file using the SnapshotManager and restores the state of the stringStore and setStore. This method calls the SnapshotManager's read method to retrieve the snapshot data, which includes the string key-value pairs, sets, and expiring keys with their corresponding expiration timestamps. The method then populates the stringStore and setStore with the data from the snapshot, allowing the Baby Redis server to restore its state when it starts up.
     *
     * @return a SnapshotData record containing the restored state of the in-memory store, including string key-value pairs, sets, and expiring keys, read from the snapshot file
     */
    public SnapshotData readSnapshot() {
        SnapshotData data = snapshotManager.load();

        stringStore.putAll(data.stringSnapshot());
        setStore.putAll(data.setSnapshot());

        return data;
    }

    /***
     * Retrieves all keys currently stored in the in-memory store, including both string keys and set keys. 
     * This method iterates through the key sets of both the stringStore and setStore maps, collects all unique keys into a list, 
     * and returns them as an array of strings. This method can be used to handle the KEYS command in the Baby Redis server, 
     * allowing clients to retrieve a list of all keys currently stored in memory.
      *
      * @param pattern a pattern to match keys against, currently only supports "*" to return all keys, and prefix matching with "prefix*"
     * @return an array of strings containing all keys currently stored in the in-memory store, including both string keys and set keys
     */

    public String[] getAllKeysMatchingPattern(String pattern){
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(stringStore.keySet());
        allKeys.addAll(setStore.keySet());

        List<String> matched = new ArrayList<>();

        if (pattern.equals("*")) {
            matched.addAll(allKeys);
        }else if(pattern.endsWith("*")){
            String prefix = pattern.substring(0, pattern.length() - 1);
            for(String key : allKeys){
                if(key.startsWith(prefix)){
                    matched.add(key);
                }
            }
        }

        // Possibly add more complex regex-based pattern 
        // matching in the future

        return matched.toArray(new String[0]);
    }

    /// For testing purposes, we can add a method to 
    // clear all data from the in-memory store

    public List<String> flushMatchingPattern(String pattern){
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(stringStore.keySet());
        allKeys.addAll(setStore.keySet());

        List<String> flushedKeys = new ArrayList<>();

        if (pattern.equals("*")) {
            flushedKeys.addAll(allKeys); // collect all keys before clearing

            setStore.clear();
            stringStore.clear();
        }else if(pattern.endsWith("*")){
            String prefix = pattern.substring(0, pattern.length() - 1);
            for(String key : allKeys){
                if(key.startsWith(prefix)){
                    flushedKeys.add(key);
                    purge(key);

                }
            }
        }

         // Possibly add more complex regex-based pattern 
         // matching in the future

        return flushedKeys;
    }

}
