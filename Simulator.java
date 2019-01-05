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

import javafx.collections.ObservableList;

import java.util.*;
import java.io.*;

public class Simulator {

    private HashMap<String, Color> colors;

    private String simulationName;
    private String tilePaintbrush;

    private Rectangle currentColorDisplay;
    private TextArea consoleOutput;

    private MessageLog messageLog;
    private ListView<String> scheduleBaseOutput;
    
    private Grid grid;

    private Stage actorStage;

    private ScheduleGenerator scheduleGenerator;
    private LocationData locationData;
    
    private List<List<Tile>> actorSchedules;
    private List<List<List<Tile>>> actorPaths;

    private Button saveAsButton;
    private Button checkContiguousButton;
    private Button generateSchedulesButton;

    private Button simulateButton;
    private Button resetButton;

    private Button stepForwardButton;
    private Button stepBackwardButton;

    private Button nextPhaseButton;
    private Button prevPhaseButton;

    private boolean recentlyVerified = false;
    private boolean recentlyGeneratedSchedules = false;

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
        mainPane.setRight(initRightNode());

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
                
                initTileEventHandler(row, col, current);

                canvasOverlay.getChildren().addAll(current);
                grid.setRectangleAt(r, c, current);
            }
        }

        Group centerGroup = new Group();
        centerGroup.getChildren().addAll(canvasOverlay);

        return centerGroup;
    }

    private void initTileEventHandler(int row, int col, Rectangle current) {

        current.setOnMousePressed(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {
                
                recentlyVerified = false;
                recentlyGeneratedSchedules = false;
                
                simulateButton.setDisable(true);

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

        saveAsButton = new Button("Save As...");
        saveAsButton.setOnAction(new EventHandler<ActionEvent>() {

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

        checkContiguousButton = new Button("Verify Grid");
        checkContiguousButton.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                
                messageLog.println(grid.isContiguous() 
                    ? "[Verified] Grid is one contiguous body."
                    : "[Warning] Grid is not contiguous. Unreachable tiles exist."
                );

                recentlyVerified = grid.isContiguous();
                simulateButton.setDisable(!canSimulate());
            }
        });

        generateSchedulesButton = new Button("Generate Schedules");
        generateSchedulesButton.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                
                TextInputDialog dialog = new TextInputDialog();
                
                dialog.setTitle("Generate Schedules...");
                dialog.setHeaderText("How many actor schedules would you like?");
                dialog.setContentText("Enter a positive integer:");
                
                Optional<String> response = dialog.showAndWait();
                
                if (!response.isPresent()) {
                    
                    messageLog.println(
                        "[Error] No input detected for " 
                      + "number of schedules. No schedules were generated."
                    );
                }
                else {

                    try {

                        int N = Integer.parseInt(response.get());

                        if (N <= 0) {
                            throw new IllegalArgumentException();
                        }
                        
                        scheduleGenerator = new ScheduleGenerator(grid);
                        actorSchedules = scheduleGenerator.generateSchedules(N);

                        ObservableList<String> list = scheduleBaseOutput.getItems();
                        list.setAll(ScheduleGenerator.getStringList(actorSchedules));

                        recentlyGeneratedSchedules = true;
                        simulateButton.setDisable(!canSimulate());
                    }
                    catch(IllegalArgumentException ex) {

                        messageLog.println(
                            "[Error] Invalid input detected for "
                          + "number of schedules. No schedules were generated."
                        );
                    }
                }
            }
        });

        saveAsButton.setMaxWidth(Double.MAX_VALUE);
        checkContiguousButton.setMaxWidth(Double.MAX_VALUE);
        generateSchedulesButton.setMaxWidth(Double.MAX_VALUE);

        vbox.getChildren().addAll(
            saveAsButton, 
            checkContiguousButton, 
            generateSchedulesButton
        );

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

    public Node initRightNode() {
        
        VBox vbox = new VBox(6);
        vbox.setPadding(new Insets(12, 12, 12, 12));

        Text outputLabel = new Text("Schedules");

        scheduleBaseOutput = new ListView<String>();
        scheduleBaseOutput.setPrefWidth(300);

        stepForwardButton = new Button("Step Forward");
        stepForwardButton.setOnAction(new EventHandler<ActionEvent>() {
            
            @Override
            public void handle(ActionEvent event) {

                locationData.increaseStep();
                
                stepForwardButton.setDisable(!locationData.canIncreaseStep());
                stepBackwardButton.setDisable(!locationData.canDecreaseStep());

                drawSimulation();
            }
        });

        stepBackwardButton = new Button("Step Backward");
        stepBackwardButton.setOnAction(new EventHandler<ActionEvent>() {
            
            @Override
            public void handle(ActionEvent event) {
                
                locationData.decreaseStep();

                stepForwardButton.setDisable(!locationData.canIncreaseStep());
                stepBackwardButton.setDisable(!locationData.canDecreaseStep());

                drawSimulation();
            }
        });
        
        nextPhaseButton = new Button("Next Phase");
        nextPhaseButton.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                
                locationData.increasePeriod();

                nextPhaseButton.setDisable(!locationData.canIncreasePeriod());
                prevPhaseButton.setDisable(!locationData.canDecreasePeriod());

                stepForwardButton.setDisable(!locationData.canIncreaseStep());
                stepBackwardButton.setDisable(!locationData.canDecreaseStep());

                drawSimulation();
            }
        });

        prevPhaseButton = new Button("Previous Phase");
        prevPhaseButton.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {

                locationData.decreasePeriod();

                nextPhaseButton.setDisable(!locationData.canIncreasePeriod());
                prevPhaseButton.setDisable(!locationData.canDecreasePeriod());

                stepForwardButton.setDisable(!locationData.canIncreaseStep());
                stepBackwardButton.setDisable(!locationData.canDecreaseStep());

                drawSimulation();
            }
        });

        GridPane simulationControls = new GridPane();

        simulationControls.setHgap(6);
        simulationControls.setVgap(6);
        
        simulationControls.setMaxWidth(Double.MAX_VALUE);
        
        simulationControls.add(stepBackwardButton, 0, 0);
        simulationControls.add(stepForwardButton, 1, 0);

        simulationControls.add(prevPhaseButton, 0, 1);
        simulationControls.add(nextPhaseButton, 1, 1);

        Button resetButton = new Button("Reset");
        resetButton.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                
                saveAsButton.setDisable(false);
                checkContiguousButton.setDisable(false);
                generateSchedulesButton.setDisable(false);

                simulateButton.setDisable(true);
                resetButton.setDisable(true);

                stepForwardButton.setDisable(true);
                stepBackwardButton.setDisable(true);

                prevPhaseButton.setDisable(true);
                nextPhaseButton.setDisable(true);

                enableAndRedrawTiles();
            }
        });

        simulateButton = new Button("Simulate");
        simulateButton.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {

                messageLog.println("[Info] Simulation generated!");

                simulateButton.setDisable(true);
                resetButton.setDisable(false);

                saveAsButton.setDisable(true);
                checkContiguousButton.setDisable(true);
                generateSchedulesButton.setDisable(true);

                locationData = new LocationData(
                    scheduleGenerator, 
                    actorSchedules, grid
                );

                stepForwardButton.setDisable(!locationData.canIncreaseStep());
                stepBackwardButton.setDisable(!locationData.canDecreaseStep());

                nextPhaseButton.setDisable(!locationData.canIncreasePeriod());
                prevPhaseButton.setDisable(!locationData.canDecreasePeriod());

                disableTiles();
                drawSimulation();
            }
        });

        simulateButton.setMaxWidth(Double.MAX_VALUE);
        resetButton.setMaxWidth(Double.MAX_VALUE);

        stepBackwardButton.setMaxWidth(Double.MAX_VALUE);
        stepForwardButton.setMaxWidth(Double.MAX_VALUE);

        nextPhaseButton.setMaxWidth(Double.MAX_VALUE);
        prevPhaseButton.setMaxWidth(Double.MAX_VALUE);

        simulateButton.setDisable(true);
        resetButton.setDisable(true);

        stepBackwardButton.setDisable(true);
        stepForwardButton.setDisable(true);

        nextPhaseButton.setDisable(true);
        prevPhaseButton.setDisable(true);

        vbox.getChildren().addAll(
            outputLabel, 
            scheduleBaseOutput, 
            simulateButton,
            resetButton,
            simulationControls
        );

        return vbox;
    }

    private boolean canSimulate() {
        return recentlyVerified && recentlyGeneratedSchedules;
    }

    private void disableTiles() {
        for (int r = 0; r < grid.getHeight(); r++) {
            for (int c = 0; c < grid.getWidth(); c++) {
                grid.getRectangleAt(r, c).setOnMousePressed(null);
            }
        }
    }

    private void enableAndRedrawTiles() {
        
        for (int r = 0; r < grid.getHeight(); r++) {
            for (int c = 0; c < grid.getWidth(); c++) {

                Rectangle rectangle = grid.getRectangleAt(r, c);
                initTileEventHandler(r, c, grid.getRectangleAt(r, c));

                String attribute = grid.getAttributeAt(r, c);
                Color refill = colors.get(attribute);

                rectangle.setFill(refill.darker());
            }
        }
    }

    private void drawSimulation() {
        
        int period = locationData.getPeriod();
        int step = locationData.getStep();

        for (Tile validTile : grid.getValidTiles()) {
            
            Color update = locationData.getIntensityAt(period, step, validTile);
            
            int row = validTile.getRow();
            int col = validTile.getCol();

            Rectangle rectangle = grid.getRectangleAt(row, col);
            rectangle.setFill(update);
        }
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

