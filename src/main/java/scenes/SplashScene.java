package scenes;

import helpers.Constants;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

public class SplashScene extends VBox {
  public SplashScene() {
    Label title = new Label(Constants.kApplicationName);
    title.getStyleClass().add("heading");
    Label status = new Label("Connecting to your attendance sheet…");
    ProgressIndicator progress = new ProgressIndicator();
    progress.setMaxSize(30, 30);
    getChildren().addAll(KioskView.logo(128), title, status, progress);
    setAlignment(Pos.CENTER);
    setSpacing(24);
    setId("splash");
  }
}
