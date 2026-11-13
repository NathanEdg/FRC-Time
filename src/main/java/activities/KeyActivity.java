package activities;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

public class KeyActivity {
  public void setKeyHandlers(Scene scene, Stage stage) {
    scene.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.F11) {
            stage.setFullScreen(!stage.isFullScreen());
            event.consume();
          }
        });
  }
}
