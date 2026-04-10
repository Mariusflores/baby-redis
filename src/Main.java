public class Main {
    public static void main(String[] args) {
        InMemoryStore store = new InMemoryStore();
        String key = "foo";
        String value = "bar";
        store.set(key, value);

        String returned = store.get("foo");

        if(returned.equals(value)){
            System.out.println("Goated Store");
        }else{
            System.out.println("Nah fam");
        }

        store.delete("foo");
        String deletedValue = store.get("foo");

        System.out.println("Deleted value: " + deletedValue);
    }
}
