package db;

import collection.CollectionElement;
import collection.CollectionInfo;

import java.util.List;

public interface Database extends AutoCloseable {
    List<CollectionElement> show();
    CollectionInfo info();
    void addElement(CollectionElement element);
    void removeElement(CollectionElement element);
    void removeFirst();
    void removeLast();
    void add_user(String email, String password);
    boolean check_user(String email, String password);
}
