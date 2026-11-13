package databases;

import java.util.List;
import java.util.Locale;

/** Maps logical attendance columns without rewriting an existing sheet. */
public final class CurrentSheetLayout {
  public static final List<Object> HEADERS =
      List.of(
          "Member ID",
          "First Name",
          "Last Name",
          "Email",
          "Last Login",
          "Last Logout",
          "Hours",
          "Total Hours",
          "Logged In");
  private final int attendanceOffset;

  private CurrentSheetLayout(int attendanceOffset) {
    this.attendanceOffset = attendanceOffset;
  }

  public static CurrentSheetLayout fromHeaders(List<Object> headers) {
    for (int offset : new int[] {0, 2}) {
      boolean matches = true;
      for (int column = 0; column < HEADERS.size(); column++) {
        int physical = column < 4 ? column : column + offset;
        if (physical >= headers.size()
            || !matchesHeader(column, String.valueOf(headers.get(physical)))) {
          matches = false;
          break;
        }
      }
      if (matches) {
        return new CurrentSheetLayout(offset);
      }
    }
    throw new IllegalArgumentException(
        "The Current sheet has unrecognized headers. Check the sheet setup in README.md "
            + "before retrying; no attendance data has been changed.");
  }

  private static boolean matchesHeader(int column, String header) {
    String value = normalize(header);
    if (column == 0) {
      return List.of("memberid", "studentid", "id").contains(value);
    }
    if (column == 6) {
      return List.of("hours", "sessionhours", "lasthours").contains(value);
    }
    return value.equals(normalize(String.valueOf(HEADERS.get(column))));
  }

  public static void validateLogHeaders(List<Object> headers) {
    for (int column = 0; column < 3; column++) {
      if (column >= headers.size() || !matchesHeader(column, String.valueOf(headers.get(column)))) {
        throw new IllegalArgumentException(
            "The Date Log sheet has unrecognized headers. Check the sheet setup before retrying.");
      }
    }
  }

  private static String normalize(String value) {
    return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
  }

  public int physicalColumn(int logicalColumn) {
    if (logicalColumn < 0 || logicalColumn >= HEADERS.size()) {
      throw new IllegalArgumentException("Unknown attendance column.");
    }
    return logicalColumn < 4 ? logicalColumn : logicalColumn + attendanceOffset;
  }
}
