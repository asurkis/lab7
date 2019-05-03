package collection;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;

public class CollectionElement implements Serializable, Cloneable, Comparable<CollectionElement> {
    private String name;
    private double size;
    private Position position;
    private LocalDateTime creationDate = LocalDateTime.now();

    public CollectionElement(String name, double size, Position position) {
        this.name = name;
        this.size = size;
        this.position = position.clone();
    }

    public CollectionElement(String name, double size, double x, double y) {
        this(name, size, new Position(x, y));
    }

    public CollectionElement() {
        this("", 0, 0, 0);
    }

    @Override
    public CollectionElement clone() {
        return new CollectionElement(name, size, position);
    }

    @Override
    public int compareTo(CollectionElement collectionElement) {
        return Double.compare(size, collectionElement.size);
    }

    @Override
    public String toString() {
        return String.format("{ name: %s; size: %f; position: %s; created: %s }",
                name == null || name.isEmpty() ? "<empty>" : name,
                size, position, creationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(CollectionElement.class, name, size, position, creationDate);
    }

    public String getName() {
        return name;
    }

    public double getSize() {
        return size;
    }

    public Position getPosition() {
        return position;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSize(double size) {
        this.size = size;
    }

    public void setPosition(Position position) {
        this.position = position;
    }
}
