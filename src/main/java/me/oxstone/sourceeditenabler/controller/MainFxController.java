package me.oxstone.sourceeditenabler.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import me.oxstone.sourceeditenabler.JavaFxApplication;
import net.rgielen.fxweaver.core.FxmlView;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Controller
@FxmlView("MainFx.fxml")
public class MainFxController {

    @FXML
    private MenuItem menuClose;

    @FXML
    private MenuItem menuAbout;

    @FXML
    private Tab tabSourceEditEnabler;

    @FXML
    private Label lblFolderPath;

    @FXML
    private TextField txtFolderPath;

    @FXML
    private Button btnSearch;

    @FXML
    private Button btnEnabler;

    private static final String DESKTOP_PATH = System.getProperty("user.home") + "\\Desktop";
    private static final String DEFAULT_PATH = System.getProperty("user.home") + "\\Documents\\Studio 2021\\Projects";

    @Autowired
    public MainFxController() {

    }

    @FXML
    void clickBtnEnabler(ActionEvent event) {
        if (txtFolderPath.getText().equals("")) {
            String title = "Error";
            String header = "Error";
            String msg = "No file selected.";
            showMsgbox(title, header, msg, Alert.AlertType.ERROR);
            return;
        }

        String msg;
        try {
            String filePath = txtFolderPath.getText();
            File file = new File(filePath);
            if (file.exists()) { // 파일이 존재하지 않으면
                //파일 읽기
                SAXReader reader = new SAXReader();
                Document document = reader.read(file);
                Element root = document.getRootElement();
                Element settingsBundles = root.element("SettingsBundles");

                //Enable Settings
                // 루트의 엘리먼트 중 "foo"라는 이름을 갖는엘리먼트의 자식노드 반복
                for (Iterator i = settingsBundles.elementIterator( "SettingsBundle" ); i.hasNext(); ) {
                    boolean haveSettings = false;
                    Element settingsBundle = (Element) i.next();
                    for (Iterator j = settingsBundle.element("SettingsBundle").elementIterator( "SettingsGroup" ); j.hasNext(); ) {
                        Element settingsGroup = (Element) j.next();
                        if (settingsGroup.attribute("Id").getValue().equals("SourceContentSettings")) {
                            haveSettings = true;
                            List<String> idList = List.of("AllowSourceEditing", "AllowMergeAcrossParagraphs");
                            for (String id : idList) {
                                // Setting에 True 값 적용
                                Node node = settingsGroup.selectSingleNode("./Setting[@Id='" + id + "']");
                                if (node != null) {
                                    ((Element) node).setText("True");
                                } else {
                                    // 존재하지 않으면 새로 생성
                                    Element newSetting = settingsGroup.addElement("Setting");
                                    newSetting.addAttribute("Id", id);
                                    newSetting.setText("True");
                                }
                            }
                        }
                    }
                    if (!haveSettings) {
                        Element target = settingsBundle.element("SettingsBundle");
                        target.addElement("SettingsGroup")
                                .addAttribute("Id", "SourceContentSettings");
                        for (Iterator l = target.elementIterator( "SettingsGroup" ); l.hasNext(); ) {
                            Element sourceContentSettings = (Element) l.next();
                            if (sourceContentSettings.attribute("Id").getValue().equals("SourceContentSettings")) {
                                sourceContentSettings.addElement("Setting").addAttribute("Id", "AllowSourceEditing").addText("True");
                                sourceContentSettings.addElement("Setting").addAttribute("Id", "AllowMergeAcrossParagraphs").addText("True");
                            }
                        }
                    }
                }

                //파일 쓰기
                OutputFormat format = OutputFormat.createPrettyPrint();
                XMLWriter writer = new XMLWriter(
                        new FileWriter(filePath, StandardCharsets.UTF_8), format
                );
                writer.write(document);

                // 스트림 종료
                writer.close();
            } else {
                // 파일이 존재하지 않을 시 에러발생
                throw new Exception();
            }

            // 성공 메세지 띄우기
            String title = "Succeed";
            String header = "Enabled successfully.";
            msg = "You can modify and merge source segment in TRADOS STUDIO 2021";
            showMsgbox(title, header, msg, Alert.AlertType.INFORMATION);

        } catch(Exception exception){
            // 실패 메세지 띄우기
            String title = "Fail!";
            String header = "Fail to enable.";
            msg = "Please check once more whether this file is a setting file for TRADOS STUDIO 2021 or not.";
            showMsgbox(title, header, msg, Alert.AlertType.ERROR);
        }

    }

    @FXML
    void clickBtnSearch (ActionEvent event){
        String title = "Select the settings file...";
        File targetDir;
        if (txtFolderPath.getText().equals("")) {
            if (new File(DEFAULT_PATH).exists()) {
                targetDir = showFileChooser(title, btnSearch.getScene().getWindow(), DEFAULT_PATH);
            } else {
                targetDir = showFileChooser(title, btnSearch.getScene().getWindow(), DESKTOP_PATH);
            }
        } else {
            targetDir = showFileChooser(title, btnSearch.getScene().getWindow(), txtFolderPath.getText());
        }
        Platform.runLater(() -> {
            txtFolderPath.setText(targetDir.getPath());
        });
    }

    private File showDirectoryChooser (String title, Window ownerWindow, String path){
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        chooser.setInitialDirectory(new File(path));
        return chooser.showDialog(ownerWindow);
    }

    private File showFileChooser (String title, Window ownerWindow, String path){
        FileChooser chooser = new FileChooser();
        File dir = new File(path);
        chooser.setTitle(title);
        if (dir.isDirectory()) {
            chooser.setInitialDirectory(dir);
        } else {
            chooser.setInitialDirectory(dir.getParentFile());
        }
        return chooser.showOpenDialog(ownerWindow);
    }

    @FXML
    void clickMenuClose (ActionEvent event){
        Platform.exit();
    }

    @FXML
    void clickMenuAbout (ActionEvent event){
        String title = JavaFxApplication.PROGRAM_VER;
        String header = "" +
                "Program Author: " + JavaFxApplication.PROGRAM_AUTHOR +
                "\n\nCopy Right: " + JavaFxApplication.PROGRAM_COPYRIGHT +
                "\n\nLast Modified Date: " + JavaFxApplication.PROGRAM_LAST_MODIFIED;
        String msg = "" +
                "This program activates the source modification and \n\n" +
                "source merging functions of the project downloaded from TRADOS LIVE.";
        showMsgbox(title, header, msg, Alert.AlertType.INFORMATION);
    }

    private void showMsgbox (String title, String header, String content, Alert.AlertType alertType){
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(
                getClass().getResource("Stylesheet.css").toExternalForm());
        dialogPane.getStyleClass().add("myDialog");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            alert.close();
        } else {
            // ... user chose CANCEL or closed the dialog
        }
    }

}

