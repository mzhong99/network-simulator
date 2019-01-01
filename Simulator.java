import javafx.event.*;
import javafx.stage.*;
import javafx.scene.*;

import javafx.scene.control.*;
import javafx.scene.canvas.*;
import javafx.scene.layout.*;
import javafx.scene.shape.*;
import javafx.scene.input.*;

import javafx.scene.paint.Color;

import java.util.*;

public class Simulator {

    private HashMap<String, Color> colors;
    private String simulationName;
    
    private Grid grid;
    private Stage actorStage;

    private List<List<Rectangle>> tileClickables;

    private final int TILE_GAP = 3;
    private final int TILE_LENGTH = 20;
    private final int TOP_BORDER = 50;
    private final int LEFT_BORDER = 50;

    private void initColors() {
        
        colors = new HashMap<String, Color>();
        colors.put("floor", Color.WHEAT);
        colors.put("wall", Color.DARKSLATEGRAY);
    }

    private void initStage() {
        
        actorStage = new Stage();
        actorStage.setTitle(simulationName);

        Group root = new Group();
        Pane overlay = new Pane();
        
        final int TILE_SPACE = TILE_GAP + TILE_LENGTH;

        Canvas canvas = new Canvas((2 * LEFT_BORDER) + (grid.getWidth() * TILE_SPACE),
                                   (2 * TOP_BORDER) + (grid.getHeight() * TILE_SPACE));

        tileClickables = new ArrayList<List<Rectangle>>();

        for (int r = 0; r < grid.getHeight(); r++) {
            
            List<Rectangle> line = new ArrayList<Rectangle>();
            
            for (int c = 0; c < grid.getWidth(); c++) {

                final int row = r;
                final int col = c;
                
                Rectangle current = new Rectangle(TILE_LENGTH, TILE_LENGTH);
                
                current.setX(LEFT_BORDER + (c * (TILE_LENGTH + TILE_GAP)));
                current.setY(TOP_BORDER + (r * (TILE_LENGTH + TILE_GAP)));

                current.setStroke(colors.get(grid.getAttributeAt(r, c)));
                current.setFill(colors.get(grid.getAttributeAt(r, c)).darker());
                
                current.setOnMousePressed(new EventHandler<MouseEvent>() {

                    @Override
                    public void handle(MouseEvent event) {

                        System.out.printf("Tile at %d %d pressed\n", row, col);
                        System.out.printf("    Attribute: %s\n", grid.getAttributeAt(row, col));
                    }
                });

                overlay.getChildren().addAll(current);
                line.add(current);

            }
            
            tileClickables.add(line);
        }

        root.getChildren().addAll(canvas, overlay);
        actorStage.setScene(new Scene(root, 1280, 720));
        actorStage.show();
                
    }

    public Simulator(int width, int height, String simulationName, String[][] attributes) {

        initColors();

        this.simulationName = simulationName;
        this.grid = new Grid(width, height);

        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                grid.setAttributeAt(r, c, attributes[r][c]);
            }
        }

        initStage();
    }

    public void show() {
        actorStage.show();
    }
}

