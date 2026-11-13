package helpers;

import java.net.URI;
import java.util.regex.Pattern;

public final class InputValidation {
  private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+$");
  private static final Pattern SHEET_ID = Pattern.compile("[A-Za-z0-9_-]+");

  private InputValidation() {}

  public static boolean isValidMember(String firstName, String lastName, String email) {
    return firstName != null
        && !firstName.isBlank()
        && lastName != null
        && !lastName.isBlank()
        && email != null
        && EMAIL.matcher(email.trim()).matches();
  }

  public static boolean isValidId(String id, int length, int fallbackLength) {
    return id != null
        && (id.length() == length || id.length() == fallbackLength)
        && id.matches("[0-9]+");
  }

  public static String spreadsheetId(String input) {
    try {
      URI uri = URI.create(input.trim());
      if (!"https".equalsIgnoreCase(uri.getScheme())
          || !"docs.google.com".equalsIgnoreCase(uri.getHost())
          || uri.getUserInfo() != null) {
        throw new IllegalArgumentException();
      }
      String[] parts = uri.getPath().split("/");
      if (parts.length < 4
          || !"spreadsheets".equals(parts[1])
          || !"d".equals(parts[2])
          || !SHEET_ID.matcher(parts[3]).matches()) {
        throw new IllegalArgumentException();
      }
      return parts[3];
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Enter a valid Google Sheets link.");
    }
  }
}
