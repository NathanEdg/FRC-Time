package activities;

import databases.DatabaseUtils;
import databases.SheetStore;
import helpers.AttendanceTime;
import helpers.Constants;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UserActivity {
  private final DatabaseUtils database;
  private final LoginActivity login;
  private final LogoutActivity logout;
  private static final DateTimeFormatter TIME = DateTimeFormatter.ISO_DATE_TIME;

  public UserActivity() {
    this(new DatabaseUtils());
  }

  public UserActivity(DatabaseUtils database) {
    this.database = database;
    login = new LoginActivity(database);
    logout = new LogoutActivity(database);
  }

  public MeetingActivity meetings() {
    return new MeetingActivity(database);
  }

  public enum AttendanceStatus {
    UNREGISTERED,
    CHECKED_OUT,
    CHECKED_IN
  }

  public AttendanceStatus attendanceStatus(String id) {
    database.getUpdatedData();
    int row = database.getCellRowFromColumn(id, Constants.kStudentIdColumn, Constants.kMainSheet);
    if (row < 1) {
      return AttendanceStatus.UNREGISTERED;
    }
    return "TRUE"
            .equalsIgnoreCase(
                database.getCellData(row, Constants.kLoggedInColumn, Constants.kMainSheet).trim())
        ? AttendanceStatus.CHECKED_IN
        : AttendanceStatus.CHECKED_OUT;
  }

  public void createNewUser(MemberRegistration registration, String id) {
    createNewUser(registration, id, false);
    database.getUpdatedData();
  }

  public void registerAndCheckIn(MemberRegistration registration, String id) {
    database.getUpdatedData();
    createNewUser(registration, id, true);
  }

  private void createNewUser(MemberRegistration registration, String id, boolean checkIn) {
    if (!isValidID(id)) {
      throw new IllegalArgumentException("Enter a valid member ID.");
    }
    if (database.getCellRowFromColumn(id, Constants.kStudentIdColumn, Constants.kMainSheet) >= 1) {
      throw new IllegalArgumentException("That member ID is already registered.");
    }
    List<SheetStore.Cell> writes = new ArrayList<>();
    int row = database.nextEmptyCellColumn(Constants.kMainSheet);
    writes.add(
        new SheetStore.Cell(
            Constants.kMainSheet, row, Constants.kEmailColumn, registration.email()));
    writes.add(
        new SheetStore.Cell(Constants.kMainSheet, row, Constants.kTotalHoursColumn, "00:00:00"));
    writes.add(new SheetStore.Cell(Constants.kMainSheet, row, Constants.kHoursColumn, "00:00:00"));
    writes.addAll(
        checkIn
            ? LoginActivity.checkInCells(row, LocalDateTime.now().format(TIME))
            : List.of(
                new SheetStore.Cell(
                    Constants.kMainSheet, row, Constants.kLoggedInColumn, "FALSE")));
    addIdentity(writes, Constants.kMainSheet, row, registration, id);
    int logRow = database.getCellRowFromColumn(id, Constants.kStudentIdColumn, Constants.kLogSheet);
    if (logRow < 1) {
      addIdentity(
          writes,
          Constants.kLogSheet,
          database.nextEmptyCellColumn(Constants.kLogSheet),
          registration,
          id);
    }
    database.write(writes);
  }

  private static void addIdentity(
      List<SheetStore.Cell> writes, int page, int row, MemberRegistration member, String id) {
    writes.add(new SheetStore.Cell(page, row, 0, id));
    writes.add(new SheetStore.Cell(page, row, 1, member.firstName()));
    writes.add(new SheetStore.Cell(page, row, 2, member.lastName()));
  }

  public void loginUser(String id) {
    int row = memberRow(id);
    login.loginUser(row, LocalDateTime.now().format(TIME));
  }

  public String logoutUser(String id) {
    return logoutUser(id, null);
  }

  public String logoutUser(String id, LocalDateTime meetingStart) {
    int row = memberRow(id);
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime started;
    try {
      started =
          LocalDateTime.parse(
              database.getCellData(row, Constants.kLastLoginColumn, Constants.kMainSheet), TIME);
    } catch (RuntimeException e) {
      throw new IllegalStateException(
          "Your check-in time is invalid. Ask a team lead to check the sheet.");
    }
    // Preserve the original forgotten-check-out policy across meeting days.
    if (now.isBefore(started)
        || (meetingStart == null
            ? !started.toLocalDate().equals(now.toLocalDate())
            : started.isBefore(meetingStart))) {
      database.write(
          List.of(
              new SheetStore.Cell(Constants.kMainSheet, row, Constants.kLoggedInColumn, "FALSE"),
              new SheetStore.Cell(
                  Constants.kMainSheet, row, Constants.kLastLogoutColumn, now.format(TIME))));
      return "Your previous meeting was still open. Check in again to start today's time.";
    }
    Duration session = Duration.between(started, now);
    if (meetingStart == null) {
      logout.logoutUserWithHours(id, row, session, now.format(TIME));
    } else {
      logout.saveCheckout(id, row, session, now.format(TIME), meetingStart.toLocalDate());
    }
    return "Checked out. " + AttendanceTime.format(session) + " added to your meeting hours.";
  }

  private int memberRow(String id) {
    int row = database.getCellRowFromColumn(id, Constants.kStudentIdColumn, Constants.kMainSheet);
    if (row < 1) {
      throw new IllegalArgumentException("That member ID is not registered.");
    }
    return row;
  }

  public boolean isValidID(String id) {
    return LocalDbActivity.isValidMemberId(id);
  }
}
