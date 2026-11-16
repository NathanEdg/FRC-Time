package helpers;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class AlertUtils {
  public static Stage stage;

  public boolean createAlert(String title, String header, String content) {
    return onFxThread(
        () -> {
          Dialog<ButtonType> dialog = dialog(title, header);
          dialog.setContentText(content);
          dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
          return dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
        });
  }

  public boolean confirmInput(String message) {
    return onFxThread(
        () -> {
          Dialog<ButtonType> dialog = dialog("Confirm attendance", "Check in or check out?");
          dialog.setContentText(message);
          ButtonType confirm = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
          dialog.getDialogPane().getButtonTypes().addAll(confirm, ButtonType.CANCEL);
          return dialog.showAndWait().orElse(ButtonType.CANCEL) == confirm;
        });
  }

  public boolean confirmMeeting(String action, String pin) {
    return onFxThread(
        () -> {
          Dialog<ButtonType> dialog = dialog("Manage meeting", action);
          PasswordField input = new PasswordField();
          input.setPromptText("Team lead PIN");
          input.setAccessibleText("Team lead PIN");
          dialog.getDialogPane().setContent(input);
          ButtonType confirm = new ButtonType(action, ButtonBar.ButtonData.OK_DONE);
          dialog.getDialogPane().getButtonTypes().addAll(confirm, ButtonType.CANCEL);
          Platform.runLater(input::requestFocus);
          if (dialog.showAndWait().orElse(ButtonType.CANCEL) != confirm) {
            return false;
          }
          if (!pin.equals(input.getText())) {
            throw new IllegalArgumentException("Incorrect team lead PIN.");
          }
          return true;
        });
  }

  private Dialog<ButtonType> dialog(String title, String header) {
    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle(title);
    dialog.setHeaderText(header);
    if (stage != null) {
      dialog.initOwner(stage);
    }
    dialog
        .getDialogPane()
        .getStylesheets()
        .add(getClass().getResource("/" + Constants.kRootStylesheet).toExternalForm());
    dialog.getDialogPane().getStyleClass().add("team-dialog");
    dialog.setOnShowing(
        event -> {
          for (ButtonType type : dialog.getDialogPane().getButtonTypes()) {
            if (type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
              dialog.getDialogPane().lookupButton(type).getStyleClass().add("secondary-button");
            }
          }
        });
    ImageView logo = new ImageView(new Image(Constants.kTeamLogo));
    logo.setPreserveRatio(true);
    logo.setFitWidth(54);
    dialog.setGraphic(logo);
    Stage window = (Stage) dialog.getDialogPane().getScene().getWindow();
    window.getIcons().add(new Image(Constants.kApplicationIcon));
    return dialog;
  }

  public String showSpreadsheetDialog() {
    return onFxThread(
        () -> {
          Dialog<ButtonType> dialog =
              dialog("Connect Google Sheets", "Connect your attendance sheet");
          TextField link = new TextField();
          link.setPromptText("Paste a Google Sheets link");
          link.setPrefColumnCount(36);
          Label explanation =
              new Label(
                  "Choose a sheet you can edit. Attendance will be saved to Current and Date Log.");
          explanation.setWrapText(true);
          explanation.setMaxWidth(470);
          Label label = new Label("Google Sheets link");
          label.setLabelFor(link);
          GridPane form = new GridPane();
          form.setVgap(12);
          form.add(explanation, 0, 0);
          form.add(label, 0, 1);
          form.add(link, 0, 2);
          dialog.getDialogPane().setContent(form);
          ButtonType connect = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
          dialog.getDialogPane().getButtonTypes().addAll(connect, ButtonType.CANCEL);
          dialog
              .getDialogPane()
              .lookupButton(connect)
              .disableProperty()
              .bind(
                  Bindings.createBooleanBinding(
                      () -> {
                        try {
                          InputValidation.spreadsheetId(link.getText());
                          return false;
                        } catch (IllegalArgumentException e) {
                          return true;
                        }
                      },
                      link.textProperty()));
          Platform.runLater(link::requestFocus);
          return dialog.showAndWait().orElse(ButtonType.CANCEL) == connect
              ? InputValidation.spreadsheetId(link.getText())
              : "";
        });
  }

  private static <T> T onFxThread(Callable<T> action) {
    if (Platform.isFxApplicationThread()) {
      try {
        return action.call();
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new IllegalStateException("Unable to display the dialog.");
      }
    }
    FutureTask<T> result = new FutureTask<>(action);
    Platform.runLater(result);
    try {
      return result.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Dialog was interrupted.");
    } catch (ExecutionException e) {
      throw new IllegalStateException("Unable to display the dialog.");
    }
  }
}
