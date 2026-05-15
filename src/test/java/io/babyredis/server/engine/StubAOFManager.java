package io.babyredis.server.engine;

import java.util.ArrayList;
import java.util.List;

import io.babyredis.server.persistence.AppendOnlyPersistence;

public class StubAOFManager implements AppendOnlyPersistence {

    private final List<String> commands = new ArrayList<>();

    private int currentSequence = 0;

    @Override
    public void close() throws Exception {
        // No op
    }

    @Override
    public void logCommand(String command) {
        currentSequence++;
        commands.add(command);
    }

    @Override
    public List<String> loadAfter(int lastSequence) {
            return new ArrayList<>();
    }

    @Override
    public int getCurrentSequence() {
    
        return currentSequence;
    }

    public List<String> getLoggedCommands() {
        return List.copyOf(commands);
    }

}
