package map.quad;

import java.awt.Point;

@SuppressWarnings("serial")
public class Quad extends Point {
    private QuadType _type;

    public Quad(QuadType type) {
        this(0, 0, type);
    }

    public Quad(int x, int y, QuadType type) {
        super(x, y);
        _type = type;
    }

    public QuadType getType() {
        return _type;
    }

    public void setType(QuadType type) {
        _type = type;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + _type.ordinal();
    }
    
    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }
}
