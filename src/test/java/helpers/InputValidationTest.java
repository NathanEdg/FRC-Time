package helpers;

import static org.junit.jupiter.api.Assertions.*;

import activities.MemberRegistration;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class InputValidationTest {
  @Test
  void namesAndEmailAreRequiredAndRecheckedAfterEdits() {
    assertTrue(InputValidation.isValidMember("Test", "Member", "member@example.technology"));
    assertFalse(InputValidation.isValidMember("  ", "Member", "member@example.com"));
    assertFalse(InputValidation.isValidMember("Test", "", "member@example.com"));
    assertFalse(InputValidation.isValidMember("Test", "Member", "invalid"));
    assertThrows(
        IllegalArgumentException.class, () -> new MemberRegistration("Test", "Member", "invalid"));
  }

  @Test
  void idsAreNumericAndPreserveLeadingZeroes() {
    assertTrue(InputValidation.isValidId("001234", 6, 7));
    assertTrue(InputValidation.isValidId("1234567", 6, 7));
    assertFalse(InputValidation.isValidId("-12345", 6, 7));
    assertFalse(InputValidation.isValidId("12345a", 6, 7));
    assertFalse(InputValidation.isValidId("12345", 6, 7));
  }

  @Test
  void acceptsOnlyGoogleSpreadsheetLinks() {
    assertEquals(
        "example_ID-123",
        InputValidation.spreadsheetId(
            "https://docs.google.com/spreadsheets/d/example_ID-123/edit#gid=0"));
    for (String invalid :
        new String[] {
          "",
          "http://docs.google.com/spreadsheets/d/abc",
          "https://example.com/spreadsheets/d/abc",
          "https://docs.google.com@example.com/spreadsheets/d/abc",
          "https://docs.google.com/spreadsheets/d/",
          "https://docs.google.com/d/abc"
        }) {
      assertThrows(IllegalArgumentException.class, () -> InputValidation.spreadsheetId(invalid));
    }
  }

  @Test
  void attendanceHandlesExactSixtyAndTotalsBeyondADay() {
    assertEquals(
        "60:00:00", AttendanceTime.format(AttendanceTime.parse("59:59:59").plusSeconds(1)));
    assertEquals("01:00:00", AttendanceTime.format(Duration.ofMinutes(60)));
    assertEquals(Duration.ZERO, AttendanceTime.parse(""));
    assertThrows(IllegalArgumentException.class, () -> AttendanceTime.parse("00:60:00"));
    assertThrows(
        IllegalArgumentException.class,
        () -> AttendanceTime.parse("99999999999999999999999999999:00:00"));
  }
}
