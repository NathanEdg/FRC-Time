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
    saveCheckout(id, row, session, logoutTime, LocalDate.now());
  }

  void saveCheckout(String id, int row, Duration session, String logoutTime, LocalDate date) {
    List<SheetStore.Cell> writes = new ArrayList<>();
    appendCheckout(
        writes,
        id,
        row,
        session,
        logoutTime,
        dateColumn(writes, date),
        database.nextEmptyCellColumn(Constants.kLogSheet));
    database.write(writes);
  }

  int dateColumn(List<SheetStore.Cell> writes, LocalDate meetingDate) {
    String date = meetingDate.format(DATE);
    List<String> dates = database.getRowData(1, Constants.kLogSheet);
    for (int i = 3; i < dates.size(); i++) {
      if (date.equals(dates.get(i).replace("'", ""))) {
        return i;
      }
    }
    int column = Math.max(3, database.logWidth());
    writes.add(new SheetStore.Cell(Constants.kLogSheet, 0, column, date));
    return column;
  }

  int appendCheckout(
      List<SheetStore.Cell> writes,
      String id,
      int row,
      Duration session,
      String logoutTime,
      int dateColumn,
      int nextLogRow) {
    int logRow = database.getCellRowFromColumn(id, Constants.kStudentIdColumn, Constants.kLogSheet);
    if (logRow < 1) {
      logRow = nextLogRow++;
      for (int column = 0; column < 3; column++) {
        writes.add(
            new SheetStore.Cell(
                Constants.kLogSheet,
                logRow,
                column,
                database.getCellData(row, column, Constants.kMainSheet)));
      }
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
    return nextLogRow;
  }
}
