package scenes;

import activities.LocalDbActivity;
import activities.MemberRegistration;
import activities.UserActivity;
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
    view.setBusy(true);
    Task<String> task =
        new Task<>() {
          @Override
          protected String call() {
            UserActivity.AttendanceStatus status = users.attendanceStatus(id);
            if (status == UserActivity.AttendanceStatus.UNREGISTERED) {
              return null;
            }
            if (!LocalDbActivity.kHandsFreeMode
                && !new AlertUtils().confirmInput("Confirm attendance for member ID " + id + ".")) {
              return "";
            }
            if (status == UserActivity.AttendanceStatus.CHECKED_IN) {
              return users.logoutUser(id);
            }
            users.loginUser(id);
            return "You're checked in.";
          }
        };
    task.setOnSucceeded(
        event -> {
          String message = task.getValue();
          if (message == null) {
            view.showRegistration(
                id, member -> register(id, member), () -> view.finishRegistration(""));
          } else if (message.isEmpty()) {
            view.setBusy(false);
            view.setStatus("", false);
            view.focusInput();
          } else {
            view.finishRegistration(message);
          }
        });
    task.setOnFailed(
        event -> {
          view.setBusy(false);
          view.setStatus(
              failureMessage(
                  task.getException(),
                  "Attendance could not be saved. Check your connection and try again."),
              true);
          view.focusInput();
        });
    start(task);
  }

  private void register(String id, MemberRegistration member) {
    view.setRegistrationBusy();
    Task<Void> task =
        new Task<>() {
          @Override
          protected Void call() {
            users.registerAndCheckIn(member, id);
            return null;
          }
        };
    task.setOnSucceeded(event -> view.finishRegistration("You're registered and checked in."));
    task.setOnFailed(
        event ->
            view.showRegistrationError(
                failureMessage(
                    task.getException(),
                    "Registration could not be confirmed. Check your connection and try again.")));
    start(task);
  }

  private static String failureMessage(Throwable failure, String fallback) {
    return failure instanceof IllegalArgumentException || failure instanceof IllegalStateException
        ? failure.getMessage()
        : fallback;
  }

  private static void start(Task<?> task) {
    Thread worker = new Thread(task, "attendance");
    worker.setDaemon(true);
    worker.start();
  }
}
