package io.babyredis.server.store;
import java.util.Map;
import java.util.Set;

public record StoreState(
    Map<String, String> stringStore,
    Map<String, Set<String>> setStore
) {

}
