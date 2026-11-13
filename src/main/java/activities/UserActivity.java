package activities;

import databases.DatabaseUtils;
import databases.SheetStore;
import exceptions.CancelledUserCreationException;
import helpers.AlertUtils;
import helpers.AttendanceTime;
import helpers.Constants;
import helpers.InputValidation;
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

  public boolean isUserLoggedIn(String id) throws CancelledUserCreationException {
    database.getUpdatedData();
    int row = database.getCellRowFromColumn(id, Constants.kStudentIdColumn, Constants.kMainSheet);
    if (row >= 1) {
      return "TRUE"
          .equalsIgnoreCase(
              database.getCellData(row, Constants.kLoggedInColumn, Constants.kMainSheet).trim());
    }
    MemberRegistration registration =
        new AlertUtils()
            .getUserInfo()
            .orElseThrow(() -> new CancelledUserCreationException("Registration cancelled."));
    createNewUser(registration, id);
    return false;
  }

  public void createNewUser(MemberRegistration registration, String id) {
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
    writes.add(new SheetStore.Cell(Constants.kMainSheet, row, Constants.kLoggedInColumn, "FALSE"));
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
    database.getUpdatedData();
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
    if (!started.toLocalDate().equals(now.toLocalDate()) || now.isBefore(started)) {
      database.write(
          List.of(
              new SheetStore.Cell(Constants.kMainSheet, row, Constants.kLoggedInColumn, "FALSE"),
              new SheetStore.Cell(
                  Constants.kMainSheet, row, Constants.kLastLogoutColumn, now.format(TIME))));
      return "Your previous meeting was still open. Check in again to start today's time.";
    }
    Duration session = Duration.between(started, now);
    logout.logoutUserWithHours(id, row, session, now.format(TIME));
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
    return InputValidation.isValidId(
        id, LocalDbActivity.kIdLength, LocalDbActivity.kIdLengthFallback);
  }
}
