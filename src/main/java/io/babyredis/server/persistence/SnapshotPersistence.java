package io.babyredis.server.persistence;

public interface SnapshotPersistence {

    public void save(SnapshotData snapshotData);

    public SnapshotData load();
    
}
