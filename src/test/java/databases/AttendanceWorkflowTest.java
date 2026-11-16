package databases;

import static org.junit.jupiter.api.Assertions.*;

import activities.LoginActivity;
import activities.LogoutActivity;
import activities.MemberRegistration;
import activities.UserActivity;
import helpers.Constants;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AttendanceWorkflowTest {
  @Test
  void initializesOnlyMinimalHeadersAndIsIdempotent() {
    InMemorySheetStore store = new InMemorySheetStore();
    DatabaseUtils database = new DatabaseUtils(store);
    assertEquals(CurrentSheetLayout.HEADERS, store.read(0).get(0));
    assertEquals(CurrentSheetLayout.HEADERS.subList(0, 3), store.read(1).get(0));
    database.getUpdatedData();
    assertEquals(1, store.writes.size());
  }

  @Test
  void invalidCurrentHeadersNeverWriteAnything() {
    InMemorySheetStore store = new InMemorySheetStore();
    store.sheets.put(0, new ArrayList<>(List.of(new ArrayList<>(List.of("unknown")))));
    assertThrows(IllegalArgumentException.class, () -> new DatabaseUtils(store));
    assertTrue(store.writes.isEmpty());
  }

  @Test
  void invalidDateLogNeverWritesMissingCurrentHeaders() {
    InMemorySheetStore store = new InMemorySheetStore();
    store.sheets.put(1, new ArrayList<>(List.of(new ArrayList<>(List.of("unrelated data")))));
    assertThrows(IllegalArgumentException.class, () -> new DatabaseUtils(store));
    assertTrue(store.writes.isEmpty());
  }

  @Test
  void registrationStoresOnlyNameEmailAndAttendanceFields() {
    InMemorySheetStore store = new InMemorySheetStore();
    DatabaseUtils database = new DatabaseUtils(store);
    new UserActivity(database)
        .createNewUser(new MemberRegistration(" Test ", " Member ", "test@example.com"), "123456");
    assertEquals(
        List.of(
            "123456",
            "Test",
            "Member",
            "test@example.com",
            "",
            "",
            "00:00:00",
            "00:00:00",
            "FALSE"),
        store.read(0).get(1));
    assertEquals(List.of("123456", "Test", "Member"), store.read(1).get(1));
    assertEquals(2, store.writes.size());
  }

  @Test
  void onboardingCreatesTheMemberAndChecksInWithOneWrite() {
    InMemorySheetStore store = new InMemorySheetStore();
    UserActivity users = new UserActivity(new DatabaseUtils(store));
    MemberRegistration member = new MemberRegistration("Test", "Member", "test@example.com");
    assertEquals(UserActivity.AttendanceStatus.UNREGISTERED, users.attendanceStatus("001234"));
    int writesBefore = store.writes.size();
    users.registerAndCheckIn(member, "001234");
    assertEquals(writesBefore + 1, store.writes.size());
    assertEquals(UserActivity.AttendanceStatus.CHECKED_IN, users.attendanceStatus("001234"));
    assertEquals(List.of("001234", "Test", "Member"), store.read(1).get(1));
    assertThrows(IllegalArgumentException.class, () -> users.registerAndCheckIn(member, "001234"));
    assertEquals(writesBefore + 1, store.writes.size());
  }

  @Test
  void legacySheetKeepsHistoricalColumnsAndMapsLoginAndLogout() {
    InMemorySheetStore store = new InMemorySheetStore();
    List<Object> headers = new ArrayList<>(CurrentSheetLayout.HEADERS.subList(0, 4));
    headers.addAll(List.of("Historical A", "Historical B"));
    headers.addAll(CurrentSheetLayout.HEADERS.subList(4, 9));
    headers.add("Historical C");
    List<Object> member =
        new ArrayList<>(
            List.of(
                "123456",
                "Test",
                "Member",
                "test@example.com",
                "keep-a",
                "keep-b",
                "",
                "",
                "00:00:00",
                "59:59:59",
                "FALSE",
                "keep-c"));
    store.sheets.put(0, new ArrayList<>(List.of(headers, member)));
    DatabaseUtils database = new DatabaseUtils(store);
    new LoginActivity(database).loginUser(1, "2026-11-13T17:00:00");
    assertEquals("TRUE", store.read(0).get(1).get(10));
    assertEquals("2026-11-13T17:00:00", store.read(0).get(1).get(6));
    database.getUpdatedData();
    new LogoutActivity(database)
        .logoutUserWithHours("123456", 1, Duration.ofSeconds(1), "2026-11-13T17:00:01");
    List<Object> saved = store.read(0).get(1);
    assertEquals("60:00:00", saved.get(9));
    assertEquals("FALSE", saved.get(10));
    assertEquals("keep-a", saved.get(4));
    assertEquals("keep-b", saved.get(5));
    assertEquals("keep-c", saved.get(11));
    assertTrue(
        store.writes.stream()
            .flatMap(List::stream)
            .filter(cell -> cell.page() == 0)
            .noneMatch(cell -> cell.column() == 4 || cell.column() == 5 || cell.column() == 11));
  }

  @Test
  void checkoutAccumulatesExactMinutesAndHoursInOneBatch() {
    InMemorySheetStore store = new InMemorySheetStore();
    DatabaseUtils database = new DatabaseUtils(store);
    new UserActivity(database)
        .createNewUser(new MemberRegistration("Test", "Member", "test@example.com"), "123456");
    new LoginActivity(database).loginUser(1, "2026-11-13T17:00:00");
    database.getUpdatedData();
    int previous = store.writes.size();
    new LogoutActivity(database)
        .logoutUserWithHours("123456", 1, Duration.ofHours(1), "2026-11-13T18:00:00");
    assertEquals(previous + 1, store.writes.size());
    assertEquals("01:00:00", store.read(0).get(1).get(Constants.kTotalHoursColumn));
    assertEquals("FALSE", store.read(0).get(1).get(Constants.kLoggedInColumn));
    assertEquals(
        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd")),
        store.read(1).get(0).get(3));
    assertEquals("01:00:00", store.read(1).get(1).get(3));
  }

  @Test
  void failedCheckoutPreservesStateAndPropagatesFailure() {
    InMemorySheetStore store = new InMemorySheetStore();
    DatabaseUtils database = new DatabaseUtils(store);
    new UserActivity(database)
        .createNewUser(new MemberRegistration("Test", "Member", "test@example.com"), "123456");
    new LoginActivity(database).loginUser(1, "2026-11-13T17:00:00");
    database.getUpdatedData();
    List<List<Object>> beforeCurrent = store.read(0);
    List<List<Object>> beforeLog = store.read(1);
    store.failWrites = true;
    assertThrows(
        IllegalStateException.class,
        () ->
            new LogoutActivity(database)
                .logoutUserWithHours("123456", 1, Duration.ofHours(1), "2026-11-13T18:00:00"));
    assertEquals(beforeCurrent, store.read(0));
    assertEquals(beforeLog, store.read(1));
  }

  @Test
  void duplicateRegistrationIsRejectedWithoutWrites() {
    InMemorySheetStore store = new InMemorySheetStore();
    UserActivity users = new UserActivity(new DatabaseUtils(store));
    MemberRegistration member = new MemberRegistration("Test", "Member", "test@example.com");
    users.createNewUser(member, "123456");
    int writes = store.writes.size();
    assertThrows(IllegalArgumentException.class, () -> users.createNewUser(member, "123456"));
    assertEquals(writes, store.writes.size());
  }

  @Test
  void registrationAppendsWithoutReusingOrphanedRows() {
    InMemorySheetStore store = new InMemorySheetStore();
    List<Object> currentOrphan =
        List.of(
            "", "Former", "Member", "former@example.com", "", "", "02:00:00", "10:00:00", "FALSE");
    List<Object> logOrphan = List.of("", "Former", "Member", "10:00:00");
    store.sheets.put(
        0,
        new ArrayList<>(
            List.of(new ArrayList<>(CurrentSheetLayout.HEADERS), new ArrayList<>(currentOrphan))));
    store.sheets.put(
        1,
        new ArrayList<>(
            List.of(
                new ArrayList<>(CurrentSheetLayout.HEADERS.subList(0, 3)),
                new ArrayList<>(logOrphan))));
    DatabaseUtils database = new DatabaseUtils(store);

    new UserActivity(database)
        .createNewUser(new MemberRegistration("Test", "Member", "test@example.com"), "123456");

    assertEquals(currentOrphan, store.read(0).get(1));
    assertEquals(logOrphan, store.read(1).get(1));
    assertEquals("123456", store.read(0).get(2).get(0));
    assertEquals("123456", store.read(1).get(2).get(0));
    assertEquals("00:00:00", store.read(0).get(2).get(Constants.kTotalHoursColumn));
  }

  @Test
  void checkoutRepairDoesNotInheritOrphanedDailyHours() {
    InMemorySheetStore store = new InMemorySheetStore();
    DatabaseUtils database = new DatabaseUtils(store);
    new UserActivity(database)
        .createNewUser(new MemberRegistration("Test", "Member", "test@example.com"), "123456");
    String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
    List<Object> logOrphan = List.of("", "Former", "Member", "10:00:00");
    List<Object> logHeaders = new ArrayList<>(CurrentSheetLayout.HEADERS.subList(0, 3));
    logHeaders.add(today);
    store.sheets.put(1, new ArrayList<>(List.of(logHeaders, new ArrayList<>(logOrphan))));
    database.getUpdatedData();

    new LogoutActivity(database)
        .logoutUserWithHours("123456", 1, Duration.ofHours(1), "2026-11-15T18:00:00");

    assertEquals(logOrphan, store.read(1).get(1));
    assertEquals(List.of("123456", "Test", "Member", "01:00:00"), store.read(1).get(2));
  }

  @Test
  void rowAllocationAppendsBeyondBlankHistoricalRows() {
    InMemorySheetStore store = new InMemorySheetStore();
    DatabaseUtils database = new DatabaseUtils(store);
    store.sheets.get(0).add(new ArrayList<>());
    store
        .sheets
        .get(0)
        .add(new ArrayList<>(List.of("", "", "", "", "", "", "", "", "", "", "", "historical")));
    database.getUpdatedData();
    assertEquals(3, database.nextEmptyCellColumn(Constants.kMainSheet));
  }

  @Test
  void checkoutAppendsDateAfterEveryOccupiedHistoricalColumn() {
    InMemorySheetStore store = new InMemorySheetStore();
    DatabaseUtils database = new DatabaseUtils(store);
    new UserActivity(database)
        .createNewUser(new MemberRegistration("Test", "Member", "test@example.com"), "123456");
    // The date header was cleared, but its historical hours were not.
    store.sheets.get(1).get(1).add("10:00:00");
    database.getUpdatedData();

    new LogoutActivity(database)
        .logoutUserWithHours("123456", 1, Duration.ofHours(1), "2026-11-15T18:00:00");

    assertEquals("10:00:00", store.read(1).get(1).get(3));
    assertEquals("", store.read(1).get(0).get(3));
    assertEquals(
        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd")),
        store.read(1).get(0).get(4));
    assertEquals("01:00:00", store.read(1).get(1).get(4));
    assertEquals("01:00:00", store.read(0).get(1).get(Constants.kTotalHoursColumn));
  }

  @Test
  void spreadsheetColumnEncodingWorksPastZ() {
    assertEquals("A", DatabaseProcess.toAlphabetic(0));
    assertEquals("Z", DatabaseProcess.toAlphabetic(25));
    assertEquals("AA", DatabaseProcess.toAlphabetic(26));
    assertEquals("ZZ", DatabaseProcess.toAlphabetic(701));
    assertEquals("AAA", DatabaseProcess.toAlphabetic(702));
  }
}
