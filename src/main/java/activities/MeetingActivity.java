package activities;

import databases.DatabaseUtils;
import databases.SheetStore;
import helpers.AttendanceTime;
import helpers.Constants;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public final class MeetingActivity {
  private static final List<String> HEADERS = List.of("Started", "Ended", "Duration");
  private final DatabaseUtils database;

  MeetingActivity(DatabaseUtils database) {
    this.database = database;
  }

  public record Status(LocalDateTime started, Duration total, int row) {
    public boolean active() {
      return started != null;
    }

    public String description() {
      return (active() ? "Meeting active" : "No active meeting")
          + " · "
          + AttendanceTime.format(total)
          + " total meeting time";
    }

    public LocalDateTime requireActive() {
      if (!active()) {
        throw new IllegalStateException("Check-in opens when a team lead starts the meeting.");
      }
      return started;
    }
  }

  public Status status() {
    List<List<Object>> meetings = database.readMeetings();
    if (!meetings.isEmpty() && !HEADERS.equals(meetings.get(0))) {
      throw new IllegalStateException("The Meetings sheet has unexpected headers.");
    }
    Duration total = Duration.ZERO;
    for (int row = 1; row < meetings.size(); row++) {
      List<Object> cells = meetings.get(row);
      LocalDateTime started = time(cells.isEmpty() ? "" : cells.get(0).toString());
      if (cells.size() < 2 || cells.get(1).toString().isBlank()) {
        if (row != meetings.size() - 1) {
          throw new IllegalStateException("An open meeting must be the last row in Meetings.");
        }
        return new Status(started, total, row);
      }
      LocalDateTime ended = time(cells.get(1).toString());
      if (ended.isBefore(started)) {
        throw new IllegalStateException(
            "A meeting ends before it starts. Check the Meetings sheet.");
      }
      total = total.plus(Duration.between(started, ended));
    }
    return new Status(null, total, meetings.size());
  }

  public Status start(LocalDateTime now) {
    Status current = status();
    if (current.active()) {
      throw new IllegalStateException("A meeting is already active.");
    }
    database.getUpdatedData();
    List<SheetStore.Cell> writes = new ArrayList<>();
    if (current.row() == 0) {
      for (int column = 0; column < HEADERS.size(); column++) {
        writes.add(meetingCell(0, column, HEADERS.get(column)));
      }
    }
    List<String> flags = database.getColumnData(Constants.kLoggedInColumn, Constants.kMainSheet);
    for (int row = 1; row < flags.size(); row++) {
      if (checkedIn(flags.get(row))) {
        writes.add(
            new SheetStore.Cell(Constants.kMainSheet, row, Constants.kLoggedInColumn, "FALSE"));
      }
    }
    int row = Math.max(1, current.row());
    writes.add(meetingCell(row, 0, now.toString()));
    database.write(writes);
    return new Status(now, current.total(), row);
  }

  public Status end(LocalDateTime now) {
    Status current = status();
    LocalDateTime started = current.requireActive();
    if (now.isBefore(started)) {
      throw new IllegalStateException("The clock is earlier than the meeting start.");
    }
    database.getUpdatedData();
    List<SheetStore.Cell> writes = new ArrayList<>();
    LogoutActivity checkout = new LogoutActivity(database);
    int dateColumn = -1;
    int nextLogRow = database.nextEmptyCellColumn(Constants.kLogSheet);
    List<String> ids = database.getColumnData(Constants.kStudentIdColumn, Constants.kMainSheet);
    List<String> flags = database.getColumnData(Constants.kLoggedInColumn, Constants.kMainSheet);
    for (int row = 1; row < ids.size(); row++) {
      String id = ids.get(row);
      if (id.isBlank() || !checkedIn(flags.get(row))) {
        continue;
      }
      LocalDateTime checkIn =
          time(database.getCellData(row, Constants.kLastLoginColumn, Constants.kMainSheet));
      if (checkIn.isBefore(started) || checkIn.isAfter(now)) {
        throw new IllegalStateException(
            "Check the check-in time for member " + id + " before ending the meeting.");
      }
      if (dateColumn < 0) {
        dateColumn = checkout.dateColumn(writes, started.toLocalDate());
      }
      nextLogRow =
          checkout.appendCheckout(
              writes,
              id,
              row,
              Duration.between(checkIn, now),
              now.toString(),
              dateColumn,
              nextLogRow);
    }
    Duration elapsed = Duration.between(started, now);
    writes.add(meetingCell(current.row(), 1, now.toString()));
    writes.add(meetingCell(current.row(), 2, AttendanceTime.format(elapsed)));
    database.write(writes);
    return new Status(null, current.total().plus(elapsed), current.row() + 1);
  }

  private static boolean checkedIn(String value) {
    return "TRUE".equalsIgnoreCase(value.trim());
  }

  private static SheetStore.Cell meetingCell(int row, int column, String value) {
    return new SheetStore.Cell(Constants.kMeetingsSheet, row, column, value);
  }

  private static LocalDateTime time(String value) {
    try {
      return LocalDateTime.parse(value);
    } catch (DateTimeParseException e) {
      throw new IllegalStateException(
          "A meeting or check-in timestamp is invalid. Check the sheet.");
    }
  }
}
