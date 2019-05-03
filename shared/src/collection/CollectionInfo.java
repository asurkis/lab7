package collection;

import java.io.Serializable;
import java.time.LocalDateTime;

public class CollectionInfo implements Serializable {
    private LocalDateTime creationDate;
    private int elementCount;

    public CollectionInfo(LocalDateTime creationDate, int elementCount) {
        this.creationDate = creationDate;
        this.elementCount = elementCount;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public int getElementCount() {
        return elementCount;
    }
}
