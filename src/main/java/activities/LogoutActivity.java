package activities;

import databases.DatabaseUtils;
import databases.SheetStore;
import helpers.AttendanceTime;
import helpers.Constants;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LogoutActivity {
  private final DatabaseUtils database;
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy:MM:dd");

  public LogoutActivity(DatabaseUtils database) {
    this.database = database;
  }

  public void logoutUserWithHours(String id, int row, Duration session, String logoutTime) {
    int logRow = database.getCellRowFromColumn(id, Constants.kStudentIdColumn, Constants.kLogSheet);
    List<SheetStore.Cell> writes = new ArrayList<>();
    if (logRow < 1) {
      logRow = database.nextEmptyCellColumn(Constants.kLogSheet);
      for (int column = 0; column < 3; column++) {
        writes.add(
            new SheetStore.Cell(
                Constants.kLogSheet,
                logRow,
                column,
                database.getCellData(row, column, Constants.kMainSheet)));
      }
    }
    String today = LocalDate.now().format(DATE);
    List<String> dates = database.getRowData(1, Constants.kLogSheet);
    int dateColumn = -1;
    for (int i = 3; i < dates.size(); i++) {
      if (today.equals(dates.get(i).replace("'", ""))) {
        dateColumn = i;
        break;
      }
    }
    if (dateColumn < 0) {
      dateColumn = Math.max(3, database.logWidth());
      writes.add(new SheetStore.Cell(Constants.kLogSheet, 0, dateColumn, today));
    }
    Duration day =
        AttendanceTime.parse(database.getCellData(logRow, dateColumn, Constants.kLogSheet));
    Duration total =
        AttendanceTime.parse(
            database.getCellData(row, Constants.kTotalHoursColumn, Constants.kMainSheet));
    writes.add(
        new SheetStore.Cell(
            Constants.kLogSheet, logRow, dateColumn, AttendanceTime.format(day.plus(session))));
    writes.add(
        new SheetStore.Cell(
            Constants.kMainSheet, row, Constants.kHoursColumn, AttendanceTime.format(session)));
    writes.add(
        new SheetStore.Cell(
            Constants.kMainSheet,
            row,
            Constants.kTotalHoursColumn,
            AttendanceTime.format(total.plus(session))));
    writes.add(
        new SheetStore.Cell(Constants.kMainSheet, row, Constants.kLastLogoutColumn, logoutTime));
    writes.add(new SheetStore.Cell(Constants.kMainSheet, row, Constants.kLoggedInColumn, "FALSE"));
    // Save time and check-out state together; report failure if any cell cannot be written.
    database.write(writes);
  }
}
