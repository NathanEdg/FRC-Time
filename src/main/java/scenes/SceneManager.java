package scenes;

import activities.UserActivity;
import helpers.Constants;
import javafx.scene.layout.GridPane;

public final class SceneManager {
  private static GridPane root;
  private static KioskScene kiosk;
  private static CreditsScene credits;

  private SceneManager() {}

  public static void initialize(GridPane container, UserActivity users) {
    root = container;
    kiosk = new KioskScene(users);
    credits = new CreditsScene();
  }

  public static void updateScene(int scene) {
    root.getChildren().clear();
    if (scene == Constants.kMainSceneState) {
      kiosk.show(root);
    } else if (scene == Constants.kCreditsSceneState) {
      credits.show(root);
    } else {
      throw new IllegalArgumentException("Unknown screen.");
    }
  }
}
