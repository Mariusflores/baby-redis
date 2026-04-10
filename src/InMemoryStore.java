import java.util.HashMap;

public class InMemoryStore {

    private HashMap<String, String> store;

    public InMemoryStore(){
        store = new HashMap<>();
    }

    public void set(String key, String value){
        store.put(key, value);
    }

    public String get(String key){
        return store.get(key);
    }

    public void delete(String key){
        store.remove(key);
    }
    
}
