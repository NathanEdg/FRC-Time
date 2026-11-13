import activities.KeyActivity;
import activities.LocalDbActivity;
import activities.UserActivity;
import helpers.AlertUtils;
import helpers.CommonUtils;
import helpers.Constants;
import java.nio.file.Files;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import scenes.SceneManager;
import scenes.SplashScene;

public class Team293Time extends Application {
  @Override
  public void start(Stage stage) {
    CommonUtils.application = this;
    AlertUtils.stage = stage;
    GridPane root = new GridPane();
    SplashScene splash = new SplashScene();
    GridPane.setHgrow(splash, Priority.ALWAYS);
    GridPane.setVgrow(splash, Priority.ALWAYS);
    root.add(splash, 0, 0);
    Scene scene = new Scene(root, Constants.kMainStageWidth, Constants.kMainStageHeight);
    scene
        .getStylesheets()
        .add(getClass().getResource("/" + Constants.kRootStylesheet).toExternalForm());
    java.nio.file.Path customStyle = CommonUtils.getDataDir().resolve("styles/style.css");
    if (Files.isRegularFile(customStyle)) {
      scene.getStylesheets().add(customStyle.toUri().toString());
    }
    stage.setTitle(Constants.kApplicationName);
    stage.getIcons().add(new Image(Constants.kApplicationIcon));
    stage.setMinWidth(420);
    stage.setMinHeight(720);
    stage.setScene(scene);
    new KeyActivity().setKeyHandlers(scene, stage);
    stage.show();

    Task<UserActivity> initialize =
        new Task<>() {
          @Override
          protected UserActivity call() throws Exception {
            new LocalDbActivity().updateLocalDb();
            return new UserActivity();
          }
        };
    initialize.setOnSucceeded(
        event -> {
          stage.setTitle(LocalDbActivity.kApplicationName);
          SceneManager.initialize(root, initialize.getValue());
          SceneManager.updateScene(Constants.kMainSceneState);
        });
    initialize.setOnFailed(
        event -> {
          Throwable failure = initialize.getException();
          String message =
              failure instanceof java.io.IOException
                      || failure instanceof IllegalArgumentException
                      || failure instanceof IllegalStateException
                  ? failure.getMessage()
                  : "Setup could not finish. Check your credentials, configuration, and internet connection.";
          LabelError.show(root, message, () -> startConnection(stage, root));
        });
    Thread worker = new Thread(initialize, "connect-sheets");
    worker.setDaemon(true);
    worker.start();
  }

  private void startConnection(Stage stage, GridPane root) {
    // Retry with the same scene, preserving the existing window and configuration.
    Task<UserActivity> retry =
        new Task<>() {
          @Override
          protected UserActivity call() throws Exception {
            new LocalDbActivity().updateLocalDb();
            return new UserActivity();
          }
        };
    root.getChildren().clear();
    SplashScene splash = new SplashScene();
    GridPane.setHgrow(splash, Priority.ALWAYS);
    GridPane.setVgrow(splash, Priority.ALWAYS);
    root.add(splash, 0, 0);
    retry.setOnSucceeded(
        event -> {
          stage.setTitle(LocalDbActivity.kApplicationName);
          SceneManager.initialize(root, retry.getValue());
          SceneManager.updateScene(Constants.kMainSceneState);
        });
    retry.setOnFailed(
        event ->
            LabelError.show(
                root,
                "Could not connect. Check the app data folder, credentials, sheet permissions, and internet.",
                () -> startConnection(stage, root)));
    Thread worker = new Thread(retry, "retry-sheets");
    worker.setDaemon(true);
    worker.start();
  }

  private static class LabelError {
    static void show(GridPane root, String message, Runnable retry) {
      javafx.scene.control.Label title = new javafx.scene.control.Label("Let's get connected");
      title.getStyleClass().add("heading");
      javafx.scene.control.Label text = new javafx.scene.control.Label(message);
      text.setWrapText(true);
      text.setMaxWidth(500);
      javafx.scene.control.Button button = new javafx.scene.control.Button("Try again");
      button.setOnAction(event -> retry.run());
      javafx.scene.layout.VBox error =
          new javafx.scene.layout.VBox(24, scenes.KioskView.logo(112), title, text, button);
      error.setAlignment(javafx.geometry.Pos.CENTER);
      error.setId("splash");
      GridPane.setHgrow(error, Priority.ALWAYS);
      GridPane.setVgrow(error, Priority.ALWAYS);
      root.getChildren().setAll(error);
    }
  }
}
