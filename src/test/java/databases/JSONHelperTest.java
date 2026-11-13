package databases;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JSONHelperTest {
  @TempDir Path directory;

  @Test
  void firstRunTemplateDoesNotOverwriteExistingConfiguration() throws Exception {
    Path file = directory.resolve("config.json");
    JSONHelper helper = new JSONHelper(file);
    helper.copyTemplateJSON();
    JSONObject initial = new JSONObject(Files.readString(file));
    assertEquals("Team_293_Time", initial.getString("applicationName"));
    helper.editSpreadsheetId("example-ID");
    String configured = Files.readString(file);
    helper.copyTemplateJSON();
    assertEquals(configured, Files.readString(file));
    helper.editSpreadsheetId("example-ID");
    assertEquals(configured, Files.readString(file));
  }

  @Test
  void editingKeepsNamesSpacesAndUnrelatedSettings() throws Exception {
    Path file = directory.resolve("config.json");
    Files.writeString(
        file, "{\"sheet\":\"\",\"applicationName\":\"My Team Name\",\"custom\":\"preserve\"}");
    JSONHelper helper = new JSONHelper(file);
    helper.editSpreadsheetId("example-ID");
    assertEquals("My Team Name", helper.getKey("applicationName"));
    assertEquals("preserve", helper.getKey("custom"));
    assertEquals("example-ID", helper.getKey("sheet"));
    assertEquals(1, Files.list(directory).count());
  }

  @Test
  void malformedOrInvalidUpdatesLeaveOriginalFileIntact() throws Exception {
    Path file = directory.resolve("config.json");
    JSONHelper helper = new JSONHelper(file);
    Files.writeString(file, "invalid JSON");
    assertThrows(RuntimeException.class, () -> helper.editSpreadsheetId("example-ID"));
    assertEquals("invalid JSON", Files.readString(file));
    assertThrows(IllegalArgumentException.class, () -> helper.editSpreadsheetId("invalid/id"));
    assertEquals("invalid JSON", Files.readString(file));
  }
}
