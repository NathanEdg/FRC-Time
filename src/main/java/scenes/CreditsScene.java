package scenes;

import helpers.Constants;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class CreditsScene {
  private final VBox content;

  public CreditsScene() {
    Button back = new Button("Back to attendance");
    back.setOnAction(event -> SceneManager.updateScene(Constants.kMainSceneState));
    Label title = new Label("About Team 293 Time");
    title.getStyleClass().add("heading");
    title.setWrapText(true);
    Label summary = new Label(Constants.kCreditsSummary);
    summary.setWrapText(true);
    summary.setMaxWidth(520);
    Label libraries = new Label(Constants.kCreditsList);
    libraries.setWrapText(true);
    libraries.setMaxWidth(520);
    Label version = new Label("Version " + Constants.kVersion);
    content = new VBox(24, KioskView.logo(120), title, summary, libraries, version, back);
    content.setId("about");
    content.setAlignment(Pos.CENTER);
  }

  public void show(GridPane root) {
    ScrollPane scroll = new ScrollPane(content);
    scroll.setFitToWidth(true);
    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    GridPane.setHgrow(scroll, Priority.ALWAYS);
    GridPane.setVgrow(scroll, Priority.ALWAYS);
    root.add(scroll, 0, 0);
  }
}
