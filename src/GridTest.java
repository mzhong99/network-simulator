import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;
import java.util.Map;

import java.lang.IllegalArgumentException;

public class GridTest {

    private Grid grid;
    private static final int MAX_GRID_LENGTH = 100;

    @Before
    public void setUp() {
        grid = new Grid((int)(Math.random() * MAX_GRID_LENGTH) + 10, 
                        (int)(Math.random() * MAX_GRID_LENGTH) + 10);
    }

    @Test
    public void testConstructorExceptions() {

        Exception exception = null;
        
        try {
            Grid grid = new Grid(0, 1);
        }
        catch (IllegalArgumentException ex) {
            exception = ex;
        }

        assertNotNull(exception);
        assertTrue(exception instanceof IllegalArgumentException);

        exception = null;

        try {
            Grid grid = new Grid(1, 0);
        }
        catch (IllegalArgumentException ex) {
            exception = ex;
        }

        assertNotNull(exception);
        assertTrue(exception instanceof IllegalArgumentException);
    }

    @Test
    public void testDefaultConstruction() {

        for (int r = 0; r < grid.getHeight(); r++) {
            for (int c = 0; c < grid.getWidth(); c++) {
                assertEquals("wall", grid.getAttributeAt(r, c));
            }
        }
        
        Set<Tile> validStartingTiles = grid.getValidTiles();
        assertTrue(validStartingTiles.isEmpty());

        Map<Tile, List<Tile>> adjacency = grid.getAdjacencyMap();

        for (Tile tile : adjacency.keySet()) {
            List<Tile> neighbors = adjacency.get(tile);
            assertTrue(neighbors.isEmpty());
            assertEquals(0, neighbors.size());
        }
    }

    @Test
    public void testFullLinkage() {

        for (int r = 0; r < grid.getHeight(); r++) {
            for (int c = 0; c < grid.getWidth(); c++) {
                grid.setAttributeAt(r, c, "floor");
            }
        }

        Set<Tile> validTiles = grid.getValidTiles();
        Map<Tile, List<Tile>> adjacency = grid.getAdjacencyMap();
        assertEquals(grid.getWidth() * grid.getHeight(), validTiles.size());
        
        for (int r = 1; r < grid.getHeight() - 1; r++) {
            for (int c = 1; c < grid.getWidth() - 1; c++) {
                Tile key = new Tile(r, c, grid);
                List<Tile> neighbors = adjacency.get(key);
                assertEquals(4, neighbors.size());
            }
        }


        for (int r = 1; r < grid.getHeight() - 1; r++) {
            assertEquals(3, adjacency.get(new Tile(r, 0                  , grid)).size());
            assertEquals(3, adjacency.get(new Tile(r, grid.getWidth() - 1, grid)).size());
        }

        for (int c = 1; c < grid.getWidth() - 1; c++) {
            assertEquals(3, adjacency.get(new Tile(0                   , c, grid)).size());
            assertEquals(3, adjacency.get(new Tile(grid.getHeight() - 1, c, grid)).size());
        }

        Tile topLeft     = new Tile(0                   , 0                  , grid);
        Tile topRight    = new Tile(0                   , grid.getWidth() - 1, grid);
        Tile bottomLeft  = new Tile(grid.getHeight() - 1, 0                  , grid);
        Tile bottomRight = new Tile(grid.getHeight() - 1, grid.getWidth() - 1, grid);
        
        assertEquals(2, adjacency.get(topLeft    ).size());
        assertEquals(2, adjacency.get(topRight   ).size());
        assertEquals(2, adjacency.get(bottomLeft ).size());
        assertEquals(2, adjacency.get(bottomRight).size());

        for (int r = 1; r < grid.getHeight() - 1; r++) {
            for (int c = 1; c < grid.getWidth() - 1; c++) {
                grid.setAttributeAt(r, c, "wall");
            }
        }

        validTiles = grid.getValidTiles();
        adjacency = grid.getAdjacencyMap();

        for (int r = 0; r < grid.getHeight(); r++) {
            assertEquals(2, adjacency.get(new Tile(r, 0                  , grid)).size());
            assertEquals(2, adjacency.get(new Tile(r, grid.getWidth() - 1, grid)).size());
        }

        for (int c = 0; c < grid.getWidth(); c++) {
            assertEquals(2, adjacency.get(new Tile(0                   , c, grid)).size());
            assertEquals(2, adjacency.get(new Tile(grid.getHeight() - 1, c, grid)).size());
        }

        assertEquals((2 * (grid.getWidth() + grid.getHeight())) - 4, validTiles.size());
        
        for (int r = 1; r < grid.getHeight() - 1; r++) {
            for (int c = 1; c < grid.getWidth() - 1; c++) {
                assertFalse(validTiles.contains(new Tile(r, c, grid)));
            }
        }
    }
}
