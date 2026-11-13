package activities;

import databases.JSONHelper;
import exceptions.JsonKeyHasNoDataException;
import helpers.AlertUtils;
import helpers.Constants;
import java.io.IOException;
import org.json.JSONException;

public class LocalDbActivity {
  public static String kSheetId = "";
  public static boolean kHandsFreeMode = false;
  public static int kIdLength = 6;
  public static int kIdLengthFallback = 7;
  public static String kApplicationName = Constants.kApplicationName;

  public void updateLocalDb() throws IOException {
    JSONHelper configuration = new JSONHelper();
    configuration.copyTemplateJSON();
    try {
      try {
        kSheetId = configuration.getKey("sheet");
      } catch (JsonKeyHasNoDataException e) {
        String sheetId = new AlertUtils().showSpreadsheetDialog();
        if (sheetId.isEmpty()) {
          throw new IOException("Setup was cancelled. Restart the app to connect Google Sheets.");
        }
        configuration.editSpreadsheetId(sheetId);
        kSheetId = sheetId;
      }
      kHandsFreeMode = Boolean.parseBoolean(configuration.getKey("handsFreeMode"));
      kApplicationName = configuration.getKey("applicationName").replace('_', ' ');
      int length = Integer.parseInt(configuration.getKey("idLength"));
      int fallback = Integer.parseInt(configuration.getKey("idLengthFallback"));
      if (length < 1 || length > 18 || fallback < 1 || fallback > 18) {
        throw new IllegalArgumentException();
      }
      kIdLength = length;
      kIdLengthFallback = fallback;
    } catch (JsonKeyHasNoDataException | JSONException | IllegalArgumentException e) {
      throw new IOException("Configuration is invalid. Check config.json in the app data folder.");
    }
  }
}
