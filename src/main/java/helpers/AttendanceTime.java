package helpers;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AttendanceTime {
  private static final Pattern HOURS = Pattern.compile("([0-9]+):([0-5][0-9]):([0-5][0-9])");

  private AttendanceTime() {}

  public static Duration parse(String input) {
    if (input == null || input.isBlank()) {
      return Duration.ZERO;
    }
    Matcher match = HOURS.matcher(input.trim());
    if (!match.matches()) {
      throw new IllegalArgumentException("Existing attendance time is invalid. Check the sheet.");
    }
    try {
      return Duration.ofHours(Long.parseLong(match.group(1)))
          .plusMinutes(Long.parseLong(match.group(2)))
          .plusSeconds(Long.parseLong(match.group(3)));
    } catch (ArithmeticException | NumberFormatException e) {
      throw new IllegalArgumentException("Existing attendance time is too large.");
    }
  }

  public static String format(Duration duration) {
    if (duration.isNegative()) {
      throw new IllegalArgumentException("Attendance duration cannot be negative.");
    }
    long seconds = duration.getSeconds();
    return String.format(
        java.util.Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
  }
}
