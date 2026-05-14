package io.babyredis.server.persistence;

import java.util.List;

public interface AppendOnlyPersistence extends AutoCloseable {
    void logCommand(String command);
    List<String> loadAfter(int lastSequence);
    int getCurrentSequence();
    
}
