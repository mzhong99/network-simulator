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

import java.lang.Thread;
import javafx.concurrent.Task;

import javafx.collections.ObservableList;

import java.util.*;
import java.io.*;

public class Simulator {

    private class CenterBase {

        private BorderPane centerRoot;

        public CenterBase() {

            centerRoot = new BorderPane();
            centerRoot.setCenter(initCanvas());
            centerRoot.setBottom(initZoomer());
        }

        private Node initCanvas() {

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

            StackPane wrapper = new StackPane();
            wrapper.getChildren().addAll(canvasOverlay);

            ScrollPane outerPane = new ScrollPane();
            outerPane.setContent(wrapper);

            outerPane.setHvalue(0.5);
            outerPane.setVvalue(0.5);

            return outerPane;
        }

        private Node initZoomer() {

            HBox hbox = new HBox(6);
            hbox.setPadding(new Insets(6, 6, 6, 6));

            Label zoomAmountHUD = new Label("Zoom: ");
            Slider zoomSlider = new Slider(5, 100, 1);

            zoomSlider.setBlockIncrement(1);
            zoomSlider.setMinorTickCount(1);
            zoomSlider.setMajorTickUnit(96);
            zoomSlider.setValue(20);
            zoomSlider.setShowTickLabels(true);

            zoomSlider.valueProperty().addListener(new ChangeListener<Number>() {

                @Override
                public void changed(
                    ObservableValue<? extends Number> observable,
                    Number oldValue,
                    Number newValue) {

                    zoomSlider.setValue(Math.round(newValue.doubleValue()));
                }
            });

            zoomSlider.valueProperty().addListener(new ChangeListener<Number>() {

                @Override
                public void changed(
                    ObservableValue<? extends Number> observable,
                    Number oldValue,
                    Number newValue) {

                    int oldZoom = (int)Math.round(oldValue.doubleValue());
                    int newZoom = (int)Math.round(newValue.doubleValue());

                    if (oldZoom != newZoom) {

                        TILE_LENGTH = newZoom;
                        TILE_GAP = 1 + (newZoom / 34);
                        TILE_SPACE = TILE_LENGTH + TILE_GAP;

                        updateZoom();
                    }
                }
            });

            Button zoomButton = new Button("Reset Zoom");
            zoomButton.setOnAction(new EventHandler<ActionEvent>() {

                @Override
                public void handle(ActionEvent event) {
                    zoomSlider.setValue(20);
                    updateZoom();
                }
            });

            hbox.getChildren().addAll(zoomAmountHUD, zoomSlider, zoomButton);
            return hbox;
        }

        private void initTileEventHandler(int row, int col, Rectangle current) {

            current.setOnMousePressed(new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent event) {

                    if (event.isPrimaryButtonDown()) {
                    
                        recentlyVerified = false;
                        recentlyGeneratedSchedules = false;
                        
                        rightBase.simulateButton.setDisable(true);

                        String oldAttribute = grid.getAttributeAt(row, col);
                        String newAttribute = tilePaintbrush;
                        
                        boolean oldIsNew = oldAttribute.equals(newAttribute);

                        this.setTile(oldIsNew ? "wall" : tilePaintbrush);
                    }
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

        public void enableAndRedrawTiles() {
            
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
    }

    private class LeftBase {

        private VBox leftRoot;

        private Rectangle currentColorDisplay;

        private Button saveAsButton;
        private Button checkContiguousButton;
        private Button generateSchedulesButton;
        private Button setTimeParametersButton;
        private Button reshapeButton;

        public LeftBase() {
            
            currentColorDisplay = new Rectangle(TILE_SPACE, TILE_SPACE);
            currentColorDisplay.setStroke(colors.get("floor"));
            currentColorDisplay.setFill(colors.get("floor").darker());

            leftRoot = new VBox(6);
            leftRoot.setPadding(new Insets(12, 6, 12, 12));

            Text colorTextHUD = new Text("Selection:");
            leftRoot.getChildren().addAll(colorTextHUD, currentColorDisplay);

            initSaveAsButton();
            initCheckContiguousButton();
            initGenerateSchedulesButton();
            initSetTimeParametersButton();
            initReshapeButton();

            leftRoot.getChildren().addAll(
                saveAsButton, 
                checkContiguousButton, 
                generateSchedulesButton,
                setTimeParametersButton,
                reshapeButton
            );
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
                    rightBase.simulateButton.setDisable(!canSimulate());
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
                            rightBase.simulateButton.setDisable(!canSimulate());
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

        private void initSetTimeParametersButton() {

            setTimeParametersButton = new Button("Set Time Parameters...");
            setTimeParametersButton.setOnAction(new EventHandler<ActionEvent>() {

                @Override
                public void handle(ActionEvent event) {
                    
                    Stage promptStage = new Stage();
                    promptStage.setResizable(false);
                    promptStage.setTitle("Set Time Parameters...");
                    
                    TextField distanceUnitField = new TextField();
                    distanceUnitField.setText(DISTANCE_UNIT);
                    distanceUnitField.setPromptText("Enter distance unit");
                    distanceUnitField.setMaxWidth(Double.MAX_VALUE);
                    distanceUnitField.setTooltip(
                        new Tooltip(
                            "The distance unit length of one tile in this simulation."
                          + "\nEx: [Feet, Meters]"
                        )
                    );


                    TextField timeUnitField = new TextField();
                    timeUnitField.setText(TIME_UNIT);
                    timeUnitField.setPromptText("Enter time unit");
                    timeUnitField.setMaxWidth(Double.MAX_VALUE);
                    timeUnitField.setTooltip(
                        new Tooltip("The time unit elapsed when the simulation is incremented one step."
                                  + "\nEx: [Seconds, Milliseconds]"
                        )
                    );

                    TextField actorFrequencyField = new TextField();
                    actorFrequencyField.setText(
                        ACTOR_TRAVERSAL_FREQUENCY > 0 
                            ? String.valueOf(ACTOR_TRAVERSAL_FREQUENCY)
                            : ""
                    );
                    actorFrequencyField.setPromptText("Enter movement speed");
                    actorFrequencyField.setMaxWidth(Double.MAX_VALUE);
                    actorFrequencyField.setTooltip(
                        new Tooltip(
                            "The amount of time (measured in the [time unit]) it takes for one actor to pass a tile."
                        )
                    );

                    TextField tileScaleField = new TextField();
                    tileScaleField.setText(
                        TILE_SCALE > 0
                            ? String.valueOf(TILE_SCALE)
                            : ""
                    );
                    tileScaleField.setPromptText("Enter tile scale");
                    tileScaleField.setMaxWidth(Double.MAX_VALUE);
                    tileScaleField.setTooltip(
                        new Tooltip(
                            "How long a tile is (measured in thr [distance unit])"
                        )
                    );


                    Button updateButton = new Button("Update all parementers and return");
                    updateButton.setOnAction(new EventHandler<ActionEvent>() {

                        @Override
                        public void handle(ActionEvent event) {
                            
                            try {
                                
                                String tempDistanceText = distanceUnitField.getText();
                                String tempTimeUnitText = timeUnitField.getText();
                                
                                String actorFrequencyText = actorFrequencyField.getText();
                                String tileScaleText = tileScaleField.getText();

                                if ("".equals(tempDistanceText) || "".equals(tempTimeUnitText)) {
                                    throw new Exception("Distance and time units cannot be empty.");
                                }

                                int tempActorFrequency = Integer.parseInt(actorFrequencyText);
                                int tempTileScale = Integer.parseInt(tileScaleText);

                                if (tempActorFrequency <= 0 || tempTileScale <= 0) {
                                    throw new Exception("Speed and Tile Scale must be positive integers.");
                                }

                                ACTOR_TRAVERSAL_FREQUENCY = tempActorFrequency;
                                TILE_SCALE = tempTileScale;
                                
                                DISTANCE_UNIT = tempDistanceText;
                                TIME_UNIT = tempTimeUnitText;

                                rightBase.simulateButton.setDisable(!canSimulate());

                                messageLog.println("[Info] Time parameters set. Values:");
                                messageLog.println("[Info] Distance Unit: " + DISTANCE_UNIT);
                                messageLog.println("[Info] Time Unit: " + TIME_UNIT);
                                
                                messageLog.println(
                                    "[Info] Actor Frequency: " 
                                    + ACTOR_TRAVERSAL_FREQUENCY + " " + TIME_UNIT 
                                    + " per " + TILE_SCALE + " " + DISTANCE_UNIT
                                );
                                
                                messageLog.println("[Info] Tile Scale: " + TILE_SCALE + " " + DISTANCE_UNIT);
                            }
                            catch (Exception ex) {
                                messageLog.println(
                                    "[Error] Invalid time parameters were set. Parameters not updated."
                                );
                            }

                            promptStage.hide();
                        }
                    });
                    updateButton.setMaxWidth(Double.MAX_VALUE);

                    VBox rootPane = new VBox(6);
                    GridPane tempGridPane = new GridPane();

                    tempGridPane.setHgap(6);
                    tempGridPane.setVgap(6);

                    tempGridPane.add(distanceUnitField, 0, 0);
                    tempGridPane.add(timeUnitField, 0, 1);
                    tempGridPane.add(tileScaleField, 1, 0);
                    tempGridPane.add(actorFrequencyField, 1, 1);

                    rootPane.getChildren().addAll(
                        new Text("Enter parameters for actor movement scaling:"),
                        tempGridPane,
                        updateButton
                    );

                    rootPane.setPadding(new Insets(12, 12, 12, 12));

                    StackPane wrapper = new StackPane();
                    wrapper.getChildren().addAll(rootPane);
                    promptStage.setScene(new Scene(wrapper));

                    promptStage.showAndWait();
                }
            });

            setTimeParametersButton.setMaxWidth(Double.MAX_VALUE);
        }

        private void initReshapeButton() {
            reshapeButton = new Button("Reshape Grid...");
            reshapeButton.setMaxWidth(Double.MAX_VALUE);
            reshapeButton.setOnAction(new EventHandler<ActionEvent>() {

                @Override
                public void handle(ActionEvent event) {
                    Stage promptStage = new Stage();
                    promptStage.setResizable(false);
                    promptStage.setTitle("Resize grid");

                    Text promptText = new Text("Update the size of the grid used in this simulation.");

                    TextField relativeStartingXField = new TextField();
                    relativeStartingXField.setTooltip(
                        new Tooltip("Enter the left-most position the new grid will receive.")
                    );
                    relativeStartingXField.setText("0");

                    TextField relativeStartingYField = new TextField();
                    relativeStartingYField.setTooltip(
                        new Tooltip("Enter the top-most position the new grid will receive.")
                    );
                    relativeStartingYField.setText("0");

                    TextField updatedWidthField = new TextField();
                    updatedWidthField.setTooltip(new Tooltip("Enter a positive integer as the new width."));
                    updatedWidthField.setText(String.valueOf(grid.getWidth()));

                    TextField updatedHeightField = new TextField();
                    updatedHeightField.setTooltip(new Tooltip("Enter a positive integer as the new height."));
                    updatedHeightField.setText(String.valueOf(grid.getHeight()));

                    GridPane gridPane = new GridPane();
                    gridPane.setHgap(6);
                    gridPane.setVgap(6);

                    gridPane.add(new Text("Relative Starting X Position"), 0, 0);
                    gridPane.add(new Text("Relative Starting Y Position"), 0, 1);
                    gridPane.add(new Text("Updated Width"), 0, 2);
                    gridPane.add(new Text("Updated Height"), 0, 3);

                    gridPane.add(relativeStartingXField, 1, 0);
                    gridPane.add(relativeStartingYField, 1, 1);
                    gridPane.add(updatedWidthField, 1, 2);
                    gridPane.add(updatedHeightField, 1, 3);

                    Button updateButton = new Button("Update grid size and return");
                    updateButton.setOnAction(new EventHandler<ActionEvent>() {

                        @Override
                        public void handle(ActionEvent event) {
                            try {
                                int newStartingX = Integer.parseInt(relativeStartingXField.getText());
                                int newStartingY = Integer.parseInt(relativeStartingYField.getText());

                                int newWidth = Integer.parseInt(updatedWidthField.getText());
                                int newHeight = Integer.parseInt(updatedHeightField.getText());

                                if (newWidth <= 0 || newHeight <= 0) {
                                    throw new IllegalArgumentException("Paramaeter out of bounds");
                                }

                                Grid updatedGrid = new Grid(newWidth, newHeight);
                                for (int newX = 0, oldX = newStartingX; newX < newWidth; newX++, oldX++) {
                                    for (int newY = 0, oldY = newStartingY; newY < newHeight; newY++, oldY++) {
                                        if (oldX < 0 || oldY < 0) {
                                            continue;
                                        }
                                        else {
                                            updatedGrid.setAttributeAt(newX, newY, grid.getAttributeAt(oldX, oldY));
                                        }
                                    }
                                }

                                grid = updatedGrid;
                                centerBase = new CenterBase();
                                mainPane.setCenter(centerBase.centerRoot);

                                messageLog.println("[Info] Grid reshaping to the following dimensions:");
                                messageLog.println("[Info] Relative starting X position: " + newStartingX);
                                messageLog.println("[Info] Relative starting Y position: " + newStartingY);
                                messageLog.println("[Info] Updated Width: " + newWidth);
                                messageLog.println("[Info] Updated Height: " + newHeight);
                            }
                            catch (NumberFormatException exception) {
                                messageLog.println("[Error] Grid reshaping blocked due to bad parameters");
                            }
                            catch (IllegalArgumentException exception) {
                                messageLog.println("[Error] Grid reshaping blocked.");
                                messageLog.println("[Error] Relative X and Y positions must be zero or greater");
                                messageLog.println("[Error] Width and Height must be greater than zero");
                            }
                            promptStage.close();
                        }
                    });

                    updateButton.setMaxWidth(Double.MAX_VALUE);

                    VBox promptVBox = new VBox(6);
                    promptVBox.getChildren().addAll(promptText, gridPane, updateButton);
                    StackPane stackPane = new StackPane();
                    stackPane.getChildren().addAll(promptVBox);
                    stackPane.setPadding(new Insets(12, 12, 12, 12));

                    promptStage.setScene(new Scene(stackPane));
                    promptStage.showAndWait();
                }
            });
        }
    }

    private class TopBase {

        private HBox topRoot;

        public TopBase() {

            topRoot = new HBox(6);
            topRoot.setPadding(new Insets(12, 12, 6, 12));

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
                        leftBase.currentColorDisplay.setStroke(colors.get(tilePaintbrush));
                        leftBase.currentColorDisplay.setFill(colors.get(tilePaintbrush).darker());
                    }
                });

                topRoot.getChildren().addAll(buttons[i]);
            }
        }
    }

    private class RightBase {

        private VBox rightRoot;

        private Button simulateButton;
        private Button resetButton;

        private Button stepForwardButton;
        private Button stepBackwardButton;

        private Button nextPhaseButton;
        private Button prevPhaseButton;

        private GridPane simulationControls;

        private Slider stepSlider;
        
        private Label timeLabel;
        private Label outputLabel;

        public RightBase() {

            rightRoot = new VBox(6);
            rightRoot.setPadding(new Insets(12, 12, 12, 6));

            outputLabel = new Label("Schedules");
            timeLabel = new Label("Time: ");

            scheduleBaseOutput = new ListView<String>();
            scheduleBaseOutput.setPrefWidth(300);

            initStepForwardButton();
            initStepBackwardButton();
            
            initNextPhaseButton();
            initPrevPhaseButton();

            initSimulateButton();
            initResetButton();

            initSimulationControls();

            initStepSlider();

            initStartingBehavior();

            rightRoot.getChildren().addAll(
                outputLabel, 
                scheduleBaseOutput, 
                simulateButton,
                resetButton,
                simulationControls,
                timeLabel,
                stepSlider
            );
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
                    
                    leftBase.saveAsButton.setDisable(false);
                    leftBase.checkContiguousButton.setDisable(false);
                    leftBase.generateSchedulesButton.setDisable(false);
                    leftBase.setTimeParametersButton.setDisable(false);
                    leftBase.reshapeButton.setDisable(false);

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

                    centerBase.enableAndRedrawTiles();
                }
            });
        }

        private void initSimulateButton() {

            simulateButton = new Button("Simulate");
            simulateButton.setMaxWidth(Double.MAX_VALUE);
            simulateButton.setOnAction(new EventHandler<ActionEvent>() {

                @Override
                public void handle(ActionEvent event) {

                    Task<Boolean> task = new Task<Boolean>() {

                        @Override
                        protected Boolean call() {
                            
                            actorStage.getScene().setCursor(Cursor.WAIT);
                            runSimulation();
                            actorStage.getScene().setCursor(Cursor.DEFAULT);

                            return true;
                        }
                    };

                    Thread thread = new Thread(task);
                    thread.setDaemon(true);
                    thread.start();
                }

                public void runSimulation() {

                    simulateButton.setDisable(true);

                    leftBase.saveAsButton.setDisable(true);
                    leftBase.checkContiguousButton.setDisable(true);
                    leftBase.generateSchedulesButton.setDisable(true);
                    leftBase.setTimeParametersButton.setDisable(true);
                    leftBase.reshapeButton.setDisable(true);

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

                    messageLog.println("[Info] Simulation generated!");

                    resetButton.setDisable(false);
                }
            });
        }

        private void initSimulationControls() {

            simulationControls = new GridPane();

            simulationControls.setHgap(6);
            simulationControls.setVgap(6);
            
            simulationControls.setMaxWidth(Double.MAX_VALUE);
            
            simulationControls.add(stepBackwardButton, 0, 0);
            simulationControls.add(stepForwardButton, 1, 0);

            simulationControls.add(prevPhaseButton, 0, 1);
            simulationControls.add(nextPhaseButton, 1, 1);
        }

        private void initStartingBehavior() {
            
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
            stepSlider.setMajorTickUnit(1);
            stepSlider.setMinorTickCount(1);
            stepSlider.setShowTickLabels(true);

            stepSlider.valueProperty().addListener(new ChangeListener<Number>() {

                @Override
                public void changed(
                    ObservableValue<? extends Number> observable,
                    Number oldValue,
                    Number newValue) {

                    int newValueRaw = (int)Math.round(newValue.doubleValue());
                    int newValueDiv = newValueRaw - (newValueRaw % ACTOR_TRAVERSAL_FREQUENCY);

                    stepSlider.setValue(newValueDiv);
                }
            });

            stepSlider.valueProperty().addListener(new ChangeListener<Number>() {

                @Override
                public void changed(
                    ObservableValue<? extends Number> observable,
                    Number oldValue,
                    Number newValue) {

                    int oldStepRaw = (int)Math.round(oldValue.doubleValue());
                    int oldStepDiv = oldStepRaw - (oldStepRaw % ACTOR_TRAVERSAL_FREQUENCY);

                    int newStepRaw = (int)Math.round(newValue.doubleValue());
                    int newStepDiv = newStepRaw - (newStepRaw % ACTOR_TRAVERSAL_FREQUENCY);

                    int oldStep = oldStepDiv / ACTOR_TRAVERSAL_FREQUENCY;
                    int newStep = newStepDiv / ACTOR_TRAVERSAL_FREQUENCY;

                    if (oldStep != newStep) {
                        
                        int step = newStep;
                        locationData.setStep(step);

                        timeLabel.setText("Time: " + newStepDiv + " " + TIME_UNIT);
                        updateCanUseRightButtons();
                        drawSimulation();
                        showUsageTooltips();
                    }
                }
            });
        }
    }

    private class BottomBase {

        private VBox bottomRoot;

        public BottomBase() {

            bottomRoot = new VBox(6);

            TextArea outputTextArea = messageLog.getTextArea();
            Text consoleLabel = new Text("Message Log");
            bottomRoot.getChildren().addAll(consoleLabel, outputTextArea);

            bottomRoot.setPadding(new Insets(6, 12, 12, 12));
        }
    }

    private CenterBase centerBase;
    private LeftBase   leftBase;
    private TopBase    topBase;
    private BottomBase bottomBase;
    private RightBase  rightBase;

    private HashMap<String, Color> colors;

    private String simulationName, tilePaintbrush;

    private MessageLog messageLog;
    private ListView<String> scheduleBaseOutput;
    
    private Grid grid;

    private Stage actorStage;
    private BorderPane mainPane;

    private ScheduleGenerator scheduleGenerator;
    private LocationData locationData;
    
    private List<List<Tile>> actorSchedules;
    private List<List<List<Tile>>> actorPaths;

    private boolean recentlyVerified = false;
    private boolean recentlyGeneratedSchedules = false;

    private int TILE_GAP = 1;
    private int TILE_LENGTH = 20;
    private int TILE_SPACE = TILE_GAP + TILE_LENGTH;

    private String DISTANCE_UNIT = "";
    private String TIME_UNIT = "";

    private int ACTOR_TRAVERSAL_FREQUENCY = -1;
    private int TILE_SCALE = -1;

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
        
        initNodes();

        actorStage = new Stage();
        actorStage.setTitle(simulationName);

        StackPane rootPane = new StackPane();
        mainPane = new BorderPane();
        rootPane.getChildren().addAll(mainPane);
        
        mainPane.setCenter(centerBase.centerRoot);
        mainPane.setTop(topBase.topRoot);
        mainPane.setLeft(leftBase.leftRoot);
        mainPane.setBottom(bottomBase.bottomRoot);
        mainPane.setRight(rightBase.rightRoot);

        BorderPane.setMargin(
            mainPane.getCenter(), 
            new Insets(12, 12, 12, 12)
        );

        actorStage.setScene(new Scene(rootPane));
    }
    
    private void initNodes() {
        centerBase = new CenterBase();
        topBase = new TopBase();
        leftBase = new LeftBase();
        bottomBase = new BottomBase();
        rightBase = new RightBase();
    }

    // Updaters for when certain elements need to be enabled or disabled
    // ========================================================================
    private void updateStepSliderBounds() {

        rightBase.stepSlider.setMin(0);
        rightBase.stepSlider.setMax((locationData.getMaxStep()) * ACTOR_TRAVERSAL_FREQUENCY);
        rightBase.stepSlider.setValue((locationData.getStep()) * ACTOR_TRAVERSAL_FREQUENCY);
        rightBase.stepSlider.setMajorTickUnit(locationData.getMaxStep() * ACTOR_TRAVERSAL_FREQUENCY);
        rightBase.stepSlider.setBlockIncrement(ACTOR_TRAVERSAL_FREQUENCY);
    }

    private void updateCanUseRightButtons() {

        rightBase.stepForwardButton.setDisable(!locationData.canIncreaseStep());
        rightBase.stepBackwardButton.setDisable(!locationData.canDecreaseStep());

        rightBase.nextPhaseButton.setDisable(!locationData.canIncreasePeriod());
        rightBase.prevPhaseButton.setDisable(!locationData.canDecreasePeriod());
    }

    private void updateZoom() {

        for (int r = 0; r < grid.getHeight(); r++) {
            for (int c = 0; c < grid.getWidth(); c++) {

                Rectangle current = grid.getRectangleAt(r, c);

                current.setX(c * TILE_SPACE);
                current.setY(r * TILE_SPACE);

                current.setWidth(TILE_LENGTH);
                current.setHeight(TILE_LENGTH);
            }
        }
    }

    private boolean canSimulate() { 

        return recentlyVerified 
            && recentlyGeneratedSchedules
            && ACTOR_TRAVERSAL_FREQUENCY > 0
            && TILE_SCALE > 0
            && !("".equals(DISTANCE_UNIT))
            && !("".equals(TIME_UNIT));
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

    private void drawSimulation() {
        
        int period = locationData.getPeriod();
        int step = locationData.getStep();

        for (Tile validTile : grid.getValidTiles()) {
            
            if ("floor".equals(validTile.getAttribute())) {

                Color update = locationData.getIntensityAt(period, step, validTile);
                
                int row = validTile.getRow();
                int col = validTile.getCol();

                Rectangle rectangle = grid.getRectangleAt(row, col);
                rectangle.setFill(update);
            }
        }
    }

    public Simulator(int width,
                     int height,
                     String simulationName,
                     String[][] attributes) {

        initColors();

        this.simulationName = simulationName;
        this.tilePaintbrush = "floor";

        this.grid = new Grid(width, height);

        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                grid.setAttributeAt(r, c, attributes[r][c]);
            }
        }

        this.messageLog = new MessageLog(10);

        initStage();
    }

    public void show() {
        
        actorStage.show();

        messageLog.println("[Info] Started a simulation with the following dimensions:");
        messageLog.println("[Info] Width: " + grid.getWidth());
        messageLog.println("[Info] Height: " + grid.getHeight());
    }
}

