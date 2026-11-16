package scenes;

import activities.LocalDbActivity;
import activities.UserActivity;
import exceptions.CancelledUserCreationException;
import helpers.AlertUtils;
import javafx.concurrent.Task;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class KioskScene {
  private final UserActivity users;
  private final KioskView view = new KioskView(false);

  public KioskScene(UserActivity users) {
    this.users = users;
    view.onSubmit(this::submit);
    view.onAbout(() -> SceneManager.updateScene(2));
    view.onFullscreen(
        () -> {
          javafx.stage.Stage stage = (javafx.stage.Stage) view.getScene().getWindow();
          stage.setFullScreen(!stage.isFullScreen());
        });
  }

  public void show(GridPane root) {
    GridPane.setHgrow(view, Priority.ALWAYS);
    GridPane.setVgrow(view, Priority.ALWAYS);
    root.add(view, 0, 0);
    view.clearAndFocus();
  }

  private void submit(String id) {
    if (!users.isValidID(id)) {
      view.setStatus("Enter a member ID with " + KioskView.memberIdFormat() + ".", true);
      view.focusInput();
      return;
    }
    if (!LocalDbActivity.kHandsFreeMode
        && !new AlertUtils().confirmInput("Confirm attendance for member ID " + id + ".")) {
      view.setStatus("", false);
      view.focusInput();
      return;
    }
    view.setBusy(true);
    Task<String> task =
        new Task<>() {
          @Override
          protected String call() throws Exception {
            if (users.isUserLoggedIn(id)) {
              return users.logoutUser(id);
            }
            users.loginUser(id);
            return "You're checked in.";
          }
        };
    task.setOnSucceeded(
        event -> {
          view.setBusy(false);
          view.setStatus(task.getValue(), false);
          view.clearAndFocus();
        });
    task.setOnFailed(
        event -> {
          view.setBusy(false);
          Throwable failure = task.getException();
          if (failure instanceof CancelledUserCreationException) {
            view.setStatus("Registration cancelled. Enter an ID when you're ready.", false);
          } else if (failure instanceof IllegalArgumentException
              || failure instanceof IllegalStateException) {
            view.setStatus(failure.getMessage(), true);
          } else {
            view.setStatus(
                "Attendance could not be saved. Check your connection and try again.", true);
          }
          view.focusInput();
        });
    Thread worker = new Thread(task, "attendance");
    worker.setDaemon(true);
    worker.start();
  }
}
