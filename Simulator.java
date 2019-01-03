import javafx.event.*;
import javafx.stage.*;
import javafx.scene.*;

import javafx.scene.control.*;
import javafx.scene.canvas.*;
import javafx.scene.layout.*;
import javafx.scene.shape.*;
import javafx.scene.input.*;

import javafx.scene.paint.Color;
import javafx.geometry.Insets;

import java.util.*;

public class Simulator {

    private HashMap<String, Color> colors;
    private String simulationName;
    private String tilePaintbrush;
    
    private Grid grid;

    private Stage actorStage;

    private final int TILE_GAP = 3;
    private final int TILE_LENGTH = 20;

    private void initColors() {
        
        colors = new HashMap<String, Color>();
        colors.put("floor", Color.WHEAT);
        colors.put("wall", Color.DARKSLATEGRAY);
    }

    private void initStage() {
        
        actorStage = new Stage();
        actorStage.setTitle(simulationName);

        Group mainGroup = new Group();
        BorderPane mainPane = new BorderPane();
        mainGroup.getChildren().addAll(mainPane);
        
        Node centerNode = initCenterNode();
        Node topNode = initTopNode();

        mainPane.setCenter(centerNode);
        mainPane.setTop(topNode);

        actorStage.setScene(new Scene(mainGroup));
                
    }

    private Node initCenterNode() {

        final int TILE_SPACE = TILE_GAP + TILE_LENGTH;

        Pane canvasOverlay = new Pane();
        Canvas canvas = new Canvas(grid.getWidth() * TILE_SPACE,
                                   grid.getHeight() * TILE_SPACE);

        for (int r = 0; r < grid.getHeight(); r++) {
            
            for (int c = 0; c < grid.getWidth(); c++) {

                final int row = r;
                final int col = c;
                
                Rectangle current = new Rectangle(TILE_LENGTH, TILE_LENGTH);
                
                current.setX(c * (TILE_LENGTH + TILE_GAP));
                current.setY(r * (TILE_LENGTH + TILE_GAP));

                current.setStroke(colors.get(grid.getAttributeAt(r, c)));
                current.setFill(colors.get(grid.getAttributeAt(r, c)).darker());
                
                current.setOnMousePressed(new EventHandler<MouseEvent>() {

                    @Override
                    public void handle(MouseEvent event) {

                        grid.setAttributeAt(row, col, tilePaintbrush);
                        
                        Color color = colors.get(grid.getAttributeAt(row, col));
                        current.setStroke(color);
                        current.setFill(color.darker());
                    }
                });

                canvasOverlay.getChildren().addAll(current);
                grid.setRectangleAt(r, c, current);
            }
        }

        canvasOverlay.getChildren().addAll(canvas);

        Group centerGroup = new Group();
        centerGroup.getChildren().addAll(canvasOverlay);

        return centerGroup;
    }

    private Node initTopNode() {

        HBox hbox = new HBox(6);
        hbox.setPadding(new Insets(12, 12, 12, 12));

        Button floorButton = new Button("Floor");
        Button wallButton = new Button("Wall");

        floorButton.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                tilePaintbrush = "floor";
            }
        });

        wallButton.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                tilePaintbrush = "wall";
            }
        });

        hbox.getChildren().addAll(floorButton, wallButton);
        return hbox;
    }

    public Simulator(int width, int height, String simulationName, String[][] attributes) {

        initColors();

        this.simulationName = simulationName;
        this.tilePaintbrush = "floor";

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

