package scenes;

import activities.LocalDbActivity;
import activities.MeetingActivity;
import activities.MemberRegistration;
import activities.UserActivity;
import helpers.AlertUtils;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import javafx.concurrent.Task;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class KioskScene {
  private final UserActivity users;
  private final MeetingActivity meetings;
  private MeetingActivity.Status meeting;
  private final KioskView view = new KioskView(false);

  public KioskScene(UserActivity users) {
    this.users = users;
    meetings = users.meetings();
    view.onManageMeeting(this::manageMeeting);
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
    refreshMeeting(meetings::status);
  }

  private void manageMeeting() {
    if (LocalDbActivity.kMeetingPin.isEmpty()) {
      view.setStatus("Set meetingPin in the kiosk config to enable meeting controls.", true);
      return;
    }
    if (meeting == null) {
      refreshMeeting(meetings::status);
      return;
    }
    boolean ending = meeting.active();
    try {
      if (new AlertUtils()
          .confirmMeeting(
              ending ? "End meeting and check everyone out" : "Start meeting",
              LocalDbActivity.kMeetingPin)) {
        refreshMeeting(
            () -> ending ? meetings.end(LocalDateTime.now()) : meetings.start(LocalDateTime.now()));
      }
    } catch (IllegalArgumentException e) {
      view.setStatus(e.getMessage(), true);
    }
  }

  private void refreshMeeting(Supplier<MeetingActivity.Status> action) {
    view.setBusy(true);
    Task<MeetingActivity.Status> task =
        new Task<>() {
          @Override
          protected MeetingActivity.Status call() {
            return action.get();
          }
        };
    task.setOnSucceeded(
        event -> {
          meeting = task.getValue();
          view.showMeetingStatus(
              meeting.description(), meeting.active() ? "End meeting" : "Start meeting");
          view.finishRegistration("");
        });
    task.setOnFailed(
        event -> {
          meeting = null;
          view.showMeetingStatus("Meeting status unavailable", "Refresh meeting status");
          view.setBusy(false);
          view.setStatus(
              failureMessage(task.getException(), "Could not update the meeting."), true);
        });
    start(task);
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
            LocalDateTime meetingStart = meetings.status().requireActive();
            UserActivity.AttendanceStatus status = users.attendanceStatus(id);
            if (status == UserActivity.AttendanceStatus.UNREGISTERED) {
              return null;
            }
            if (!LocalDbActivity.kHandsFreeMode
                && !new AlertUtils().confirmInput("Confirm attendance for member ID " + id + ".")) {
              return "";
            }
            if (!meetingStart.equals(meetings.status().requireActive())) {
              throw new IllegalStateException("The meeting changed. Enter your ID again.");
            }
            if (status == UserActivity.AttendanceStatus.CHECKED_IN) {
              return users.logoutUser(id, meetingStart);
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
            meetings.status().requireActive();
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
