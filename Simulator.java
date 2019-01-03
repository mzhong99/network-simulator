import javafx.event.*;
import javafx.stage.*;
import javafx.scene.*;

import javafx.scene.control.*;
import javafx.scene.canvas.*;
import javafx.scene.layout.*;
import javafx.scene.shape.*;
import javafx.scene.input.*;
import javafx.scene.text.*;

import javafx.scene.paint.Color;
import javafx.geometry.Insets;

import java.util.*;
import java.io.*;

public class Simulator {

    private HashMap<String, Color> colors;

    private String simulationName;
    private String tilePaintbrush;

    private Rectangle currentColorDisplay;
    private TextArea consoleOutput;

    private MessageLog messageLog;
    
    private Grid grid;

    private Stage actorStage;

    private final int TILE_GAP = 3;
    private final int TILE_LENGTH = 20;
    private final int TILE_SPACE = TILE_GAP + TILE_LENGTH;

    private void initColors() {
        
        colors = new HashMap<String, Color>();
        colors.put("floor", Color.WHEAT);
        colors.put("wall", Color.GRAY);
        colors.put("room_red", Color.CORAL);
        colors.put("room_blue", Color.STEELBLUE);
        colors.put("room_green", Color.TEAL);
        colors.put("room_orange", Color.SANDYBROWN);
    }

    private void initStage() {
        
        actorStage = new Stage();
        actorStage.setTitle(simulationName);

        Group mainGroup = new Group();
        BorderPane mainPane = new BorderPane();
        mainGroup.getChildren().addAll(mainPane);
        
        mainPane.setCenter(initCenterNode());
        mainPane.setTop(initTopNode());
        mainPane.setLeft(initLeftNode());
        mainPane.setBottom(initBottomNode());

        BorderPane.setMargin(mainPane.getCenter(), 
                             new Insets(12, 12, 12, 12));

        actorStage.setScene(new Scene(mainGroup));
                
    }

    private Node initCenterNode() {

        Pane canvasOverlay = new Pane();

        for (int r = 0; r < grid.getHeight(); r++) {
            
            for (int c = 0; c < grid.getWidth(); c++) {

                final int row = r;
                final int col = c;
                
                Rectangle current = new Rectangle(TILE_LENGTH, TILE_LENGTH);
                
                current.setX(c * (TILE_SPACE));
                current.setY(r * (TILE_SPACE));

                current.setStroke(colors.get(grid.getAttributeAt(r, c)));
                current.setFill(colors.get(grid.getAttributeAt(r, c)).darker());
                
                current.setOnMousePressed(new EventHandler<MouseEvent>() {

                    @Override
                    public void handle(MouseEvent event) {
                        
                        String oldAttribute = grid.getAttributeAt(row, col);
                        String newAttribute = tilePaintbrush;
                        
                        boolean oldIsNew = oldAttribute.equals(newAttribute);

                        this.setTile(oldIsNew ? "wall" : tilePaintbrush);
                    }

                    public void setTile(String brush) {
                        grid.setAttributeAt(row, col, brush);
                        Color color = colors.get(grid.getAttributeAt(row, col));
                        current.setStroke(color);
                        current.setFill(color.darker());
                    }

                });

                canvasOverlay.getChildren().addAll(current);
                grid.setRectangleAt(r, c, current);
            }
        }

        Group centerGroup = new Group();
        centerGroup.getChildren().addAll(canvasOverlay);

        return centerGroup;
    }

    private Node initTopNode() {

        HBox hbox = new HBox(6);
        hbox.setPadding(new Insets(12, 12, 12, 12));

        Button[] buttons = {
            new Button("Floor"),
            new Button("Red Room"),
            new Button("Blue Room"),
            new Button("Green Room"),
            new Button("Orange Room"),
        };

        String[] tilesInOrder = {
            "floor",
            "room_red",
            "room_blue",
            "room_green",
            "room_orange"
        };

        for (int i = 0; i < buttons.length; i++) {
            
            String updatedPaintbrush = tilesInOrder[i];
            buttons[i].setOnAction(new EventHandler<ActionEvent>() {

                @Override
                public void handle(ActionEvent event) {
                    tilePaintbrush = updatedPaintbrush;
                    currentColorDisplay.setStroke(colors.get(tilePaintbrush));
                    currentColorDisplay.setFill(colors.get(tilePaintbrush).darker());
                }
            });

            hbox.getChildren().addAll(buttons[i]);
        }

        return hbox;
    }

    private Node initLeftNode() {

        VBox vbox = new VBox(6);
        vbox.setPadding(new Insets(12, 12, 12, 12));

        Text colorTextHUD = new Text("Selection:");
        vbox.getChildren().addAll(colorTextHUD, currentColorDisplay);

        Button saveAs = new Button("Save As...");
        saveAs.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                
                FileChooser fileSaver = new FileChooser();
                fileSaver.setTitle("Save Network File As...");

                Stage tempStage = new Stage();
                tempStage.show();
                File file = fileSaver.showSaveDialog(tempStage);
                
                if (file != null) {
                    try {
                        printToFile(file);
                    }
                    catch (FileNotFoundException ex) {
                        System.out.println(ex.toString());
                    }
                }
                tempStage.hide();
            }

            public void printToFile(File file) throws FileNotFoundException {
                
                PrintWriter writer = new PrintWriter(file);
                
                writer.println("ntwk_file");
                writer.println(simulationName);
                writer.println(grid.getWidth() + " " + grid.getHeight());
                
                for (int r = 0; r < grid.getHeight(); r++) {
                    for (int c = 0; c < grid.getWidth(); c++) {
                        writer.printf(grid.getAttributeAt(r, c) + " ");
                    }
                    writer.println();
                }

                writer.flush();
                writer.close();
                messageLog.println(
                    "File " 
                  + file.getName() 
                  + " saved. " 
                  + (new Date()).toString()
                );
            }

        });

        Button checkContiguousButton = new Button("Verify Grid");
        checkContiguousButton.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                messageLog.println(grid.isContiguous() 
                    ? "[Verified] Grid is one contiguous body."
                    : "[Warning] Grid is not contiguous. Unreachable tiles exist."
                );
            }
        });

        vbox.getChildren().addAll(saveAs, checkContiguousButton);
        return vbox;
    }

    public Node initBottomNode() {

        VBox vbox = new VBox(6);

        TextArea outputTextArea = messageLog.getTextArea();
        Text consoleLabel = new Text("Message Log");
        vbox.getChildren().addAll(consoleLabel, outputTextArea);

        vbox.setPadding(new Insets(12, 12, 12, 12));
        return vbox;
    }

    public Simulator(int width, int height, String simulationName, String[][] attributes) {

        initColors();

        this.simulationName = simulationName;
        this.tilePaintbrush = "floor";

        this.currentColorDisplay = new Rectangle(TILE_SPACE, TILE_SPACE);
        this.currentColorDisplay.setStroke(colors.get("floor"));
        this.currentColorDisplay.setFill(colors.get("floor").darker());

        this.grid = new Grid(width, height);

        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                grid.setAttributeAt(r, c, attributes[r][c]);
            }
        }

        this.messageLog = new MessageLog(6);

        initStage();
    }

    public void show() {
        actorStage.show();
    }
}

