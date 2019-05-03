package collection;

import java.io.Serializable;
import java.util.Objects;

public class Position implements Cloneable, Serializable {
    private double x, y;

    public Position(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Position() {
        this(0, 0);
    }

    @Override
    public Position clone() {
        return new Position(x, y);
    }

    @Override
    public String toString() {
        return String.format("(%f; %f)", x, y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }
}
