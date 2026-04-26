package com.sshclient.app;

import com.sshclient.config.ConnectionConfig;
import com.sshclient.core.SessionManager;
import com.sshclient.ssh.SSHClientService;
import com.sshclient.terminal.TerminalEmulator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class Main extends Application {

    private SessionManager sessionManager = new SessionManager();
    private SSHClientService sshService;
    private TerminalEmulator emulator;
    
    private Stage configStage;
    private Stage terminalStage;

    // UI Fields
    private TextField hostField;
    private TextField portField;
    private TextField userField;
    private PasswordField passField;
    
    private TextField colsField;
    private TextField rowsField;
    
    private TextField keepaliveField;
    
    private TextField keyPathField;

    private CheckBox implicitCRBox;
    private CheckBox implicitLFBox;

    @Override
    public void start(Stage primaryStage) {
        this.configStage = primaryStage;
        showConfigWindow();
    }

    private void showConfigWindow() {
        configStage.setTitle("BarracudaSSH Configuration");
        try {
            configStage.getIcons().add(new Image(getClass().getResourceAsStream("/barracudassh.png")));
        } catch (Exception e) {
            // Icon could not be loaded
        }

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // --- Left: Category Tree ---
        TreeView<String> categoryTree = new TreeView<>();
        TreeItem<String> rootItem = new TreeItem<>("Category:");
        rootItem.setExpanded(true);
        
        TreeItem<String> sessionNode = new TreeItem<>("Session");
        TreeItem<String> terminalNode = new TreeItem<>("Terminal");
        TreeItem<String> windowNode = new TreeItem<>("Window");
        TreeItem<String> connectionNode = new TreeItem<>("Connection");
        TreeItem<String> sshNode = new TreeItem<>("SSH");
        TreeItem<String> authNode = new TreeItem<>("Auth");
        
        connectionNode.getChildren().add(sshNode);
        sshNode.getChildren().add(authNode);
        connectionNode.setExpanded(true);
        
        rootItem.getChildren().addAll(sessionNode, terminalNode, windowNode, connectionNode);
        categoryTree.setRoot(rootItem);
        categoryTree.setShowRoot(true);
        categoryTree.setPrefWidth(180);

        // --- Right: Content Area (StackPane) ---
        StackPane contentStack = new StackPane();
        contentStack.setPadding(new Insets(0, 0, 0, 10));
        
        // 1. Session Panel
        VBox sessionPanel = createSessionPanel();
        
        // 2. Terminal Panel
        VBox terminalPanel = createTerminalPanel();
        
        // 3. Window Panel
        VBox windowPanel = createWindowPanel();
        
        // 4. Connection Panel
        VBox connectionPanel = createConnectionPanel();
        
        // 5. Auth Panel
        VBox authPanel = createAuthPanel();

        contentStack.getChildren().addAll(sessionPanel, terminalPanel, windowPanel, connectionPanel, authPanel);

        // Hide all initially except Session
        terminalPanel.setVisible(false);
        windowPanel.setVisible(false);
        connectionPanel.setVisible(false);
        authPanel.setVisible(false);

        // TreeView Listener
        categoryTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            sessionPanel.setVisible(false);
            terminalPanel.setVisible(false);
            windowPanel.setVisible(false);
            connectionPanel.setVisible(false);
            authPanel.setVisible(false);
            
            String selected = newVal.getValue();
            switch (selected) {
                case "Session": sessionPanel.setVisible(true); break;
                case "Terminal": terminalPanel.setVisible(true); break;
                case "Window": windowPanel.setVisible(true); break;
                case "Connection": connectionPanel.setVisible(true); break;
                case "Auth": authPanel.setVisible(true); break;
                default: sessionPanel.setVisible(true); break;
            }
        });
        
        categoryTree.getSelectionModel().select(sessionNode);

        // --- Bottom: Open/Cancel Buttons ---
        HBox bottomButtons = new HBox(10);
        bottomButtons.setPadding(new Insets(10, 0, 0, 0));
        bottomButtons.setAlignment(Pos.CENTER_RIGHT);
        
        Button openBtn = new Button("Open");
        openBtn.setPrefWidth(80);
        openBtn.setDefaultButton(true);
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setPrefWidth(80);
        cancelBtn.setCancelButton(true);
        
        bottomButtons.getChildren().addAll(openBtn, cancelBtn);

        // --- Layout Assembly ---
        root.setLeft(categoryTree);
        root.setCenter(contentStack);
        root.setBottom(bottomButtons);
        
        // --- Actions ---
        cancelBtn.setOnAction(e -> Platform.exit());
        
        openBtn.setOnAction(e -> {
            ConnectionConfig config = new ConnectionConfig(
                    hostField.getText(),
                    Integer.parseInt(portField.getText()),
                    userField.getText()
            );
            config.setPassword(passField.getText());
            config.setColumns(Integer.parseInt(colsField.getText()));
            config.setRows(Integer.parseInt(rowsField.getText()));
            config.setKeepaliveInterval(Integer.parseInt(keepaliveField.getText()));
            config.setPrivateKeyPath(keyPathField.getText());
            config.setImplicitCR(implicitCRBox.isSelected());
            config.setImplicitLF(implicitLFBox.isSelected());
            
            configStage.hide();
            showTerminalWindow(config);
        });

        Scene scene = new Scene(root, 550, 480);
        configStage.setScene(scene);
        configStage.show();
    }

    private VBox createSessionPanel() {
        VBox panel = new VBox(15);
        
        TitledPane basicOptions = new TitledPane();
        basicOptions.setText("Basic options for your BarracudaSSH session");
        basicOptions.setCollapsible(false);
        
        GridPane basicGrid = new GridPane();
        basicGrid.setHgap(10); basicGrid.setVgap(5); basicGrid.setPadding(new Insets(10));
        
        hostField = new TextField("localhost");
        portField = new TextField("22");
        portField.setPrefWidth(60);
        
        basicGrid.add(new Label("Host Name (or IP address)"), 0, 0);
        basicGrid.add(hostField, 0, 1);
        basicGrid.add(new Label("Port"), 1, 0);
        basicGrid.add(portField, 1, 1);
        
        userField = new TextField(System.getProperty("user.name"));
        passField = new PasswordField();
        basicGrid.add(new Label("Username"), 0, 2);
        basicGrid.add(userField, 0, 3);
        basicGrid.add(new Label("Password (Auth)"), 1, 2);
        basicGrid.add(passField, 1, 3);
        
        basicOptions.setContent(basicGrid);
        
        TitledPane savedSessionsPane = new TitledPane();
        savedSessionsPane.setText("Load, save or delete a stored session");
        savedSessionsPane.setCollapsible(false);
        
        VBox savedBox = new VBox(5);
        savedBox.setPadding(new Insets(10));
        
        TextField savedField = new TextField("Default Settings");
        ListView<String> savedList = new ListView<>();
        savedList.getItems().add("Default Settings");
        savedList.getItems().addAll(sessionManager.getAllSessions().keySet());
        savedList.setPrefHeight(120);
        
        HBox btns = new HBox(5);
        Button loadBtn = new Button("Load"); Button saveBtn = new Button("Save"); Button deleteBtn = new Button("Delete");
        loadBtn.setPrefWidth(70); saveBtn.setPrefWidth(70); deleteBtn.setPrefWidth(70);
        btns.getChildren().addAll(loadBtn, saveBtn, deleteBtn);
        btns.setAlignment(Pos.CENTER_RIGHT);
        
        savedList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) savedField.setText(newV);
        });

        saveBtn.setOnAction(e -> {
            String name = savedField.getText();
            if (name == null || name.isEmpty() || name.equals("Default Settings")) return;
            ConnectionConfig c = new ConnectionConfig(hostField.getText(), Integer.parseInt(portField.getText()), userField.getText());
            c.setColumns(Integer.parseInt(colsField.getText()));
            c.setRows(Integer.parseInt(rowsField.getText()));
            c.setKeepaliveInterval(Integer.parseInt(keepaliveField.getText()));
            c.setPrivateKeyPath(keyPathField.getText());
            c.setImplicitCR(implicitCRBox.isSelected());
            c.setImplicitLF(implicitLFBox.isSelected());
            
            sessionManager.saveSession(name, c);
            if (!savedList.getItems().contains(name)) savedList.getItems().add(name);
        });

        loadBtn.setOnAction(e -> {
            String name = savedList.getSelectionModel().getSelectedItem();
            if (name != null && sessionManager.getAllSessions().containsKey(name)) {
                ConnectionConfig c = sessionManager.loadSession(name);
                hostField.setText(c.getHost());
                portField.setText(String.valueOf(c.getPort()));
                userField.setText(c.getUsername());
                colsField.setText(String.valueOf(c.getColumns()));
                rowsField.setText(String.valueOf(c.getRows()));
                keepaliveField.setText(String.valueOf(c.getKeepaliveInterval()));
                keyPathField.setText(c.getPrivateKeyPath() != null ? c.getPrivateKeyPath() : "");
                implicitCRBox.setSelected(c.isImplicitCR());
                implicitLFBox.setSelected(c.isImplicitLF());
            }
        });

        deleteBtn.setOnAction(e -> {
            String name = savedList.getSelectionModel().getSelectedItem();
            if (name != null && !name.equals("Default Settings")) {
                sessionManager.deleteSession(name);
                savedList.getItems().remove(name);
            }
        });

        savedBox.getChildren().addAll(new Label("Saved Sessions"), savedField, savedList, btns);
        savedSessionsPane.setContent(savedBox);
        panel.getChildren().addAll(basicOptions, savedSessionsPane);
        return panel;
    }

    private VBox createTerminalPanel() {
        VBox panel = new VBox(15);
        implicitCRBox = new CheckBox("Implicit CR in every LF");
        implicitLFBox = new CheckBox("Implicit LF in every CR");
        TitledPane opt = new TitledPane("Options controlling the terminal emulation", new VBox(10, 
            implicitCRBox,
            implicitLFBox
        ));
        opt.setCollapsible(false);
        panel.getChildren().add(opt);
        return panel;
    }

    private VBox createWindowPanel() {
        VBox panel = new VBox(15);
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(5); grid.setPadding(new Insets(10));
        
        colsField = new TextField("80"); colsField.setPrefWidth(50);
        rowsField = new TextField("24"); rowsField.setPrefWidth(50);
        
        grid.add(new Label("Columns:"), 0, 0); grid.add(colsField, 1, 0);
        grid.add(new Label("Rows:"), 0, 1); grid.add(rowsField, 1, 1);
        
        TitledPane opt = new TitledPane("Set the size of the window", grid);
        opt.setCollapsible(false);
        panel.getChildren().add(opt);
        return panel;
    }

    private VBox createConnectionPanel() {
        VBox panel = new VBox(15);
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        keepaliveField = new TextField("0"); keepaliveField.setPrefWidth(50);
        HBox hb = new HBox(10, new Label("Seconds between keepalives (0 to turn off)"), keepaliveField);
        hb.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(hb);
        
        TitledPane opt = new TitledPane("Options controlling the connection", box);
        opt.setCollapsible(false);
        panel.getChildren().add(opt);
        return panel;
    }

    private VBox createAuthPanel() {
        VBox panel = new VBox(15);
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        keyPathField = new TextField();
        Button browseBtn = new Button("Browse...");
        HBox hb = new HBox(10, keyPathField, browseBtn);
        HBox.setHgrow(keyPathField, Priority.ALWAYS);
        
        browseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Private Key File");
            File file = fc.showOpenDialog(configStage);
            if (file != null) keyPathField.setText(file.getAbsolutePath());
        });
        
        box.getChildren().addAll(new Label("Private key file for authentication:"), hb);
        
        TitledPane opt = new TitledPane("Options controlling SSH authentication", box);
        opt.setCollapsible(false);
        panel.getChildren().add(opt);
        return panel;
    }

    private void showTerminalWindow(ConnectionConfig config) {
        terminalStage = new Stage();
        terminalStage.setTitle(config.getHost() + " - BarracudaSSH");
        try {
            terminalStage.getIcons().add(new Image(getClass().getResourceAsStream("/barracudassh.png")));
        } catch (Exception e) {}

        TextArea terminalArea = new TextArea();
        terminalArea.setStyle("-fx-font-family: 'Courier New'; -fx-control-inner-background: #000000; -fx-text-fill: #BBBBBB; -fx-font-size: 14px; -fx-highlight-fill: #555555; -fx-text-box-border: transparent; -fx-focus-color: transparent;");
        terminalArea.setWrapText(true);

        terminalArea.addEventFilter(KeyEvent.KEY_TYPED, event -> event.consume());
        terminalArea.setOnMouseClicked(event -> terminalArea.positionCaret(terminalArea.getLength()));

        terminalArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (emulator != null && sshService != null && sshService.isConnected()) {
                String input = "";
                switch (event.getCode()) {
                    case ENTER: input = "\r"; break;
                    case BACK_SPACE: input = "\177"; break;
                    case TAB: input = "\t"; break;
                    case UP: input = "\033[A"; break;
                    case DOWN: input = "\033[B"; break;
                    case RIGHT: input = "\033[C"; break;
                    case LEFT: input = "\033[D"; break;
                    default:
                        if (!event.getText().isEmpty()) {
                            input = event.getText();
                        }
                        break;
                }
                if (!input.isEmpty()) {
                    emulator.sendInput(input);
                }
                event.consume();
            }
        });

        Scene scene = new Scene(terminalArea, 800, 600);
        terminalStage.setScene(scene);
        terminalStage.setOnCloseRequest(e -> {
            disconnect();
            Platform.exit();
        });
        terminalStage.show();

        connect(config, terminalArea);
    }

    private void connect(ConnectionConfig config, TextArea terminalArea) {
        terminalArea.appendText("Looking up host \"" + config.getHost() + "\"...\n");
        terminalArea.appendText("Connecting to " + config.getHost() + " port " + config.getPort() + "...\n");

        new Thread(() -> {
            try {
                sshService = new SSHClientService();
                
                PipedInputStream sshIn = new PipedInputStream();
                PipedOutputStream sshOut = new PipedOutputStream();
                PipedInputStream uiIn = new PipedInputStream();
                PipedOutputStream uiOut = new PipedOutputStream();

                sshIn.connect(uiOut);
                uiIn.connect(sshOut);

                emulator = new TerminalEmulator(config, terminalArea, uiIn, uiOut);
                emulator.start();

                sshService.connect(config, sshIn, sshOut, sshOut);

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    terminalArea.appendText("\n[Network error: " + ex.getMessage() + "]\n");
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("BarracudaSSH Fatal Error");
                    alert.setHeaderText("Network error: " + ex.getMessage());
                    
                    // Show full stack trace in an expandable text area
                    java.io.StringWriter sw = new java.io.StringWriter();
                    ex.printStackTrace(new java.io.PrintWriter(sw));
                    TextArea traceArea = new TextArea(sw.toString());
                    traceArea.setEditable(false);
                    traceArea.setWrapText(true);
                    traceArea.setMaxWidth(Double.MAX_VALUE);
                    traceArea.setMaxHeight(Double.MAX_VALUE);
                    javafx.scene.layout.GridPane.setVgrow(traceArea, javafx.scene.layout.Priority.ALWAYS);
                    javafx.scene.layout.GridPane.setHgrow(traceArea, javafx.scene.layout.Priority.ALWAYS);
                    javafx.scene.layout.GridPane expContent = new javafx.scene.layout.GridPane();
                    expContent.setMaxWidth(Double.MAX_VALUE);
                    expContent.add(new Label("The exception stacktrace was:"), 0, 0);
                    expContent.add(traceArea, 0, 1);
                    alert.getDialogPane().setExpandableContent(expContent);
                    alert.getDialogPane().setExpanded(true);
                    
                    alert.showAndWait();
                    
                    terminalStage.hide();
                    configStage.show();
                });
            }
        }).start();
    }

    private void disconnect() {
        if (emulator != null) emulator.stop();
        if (sshService != null) sshService.disconnect();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
