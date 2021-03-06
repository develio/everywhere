package controller;

import client.ClientWindow;
import constants.CommonConstants;
import constants.LuceneConstants;
import index.IndexUtil;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import search.SearchUtil;
import search.SearchedResult;
import setting.ConfigController;
import setting.ConfigSetting;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    public TextField searchTextId;

    @FXML
    private TableView tview;

    @FXML
    private ComboBox comboType;

    @FXML
    private Label indexLabel;

    @FXML
    private TableColumn<SearchedResult, String> filepathCol;

    @FXML
    private TableColumn<SearchedResult, String> contextCol;

    private String searchField = LuceneConstants.CONTENT;

    String searchText = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // only select the current cell
        tview.getSelectionModel().setCellSelectionEnabled(true);
        comboType.getItems().addAll(
                    "content",
                    "path"
            );
    }

    public void runIndex(ActionEvent e) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                executeIndex();
                return null;
            }
        };
        task.setOnRunning(event -> {
            indexLabel.setText("indexing, please wait.");
        });
        task.setOnSucceeded(event -> {
            indexLabel.setText("index finished!");
        });
        new Thread(task).start();
        indexLabel.setText("");
    }

    private void executeIndex() {
        ConfigSetting configSetting = ConfigController.readConfig();
        CommonConstants.EXCLUDE_FILE_PATHS = configSetting.getExcludeFilePathList();
        IndexUtil.executeIndex(configSetting.getSearchMethod());
        if (configSetting.getHasCreateIndex() == false) {
            configSetting.setHasCreateIndex(true);
        }
        ConfigController.writeConfigToYaml(configSetting);
    }

    // add listener to the searchType change
    public void getSearchTypeChange(ActionEvent e) {
        if (!comboType.getValue().equals(searchField)) {
            searchField = comboType.getValue().toString();
        }
    }

    public void getSearchTextChanged(InputMethodEvent event) {
        Platform.isSupported(ConditionalFeature.INPUT_METHOD);
        if (!event.getCommitted().isEmpty()) {
            searchText += event.getCommitted();
            searchTextId.setText(searchText);
            searchTextId.end();
            System.out.println(searchText);
            List<SearchedResult> searchedResults = getSearchResult(searchText, searchField);
            showTableData(searchedResults);
        }
    }

    public void getKeyTyped(KeyEvent keyEvent) {
        if ("\b".equals(keyEvent.getCharacter()) && !searchText.isEmpty()) {
            searchText = searchText.substring(0, searchText.length() - 1);
        } else {
            if (!keyEvent.getCharacter().contains("\\")) {
               searchText += keyEvent.getCharacter();
            }
        }
        System.out.println(searchText);
//        searchTextId.setText(searchText);
//        searchTextId.end();
        if (!searchText.isEmpty()) {
            List<SearchedResult> searchedResults = getSearchResult(searchText, searchField);
            showTableData(searchedResults);
        } else {
            tview.setItems(null);
        }
    }

    private List<SearchedResult> getSearchResult(String searchText, String searchField) {
        List<SearchedResult> searchedResults = SearchUtil.executeSearch(searchText, searchField);
        return searchedResults;

    }

    private void showTableData(List<SearchedResult> searchedResults) {
        ObservableList<SearchedResult> list = FXCollections.observableArrayList();
        if (searchedResults != null && searchedResults.size() != 0) {
            for (SearchedResult searchedResult: searchedResults) {
                SearchedResult result = new SearchedResult();
                result.setContext(searchedResult.getContext());
                result.setFilepath(searchedResult.getFilepath());
                filepathCol.setCellFactory(new Callback<TableColumn<SearchedResult, String>, TableCell<SearchedResult, String>>() {
                    @Override
                    public TableCell<SearchedResult, String> call(TableColumn<SearchedResult, String> col) {
                        final TableCell<SearchedResult, String> cell = new TableCell<>();
                        cell.textProperty().bind(cell.itemProperty()); // in general might need to subclass TableCell and override updateItem(...) here
                        cell.setTextFill(Color.BLUE);
                        cell.setOnMouseClicked(new EventHandler<MouseEvent>() {
                            @Override
                            public void handle(MouseEvent event) {
                                if (event.getButton() == MouseButton.PRIMARY) {
                                    cell.setTextFill(Color.GRAY);
                                    ClientWindow clientWindow = new ClientWindow();
                                    clientWindow.getHostServices().showDocument(cell.getText());
                                    // handle right click on cell...
                                    // access cell data with cell.getItem();
                                    // access row data with (Person)cell.getTableRow().getItem();
                                }
                            }
                        });
                        return cell ;
                    }
                });
                filepathCol.setCellValueFactory(new PropertyValueFactory<SearchedResult, String>("filepath"));
                contextCol.setCellValueFactory(new PropertyValueFactory<SearchedResult, String>("context"));
                list.add(result);
            }
            tview.setItems(list);
        }
    }

}
