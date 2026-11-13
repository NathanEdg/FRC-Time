import activities.LocalDbActivity;
import helpers.AlertUtils;
import helpers.Constants;
import helpers.InputValidation;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
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
            view.setStatus("Enter a 6- or 7-digit member ID to preview registration.", true);
            return;
          }
          new AlertUtils()
              .getUserInfo()
              .ifPresent(
                  member -> {
                    view.setStatus(
                        "Preview complete. No account or attendance data was saved.", false);
                    view.clearAndFocus();
                  });
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
  }
}
