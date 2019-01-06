import javafx.application.Application;

import javafx.geometry.Insets;

import javafx.event.*;
import javafx.stage.*;

import javafx.scene.*;

import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;

import javafx.scene.layout.*;

import java.util.Scanner;
import java.util.Arrays;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.lang.RuntimeException;

public class MainApplication extends Application {
    
    public static void main(String[] args) {
        Application.launch(args);
    }

    Button createNewButton = new Button("Create New");
    Button loadButton = new Button("Load from File");

    @Override
    public void start(Stage primaryStage) {
        
        primaryStage.setTitle("Network Simulator");
        
        initButtonBehavior();

        GridPane gridpane = new GridPane();
        gridpane.add(createNewButton, 0, 0);
        gridpane.add(loadButton, 1, 0);
        
        gridpane.setHgap(6);
        gridpane.setVgap(6);

        Pane rootGroup = new VBox(12);
        rootGroup.getChildren().addAll(gridpane);
        rootGroup.setPadding(new Insets(12, 12, 12, 12));

        primaryStage.setScene(new Scene(rootGroup));
        primaryStage.show();
    }

    private void initButtonBehavior() {

        createNewButton.setOnAction(new EventHandler<ActionEvent>() {
            
            @Override
            public void handle(ActionEvent event) {
                promptForNew();
            }
        });

        loadButton.setOnAction(new EventHandler<ActionEvent>() {
            
            @Override
            public void handle(ActionEvent event) {
                promptForLoad();
            }
        });
    }

    public void promptForLoad() {
        
        Stage loadingStage = new Stage();
        loadingStage.setTitle("Choose file to load");

        boolean fileNeeded = true;
        boolean hasCanceled = false;

        String simulationName = "";
        String[][] attributes = null;

        int width = -1;
        int height = -1;

        while (fileNeeded && !hasCanceled) {

            FileChooser fileChooser = new FileChooser();
            
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Network Data Files", "*.ntwk"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
            );

            File fileToOpen = fileChooser.showOpenDialog(loadingStage);
            
            hasCanceled = fileToOpen == null;
            if (hasCanceled) break;

            fileNeeded = !fileToOpen.getName().endsWith(".ntwk");

            try {
                
                if (fileNeeded) {
                    throw new RuntimeException(
                        "File has improper extension (seeking .ntwk). Try again."
                    );
                }

                Scanner scan = new Scanner(fileToOpen);
                
                if (!("ntwk_file".equals(scan.next()))) {
                    throw new RuntimeException(
                        "File has improper header and may be corrupted. Try again."
                    );
                }

                simulationName = scan.next();

                width = scan.nextInt();
                height = scan.nextInt();
                
                attributes = new String[height][width];

                for (int r = 0; r < height; r++) {
                    for (int c = 0; c < width; c++) {
                        attributes[r][c] = scan.next();
                    }
                }
            }
            catch (FileNotFoundException ex) {
                
                Alert badFileAlert = new Alert(
                    AlertType.ERROR,
                    "File not found. Try again.",
                    ButtonType.OK
                );

                badFileAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                badFileAlert.showAndWait();

                loadingStage.hide();

                fileNeeded = true;
                hasCanceled = false;
                continue;
            }
            catch (RuntimeException ex) {
                
                Alert badFileAlert = new Alert(
                    AlertType.ERROR,
                    ex.getMessage(),
                    ButtonType.OK
                );

                badFileAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                badFileAlert.showAndWait();
                
                loadingStage.hide();

                fileNeeded = true;
                hasCanceled = false;
                continue;
            }
        }
        
        if (hasCanceled) return;
        loadCachedSimulation(width, height, simulationName, attributes);
    }

    public void promptForNew() {
        
        Stage promptStage = new Stage();
        promptStage.setTitle("Create New");

        GridPane gridpane = new GridPane();
        
        TextField nameTextField = new TextField();
        nameTextField.setPromptText("Enter simulation name");

        TextField widthTextField = new TextField();
        widthTextField.setPromptText("Enter width");

        TextField heightTextField = new TextField();
        heightTextField.setPromptText("Enter height");

        Button createButton = new Button("Create");
        createButton.setOnAction(new EventHandler<ActionEvent>() {
            
            @Override
            public void handle(ActionEvent event) {
                
                String widthString = widthTextField.getText();
                String heightString = heightTextField.getText();
                String simulationName = nameTextField.getText();
                
                int width = -1;
                int height = -1;

                try {
                    
                    width = Integer.parseInt(widthString);
                    height = Integer.parseInt(heightString);
                    
                    if (width <= 0 || height <= 0) {
                        throw new Exception("Invalid width / height");
                    }
                    
                    if ("".equals(simulationName) || simulationName == null) {
                        throw new Exception("Invalid filename");
                    }

                    int countOfPeriods = simulationName.length() 
                                       - simulationName.replace(".", "").length();

                    if (countOfPeriods > 0) {
                        throw new Exception("Invalid characters in filename");
                    }
                }
                catch (Exception ex) {
                    
                    Alert badInputAlert = new Alert(
                        AlertType.ERROR,
                        "Please enter positive integers for width and height " 
                      + "and a valid filename.", 
                        ButtonType.OK
                    );

                    badInputAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                    badInputAlert.showAndWait();
                    return;
                }
                
                promptStage.hide();
                startNewSimulation(width, height, simulationName);
            }
        });

        gridpane.add(nameTextField, 0, 0);
        gridpane.add(widthTextField, 0, 1);
        gridpane.add(heightTextField, 0, 2);
        gridpane.add(createButton, 0, 3);
        
        gridpane.setHgap(6);
        gridpane.setVgap(6);

        Pane rootGroup = new VBox(12);
        rootGroup.getChildren().addAll(gridpane);
        rootGroup.setPadding(new Insets(12, 12, 12, 12));

        promptStage.setScene(new Scene(rootGroup));
        promptStage.show();
    }

    public void loadCachedSimulation(
        int width, 
        int height, 
        String simulationName, 
        String[][] attributes) {

        Simulator simulator = new Simulator(width, height, simulationName, attributes);
        simulator.show();
    }

    public void startNewSimulation(int width, int height, String simulationName) {

        String[][] attributes = new String[height][width];
        for (String[] row : attributes) {
            Arrays.fill(row, "wall");
        }
        
        loadCachedSimulation(width, height, simulationName, attributes);
    }
}
