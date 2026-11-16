import activities.LocalDbActivity;
import helpers.AlertUtils;
import helpers.Constants;
import helpers.InputValidation;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;
import scenes.KioskView;

public class PreviewApplication extends Application {
  @Override
  public void start(Stage stage) {
    AlertUtils.stage = stage;
    KioskView view = new KioskView(true);
    Scene scene = new Scene(view, Constants.kMainStageWidth, Constants.kMainStageHeight);
    scene
        .getStylesheets()
        .add(getClass().getResource("/" + Constants.kRootStylesheet).toExternalForm());
    stage.setTitle(Constants.kApplicationName + " · Preview");
    stage.getIcons().add(new Image(Constants.kApplicationIcon));
    stage.setMinWidth(420);
    stage.setMinHeight(720);
    stage.setScene(scene);
    view.onSubmit(
        id -> {
          if (!InputValidation.isValidId(
              id, LocalDbActivity.kIdLength, LocalDbActivity.kIdLengthFallback)) {
            view.setStatus("Enter a member ID with " + KioskView.memberIdFormat() + ".", true);
            return;
          }
          view.showRegistration(
              id,
              member ->
                  view.finishRegistration(
                      "Preview complete. No account or attendance data was saved."),
              () -> view.finishRegistration(""));
        });
    view.onAbout(
        () ->
            new AlertUtils()
                .createAlert(
                    "About Team 293 Time", "Team 293 Time · Preview", Constants.kCreditsSummary));
    view.onFullscreen(() -> stage.setFullScreen(!stage.isFullScreen()));
    new activities.KeyActivity().setKeyHandlers(scene, stage);
    stage.show();
    view.clearAndFocus();
    if (getParameters().getRaw().contains("--smoke-test")) {
      PauseTransition ready = new PauseTransition(Duration.millis(250));
      ready.setOnFinished(
          event -> {
            view.applyCss();
            view.layout();
            System.out.println("PREVIEW_READY");
            Platform.exit();
          });
      ready.play();
    }
  }
}
