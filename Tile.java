import java.util.List;
import java.util.ArrayList;

import java.lang.IllegalArgumentException;

import javafx.scene.shape.*;

public class Tile {
    
    private final int row;
    private final int col;
    private final Grid grid;
    private String attribute;
    private Rectangle rectangle;

    private boolean isInRange(Tile tile) {
        return tile.row >= 0 && tile.row < grid.getHeight()
            && tile.col >= 0 && tile.col < grid.getWidth();
    }

    private boolean isTraversable(Tile tile) {
        if (tile == null) {
            return false;
        }
        return !("wall".equals(tile.attribute));
    }

    public Tile(int row, int col, Grid grid) {
        this(row, col, grid, "wall");
    }

    public Tile(int row, int col, Grid grid, String attribute) {
        
        this.row = row;
        this.col = col;

        if (grid == null) {
            throw new IllegalArgumentException("Tile received null grid");
        }

        this.grid = grid;
        this.attribute = attribute;
    }

    public Tile(Tile rhs) {
        this.row = rhs.row;
        this.col = rhs.col;
        this.grid = rhs.grid;
        this.attribute = rhs.attribute;
    }

    public int getRow()             { return this.row;       }
    public int getCol()             { return this.col;       }
    public String getAttribute()    { return this.attribute; }
    public Rectangle getRectangle() { return this.rectangle; }

    public void setAttribute(String attribute)    { this.attribute = attribute; }
    public void setRectangle(Rectangle rectangle) { this.rectangle = rectangle; }

    private Tile up()    { return new Tile(row - 1, col, grid); }
    private Tile down()  { return new Tile(row + 1, col, grid); }
    private Tile left()  { return new Tile(row, col - 1, grid); }
    private Tile right() { return new Tile(row, col + 1, grid); }
    
    public List<Tile> getNeighbors() {
        
        if ("wall".equals(attribute)) {
            return new ArrayList<Tile>();
        }

        List<Tile> neighborsRaw      = new ArrayList<Tile>();
        List<Tile> neighborsFiltered = new ArrayList<Tile>();

        neighborsRaw.add(this.up()   );
        neighborsRaw.add(this.down() );
        neighborsRaw.add(this.left() );
        neighborsRaw.add(this.right());

        for (Tile tile : neighborsRaw) {
            if (isInRange(tile)) {
                neighborsFiltered.add(tile);
            }
        }

        neighborsRaw      = neighborsFiltered;
        neighborsFiltered = new ArrayList<Tile>();

        for (Tile tile : neighborsRaw) {
            tile.setAttribute(grid.getAttributeAt(tile.getRow(), tile.getCol()));
            if (isTraversable(tile)) {
                neighborsFiltered.add(tile);
            }
        }

        return neighborsFiltered;
    }

    @Override
    public int hashCode() {
        return (row << 16) + col;
    }

    @Override
    public boolean equals(Object rhs) {
        return row == ((Tile)rhs).row && col == ((Tile)rhs).col;
    }
}
