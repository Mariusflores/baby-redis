package io.babyredis.server;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.babyredis.error.BabyRedisException;
import io.babyredis.server.engine.CommandExecutor;
import io.babyredis.server.engine.ExpiryManager;
import io.babyredis.server.persistence.AppendOnlyPersistence;
import io.babyredis.server.persistence.SnapshotData;
import io.babyredis.server.persistence.SnapshotPersistence;
import io.babyredis.server.store.InMemoryStore;
import io.babyredis.server.store.StoreState;

public class BabyRedisServer {
    private final SnapshotPersistence snapshotManager;
    private final AppendOnlyPersistence aofManager;
    private static final Logger log = LoggerFactory.getLogger(BabyRedisServer.class);
    private final Set<Socket> activeConnections;

    private final InMemoryStore store = new InMemoryStore();
    private final CommandExecutor commandExecutor;
    private final ExpiryManager expiryManager = new ExpiryManager(store::purge);

    public BabyRedisServer(
            SnapshotPersistence snapshotManager,
            AppendOnlyPersistence aofManager) throws IOException {
        // Retrieves instigates snapshot retrieval
        this.snapshotManager = snapshotManager;
        this.aofManager = aofManager;
        commandExecutor = new CommandExecutor(aofManager, store, expiryManager);
        replayState();
        activeConnections = ConcurrentHashMap.newKeySet();


        // Daemon thread, Sync snapshot every 30 seconds
        // TODO confirm snapshot throws on error
        Runnable syncSnapshot = () -> {
            while (true) {
                try {
                    Thread.sleep(30000);

                    saveSnapshot();
                } catch (InterruptedException e) {
                    throw new BabyRedisException(
                            "Error occurred while syncing snapshot",
                            e);
                }
            }

        };

        Thread.ofVirtual().start(syncSnapshot);
    }

    public String executeCommand(String command) {
        return commandExecutor.execute(command);
    }



    public void addSocket(Socket s) {
        activeConnections.add(s);
    }

    public void closeSocket(Socket s) throws IOException {
        if (activeConnections.contains(s)) {
            activeConnections.remove(s);
            s.close();
        }
    }

    public void close() throws IOException {
        saveSnapshot();
        closeAllConnections();
        try {
            aofManager.close();
        } catch (Exception e) {
            log.error("Error closing AOF manager", e);
        }
    }

    private void closeAllConnections() {
        for (Socket s : Set.copyOf(activeConnections)) {
            try {
                closeSocket(s);
            } catch (IOException e) {
                log.error("Error closing socket", e);
            }
        }
    }

    private void replayState() {

        SnapshotData snapshot = snapshotManager.load();
        store.loadState(new StoreState(snapshot.stringSnapshot(), snapshot.setSnapshot()));

        int lastSnapshotSequence = snapshotManager.getLastSequence();
        expiryManager.setQueueState(snapshot.expiryQueueSnapshot());

        expiryManager.loadQueue();

        replayAOF(lastSnapshotSequence);

    }

    private void replayAOF(int lastSnapshotSequence) {
        List<String> commands = aofManager.loadAfter(lastSnapshotSequence);
        for (String command : commands) {
            commandExecutor.execute(command, true);
        }
    }

    private void saveSnapshot() {
        StoreState state = store.exportState();
        int sequence = aofManager.getCurrentSequence();
        snapshotManager.save(new SnapshotData(state.stringStore(), state.setStore(), expiryManager.getQueueState()), sequence);
    }

}
