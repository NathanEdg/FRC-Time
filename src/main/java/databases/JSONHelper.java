package databases;

import exceptions.JsonKeyHasNoDataException;
import helpers.CommonUtils;
import helpers.Constants;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.json.JSONObject;

/** Runtime configuration stays outside the source tree and is updated atomically. */
public class JSONHelper {
  private final Path config;

  public JSONHelper() {
    this(CommonUtils.getDataDir().resolve(Constants.kConfigName));
  }

  public JSONHelper(Path config) {
    this.config = config.toAbsolutePath();
  }

  public String getKey(String key) throws JsonKeyHasNoDataException, FileNotFoundException {
    String result = new JSONObject(CommonUtils.readFile(config.toString())).getString(key);
    if (result.isEmpty()) {
      throw new JsonKeyHasNoDataException("A required configuration value is empty.");
    }
    return result;
  }

  public String optionalString(String key) throws IOException {
    return new JSONObject(Files.readString(config)).optString(key);
  }

  public void editSpreadsheetId(String sheetId) throws IOException {
    if (sheetId == null || !sheetId.matches("[A-Za-z0-9_-]+")) {
      throw new IllegalArgumentException("Invalid spreadsheet ID.");
    }
    JSONObject data = new JSONObject(Files.readString(config));
    if (sheetId.equals(data.optString("sheet"))) {
      return;
    }
    data.put("sheet", sheetId);
    Path temporary = Files.createTempFile(config.getParent(), ".config-", ".tmp");
    try {
      Files.writeString(temporary, data.toString(2) + "\n");
      try {
        Files.move(
            temporary, config, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temporary, config, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  public void copyTemplateJSON() throws IOException {
    Files.createDirectories(config.getParent());
    if (Files.exists(config)) {
      return;
    }
    try (InputStream template =
        getClass().getClassLoader().getResourceAsStream("templates/config.json")) {
      if (template == null) {
        throw new IOException("Configuration template is missing.");
      }
      Files.copy(template, config);
    }
  }
}
