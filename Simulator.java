import javafx.event.*;
import javafx.beans.value.*;

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

    private Slider stepSlider;

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

        StackPane rootPane = new StackPane();
        BorderPane mainPane = new BorderPane();
        rootPane.getChildren().addAll(mainPane);
        
        mainPane.setCenter(initCenterNode());
        mainPane.setTop(initTopNode());
        mainPane.setLeft(initLeftNode());
        mainPane.setBottom(initBottomNode());
        mainPane.setRight(initRightNode());

        BorderPane.setMargin(mainPane.getCenter(), 
                             new Insets(12, 12, 12, 12));

        actorStage.setScene(new Scene(rootPane));
    }

    // Initializers for center node of UI
    // ========================================================================
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

        Tooltip coordinateTooltip = new Tooltip();
        coordinateTooltip.setText("(" + row + ", " + col + ")");
        
        grid.setTooltipAt(row, col, coordinateTooltip);
        Tooltip.install(current, coordinateTooltip);
    }

    // Initializers for top node of UI
    // ========================================================================
    private Node initTopNode() {

        HBox hbox = new HBox(6);
        hbox.setPadding(new Insets(12, 12, 6, 12));

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

    // Initializers for left node of UI
    // ========================================================================
    private Node initLeftNode() {

        VBox vbox = new VBox(6);
        vbox.setPadding(new Insets(12, 6, 12, 12));

        Text colorTextHUD = new Text("Selection:");
        vbox.getChildren().addAll(colorTextHUD, currentColorDisplay);

        initSaveAsButton();
        initCheckContiguousButton();
        initGenerateSchedulesButton();

        vbox.getChildren().addAll(
            saveAsButton, 
            checkContiguousButton, 
            generateSchedulesButton
        );

        return vbox;
    }

    private void initSaveAsButton() {

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
                    "[Info] File " + file.getName() + " saved. " + (new Date()).toString()
                );
            }

        });

        saveAsButton.setMaxWidth(Double.MAX_VALUE);
    }

    private void initCheckContiguousButton() {
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

        checkContiguousButton.setMaxWidth(Double.MAX_VALUE);
    }

    private void initGenerateSchedulesButton() {

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

        generateSchedulesButton.setMaxWidth(Double.MAX_VALUE);
    }

    // Initializers for bottom node of UI
    // ========================================================================
    private Node initBottomNode() {

        VBox vbox = new VBox(6);

        TextArea outputTextArea = messageLog.getTextArea();
        Text consoleLabel = new Text("Message Log");
        vbox.getChildren().addAll(consoleLabel, outputTextArea);

        vbox.setPadding(new Insets(6, 12, 12, 12));
        return vbox;
    }

    // Initializers for right node of UI
    // ========================================================================
    private Node initRightNode() {
        
        VBox vbox = new VBox(6);
        vbox.setPadding(new Insets(12, 12, 12, 6));

        Text outputLabel = new Text("Schedules");

        scheduleBaseOutput = new ListView<String>();
        scheduleBaseOutput.setPrefWidth(300);

        initStepForwardButton();
        initStepBackwardButton();
        
        initNextPhaseButton();
        initPrevPhaseButton();

        initSimulateButton();
        initResetButton();

        GridPane simulationControls = initSimulationControls();

        initStepSlider();

        initRightStartingNodeBehavior();

        vbox.getChildren().addAll(
            outputLabel, 
            scheduleBaseOutput, 
            simulateButton,
            resetButton,
            simulationControls,
            stepSlider
        );

        return vbox;
    }

    private void initStepForwardButton() {

        stepForwardButton = new Button("Step Forward");
        stepForwardButton.setMaxWidth(Double.MAX_VALUE);
        stepForwardButton.setOnAction(new EventHandler<ActionEvent>() {
            
            @Override
            public void handle(ActionEvent event) {

                locationData.increaseStep();
                
                updateCanUseRightButtons();
                updateStepSliderBounds();

                drawSimulation();
                showUsageTooltips();
            }
        });
    }

    private void initStepBackwardButton() {
        
        stepBackwardButton = new Button("Step Backward");
        stepBackwardButton.setMaxWidth(Double.MAX_VALUE);
        stepBackwardButton.setOnAction(new EventHandler<ActionEvent>() {
            
            @Override
            public void handle(ActionEvent event) {
                
                locationData.decreaseStep();

                updateCanUseRightButtons();
                updateStepSliderBounds();

                drawSimulation();
                showUsageTooltips();
            }
        });
    }

    private void initNextPhaseButton() {

        nextPhaseButton = new Button("Next Phase");
        nextPhaseButton.setMaxWidth(Double.MAX_VALUE);
        nextPhaseButton.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                
                locationData.increasePeriod();

                updateCanUseRightButtons();
                updateStepSliderBounds();

                drawSimulation();
                showUsageTooltips();
            }
        });
    }

    private void initPrevPhaseButton() {

        prevPhaseButton = new Button("Previous Phase");
        prevPhaseButton.setMaxWidth(Double.MAX_VALUE);
        prevPhaseButton.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {

                locationData.decreasePeriod();

                updateCanUseRightButtons();
                updateStepSliderBounds();

                drawSimulation();
                showUsageTooltips();
            }
        });
    }

    private void initResetButton() {

        resetButton = new Button("Reset");
        resetButton.setMaxWidth(Double.MAX_VALUE);
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

                stepSlider.setDisable(true);
                stepSlider.setValue(0);

                stepSlider.setMin(0);
                stepSlider.setMax(1);

                enableAndRedrawTiles();
            }
        });
    }

    private void initSimulateButton() {

        simulateButton = new Button("Simulate");
        simulateButton.setMaxWidth(Double.MAX_VALUE);
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
                    actorSchedules, 
                    grid
                );

                updateCanUseRightButtons();
                updateStepSliderBounds();

                stepSlider.setDisable(false);

                drawSimulation();
                disableTiles();
                showUsageTooltips();
            }
        });
    }

    private GridPane initSimulationControls() {

        GridPane simulationControls = new GridPane();

        simulationControls.setHgap(6);
        simulationControls.setVgap(6);
        
        simulationControls.setMaxWidth(Double.MAX_VALUE);
        
        simulationControls.add(stepBackwardButton, 0, 0);
        simulationControls.add(stepForwardButton, 1, 0);

        simulationControls.add(prevPhaseButton, 0, 1);
        simulationControls.add(nextPhaseButton, 1, 1);

        return simulationControls;
    }

    private void initRightStartingNodeBehavior() {
        
        simulateButton.setDisable(true);
        resetButton.setDisable(true);

        stepBackwardButton.setDisable(true);
        stepForwardButton.setDisable(true);

        nextPhaseButton.setDisable(true);
        prevPhaseButton.setDisable(true);
        stepSlider.setDisable(true);
    }

    private void initStepSlider() {
        
        stepSlider = new Slider(0, 1, 1);
        stepSlider.setBlockIncrement(1);
        stepSlider.setMajorTickUnit(1);
        stepSlider.setMinorTickCount(1);
        stepSlider.setShowTickLabels(true);

        stepSlider.valueProperty().addListener(new ChangeListener<Number>() {

            @Override
            public void changed(
                ObservableValue<? extends Number> observable,
                Number oldValue,
                Number newValue) {

                stepSlider.setValue(Math.round(newValue.doubleValue()));
            }
        });

        stepSlider.valueProperty().addListener(new ChangeListener<Number>() {

            @Override
            public void changed(
                ObservableValue<? extends Number> observable,
                Number oldValue,
                Number newValue) {

                int oldStep = (int)Math.round(oldValue.doubleValue());
                int newStep = (int)Math.round(newValue.doubleValue());

                if (oldStep != newStep) {
                    
                    int step = newStep - 1;
                    locationData.setStep(step);

                    updateCanUseRightButtons();
                    drawSimulation();
                    showUsageTooltips();
                }
            }
        });
    }

    private void updateStepSliderBounds() {

        stepSlider.setMin(1);
        stepSlider.setMax(locationData.getMaxStep() + 1);
        stepSlider.setValue(locationData.getStep() + 1);
        stepSlider.setMajorTickUnit(locationData.getMaxStep());
    }

    private void updateCanUseRightButtons() {

        stepForwardButton.setDisable(!locationData.canIncreaseStep());
        stepBackwardButton.setDisable(!locationData.canDecreaseStep());

        nextPhaseButton.setDisable(!locationData.canIncreasePeriod());
        prevPhaseButton.setDisable(!locationData.canDecreasePeriod());
    }

    private boolean canSimulate() { 

        return recentlyVerified && recentlyGeneratedSchedules;
    }

    private void disableTiles() {

        for (int r = 0; r < grid.getHeight(); r++) {
            
            for (int c = 0; c < grid.getWidth(); c++) {
                
                Rectangle current = grid.getRectangleAt(r, c);
                
                current.setOnMousePressed(null);
                
                int period = locationData.getPeriod();
                int step = locationData.getStep();
                Tile tile = grid.getTileAt(r, c);

                Tooltip usageTooltip = grid.getTooltipAt(r, c);
                usageTooltip.setText("Usage: 0");
            }
        }
    }

    private void showUsageTooltips() {

        for (Tile validTile : grid.getValidTiles()) {
            
            int row = validTile.getRow();
            int col = validTile.getCol();
            
            int period = locationData.getPeriod();
            int step = locationData.getStep();

            Tooltip usageTooltip = grid.getTooltipAt(row, col);
            usageTooltip.setText("Usage: " + locationData.getUsageAt(period, step, validTile));
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

        messageLog.println("[Info] Started a simulation with the following dimensions:");
        messageLog.println("[Info] Width: " + grid.getWidth());
        messageLog.println("[Info] Height: " + grid.getHeight());
    }
}

