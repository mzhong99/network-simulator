import java.util.List;
import java.util.Set;
import java.util.Map;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

import java.util.Stack;

import java.lang.IllegalArgumentException;

import javafx.scene.shape.Rectangle;

public class Grid {
    
    private List<List<Tile>> grid;
    private Set<Tile> validTiles;

    private final int width;
    private final int height;
    
    private Tile getTileAt(int row, int col) {

        if (row < 0 || row >= height || col < 0 || col >= width) {
            throw new IllegalArgumentException("Attempted to access tile out of bounds");
        }

        return grid.get(row).get(col);
    }

    public Grid(int width, int height) {
        
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height less than zero");
        }
        
        this.width  = width;
        this.height = height;
        
        this.grid = new ArrayList<List<Tile>>();
        
        for (int r = 0; r < height; r++) {
            
            List<Tile> line = new ArrayList<Tile>();
            
            for (int c = 0; c < width; c++) {
                line.add(new Tile(r, c, this));
            }
            
            grid.add(line);
        }

        this.validTiles = new HashSet<Tile>();
    }
    
    public int getWidth()  { return width;  }
    public int getHeight() { return height; }
    
    public void setAttributeAt(int row, int col, String attribute) {
        
        if (row < 0 || row >= height || col < 0 || col >= width) {
            throw new IllegalArgumentException("Attempted to set out of bounds");
        }
        
        String oldAttribute = this.getAttributeAt(row, col);
        boolean isTurnedOn  =  ("wall".equals(oldAttribute)) && !("wall".equals(attribute));
        boolean isTurnedOff = !("wall".equals(oldAttribute)) &&  ("wall".equals(attribute));

        grid.get(row).get(col).setAttribute(attribute);
        
        if (isTurnedOn) {
            validTiles.add(getTileAt(row, col));
        }

        if (isTurnedOff) {
            validTiles.remove(getTileAt(row, col));
        }
    }

    public String getAttributeAt(int row, int col) {

        if (row < 0 || row >= height || col < 0 || col >= width) {
            throw new IllegalArgumentException("Attempted to access attribute out of bounds");
        }

        return getTileAt(row, col).getAttribute();
    }
    
    public void setRectangleAt(int row, int col, Rectangle rectangle) {

        if (row < 0 || row >= height || col < 0 || col >= width) {
            throw new IllegalArgumentException("Attempted to set rectangle out of bounds");
        }

        getTileAt(row, col).setRectangle(rectangle);
    }

    public Rectangle getRectangleAt(int row, int col) {

        if (row < 0 || row >= height || col < 0 || col >= width) {
            throw new IllegalArgumentException("Attempted to access rectangle out of bounds");
        }

        return getTileAt(row, col).getRectangle();
    }

    public Set<Tile> getValidTiles() {
        
        Set<Tile> validTilesCopy = new HashSet<Tile>();

        for (Tile tile : validTiles) {
            validTilesCopy.add(new Tile(tile));
        }

        return validTilesCopy;
    }

    public Map<Tile, List<Tile>> getAdjacencyMap() {
        
        Map<Tile, List<Tile>> adjacency = new HashMap<Tile, List<Tile>>();
        
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                Tile cursor = getTileAt(r, c);
                adjacency.put(cursor, cursor.getNeighbors());
            }
        }
        
        return adjacency;
    }

    public boolean isContiguous() {
        
        Set<Tile> tilesToExplore = getValidTiles();
        Map<Tile, List<Tile>> adjacency = getAdjacencyMap();

        if (validTiles.size() == 0) {
            return false;
        }

        Tile start = tilesToExplore.iterator().next();
        Stack<Tile> dfs = new Stack<Tile>();
        
        dfs.push(start);
        tilesToExplore.remove(start);
        
        while (!dfs.empty()) {
            Tile current = dfs.pop();
            for (Tile neighbor : adjacency.get(current)) {
                if (tilesToExplore.contains(neighbor)) {
                    dfs.add(neighbor);
                    tilesToExplore.remove(neighbor);
                }
            }
        }

        return tilesToExplore.isEmpty();
    }
}
