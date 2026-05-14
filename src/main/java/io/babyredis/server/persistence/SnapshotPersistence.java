package io.babyredis.server.persistence;

public interface SnapshotPersistence {

    public void save(SnapshotData snapshotData, int sequence);

    public SnapshotData load();

    public int getLastSequence();
    
}
