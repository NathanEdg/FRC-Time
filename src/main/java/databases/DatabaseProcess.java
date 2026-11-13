package databases;

import activities.LocalDbActivity;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import helpers.CommonUtils;
import helpers.Constants;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;

public class DatabaseProcess implements SheetStore {
  private final Sheets service;
  private final String spreadsheet;
  private final Map<Integer, SheetProperties> sheets = new HashMap<>();

  public DatabaseProcess() {
    spreadsheet = LocalDbActivity.kSheetId;
    try {
      NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
      service =
          new Sheets.Builder(transport, GsonFactory.getDefaultInstance(), credentials(transport))
              .setApplicationName(Constants.kApplicationName)
              .setHttpRequestInitializer(
                  request -> {
                    credentialsForRequest.initialize(request);
                    request.setConnectTimeout(15000);
                    request.setReadTimeout(30000);
                  })
              .build();
      initializeSheets();
    } catch (IOException | GeneralSecurityException e) {
      throw new IllegalStateException(
          "Could not connect to Google Sheets. Check credentials, sheet permissions, and internet.");
    }
  }

  private Credential credentialsForRequest;

  DatabaseProcess(Sheets service, String spreadsheet) throws IOException {
    this.service = service;
    this.spreadsheet = spreadsheet;
    initializeSheets();
  }

  private Credential credentials(NetHttpTransport transport) throws IOException {
    String override = System.getenv("TEAM293_TIME_CREDENTIALS");
    Path path =
        override == null || override.isBlank()
            ? CommonUtils.getDataDir().resolve("credentials.json")
            : Path.of(override);
    if (!Files.isRegularFile(path)) {
      throw new IOException("OAuth credentials are missing.");
    }
    GoogleClientSecrets secrets;
    try (Reader input = Files.newBufferedReader(path)) {
      secrets = GoogleClientSecrets.load(GsonFactory.getDefaultInstance(), input);
    }
    GoogleAuthorizationCodeFlow flow =
        new GoogleAuthorizationCodeFlow.Builder(
                transport,
                GsonFactory.getDefaultInstance(),
                secrets,
                List.of(SheetsScopes.SPREADSHEETS))
            .setDataStoreFactory(
                new FileDataStoreFactory(CommonUtils.getDataDir().resolve("tokens").toFile()))
            .setAccessType("offline")
            .build();
    credentialsForRequest =
        new AuthorizationCodeInstalledApp(
                flow,
                new LocalServerReceiver.Builder().setPort(8888).build(),
                url ->
                    Platform.runLater(
                        () -> CommonUtils.application.getHostServices().showDocument(url)))
            .authorize("user");
    return credentialsForRequest;
  }

  private void initializeSheets() throws IOException {
    Spreadsheet metadata =
        service.spreadsheets().get(spreadsheet).setFields("sheets.properties").execute();
    List<Request> missing = new ArrayList<>();
    for (int page : new int[] {Constants.kMainSheet, Constants.kLogSheet}) {
      for (Sheet sheet : metadata.getSheets()) {
        if (pageName(page).equals(sheet.getProperties().getTitle())) {
          sheets.put(page, sheet.getProperties());
        }
      }
      if (!sheets.containsKey(page)) {
        missing.add(
            new Request()
                .setAddSheet(
                    new AddSheetRequest()
                        .setProperties(new SheetProperties().setTitle(pageName(page)))));
      }
    }
    if (!missing.isEmpty()) {
      service
          .spreadsheets()
          .batchUpdate(spreadsheet, new BatchUpdateSpreadsheetRequest().setRequests(missing))
          .execute();
      initializeSheets();
    }
  }

  @Override
  public List<List<Object>> read(int page) {
    try {
      List<List<Object>> values =
          service
              .spreadsheets()
              .values()
              .get(spreadsheet, "'" + pageName(page) + "'")
              .execute()
              .getValues();
      if (values == null || values.isEmpty()) {
        return List.of();
      }
      return values;
    } catch (IOException e) {
      throw new IllegalStateException(
          "Could not read attendance. Check your internet connection and sheet permissions, then retry.");
    }
  }

  @Override
  public void write(List<Cell> cells) {
    if (cells.isEmpty()) {
      return;
    }
    try {
      ensureCapacity(cells);
      List<ValueRange> data = new ArrayList<>();
      for (Cell cell : cells) {
        if (cell.row() < 0 || cell.column() < 0) {
          throw new IllegalArgumentException("Invalid sheet cell.");
        }
        data.add(
            new ValueRange()
                .setRange(
                    "'"
                        + pageName(cell.page())
                        + "'!"
                        + toAlphabetic(cell.column())
                        + (cell.row() + 1))
                .setValues(List.of(List.of(cell.value()))));
      }
      service
          .spreadsheets()
          .values()
          .batchUpdate(
              spreadsheet, new BatchUpdateValuesRequest().setValueInputOption("RAW").setData(data))
          .execute();
    } catch (IOException e) {
      throw new IllegalStateException(
          "Attendance could not be saved. Check the sheet before retrying to avoid a duplicate.");
    }
  }

  private void ensureCapacity(List<Cell> cells) throws IOException {
    initializeSheets();
    Map<Integer, int[]> required = new HashMap<>();
    for (Cell cell : cells) {
      int[] size = required.computeIfAbsent(cell.page(), ignored -> new int[] {0, 0});
      size[0] = Math.max(size[0], cell.row() + 1);
      size[1] = Math.max(size[1], cell.column() + 1);
    }
    List<Request> requests = new ArrayList<>();
    for (Map.Entry<Integer, int[]> entry : required.entrySet()) {
      SheetProperties sheet = sheets.get(entry.getKey());
      GridProperties grid = sheet.getGridProperties();
      int rows = Math.max(grid.getRowCount(), entry.getValue()[0]);
      int columns = Math.max(grid.getColumnCount(), entry.getValue()[1]);
      if (rows != grid.getRowCount() || columns != grid.getColumnCount()) {
        if (rows > grid.getRowCount()) {
          requests.add(
              new Request()
                  .setAppendDimension(
                      new AppendDimensionRequest()
                          .setSheetId(sheet.getSheetId())
                          .setDimension("ROWS")
                          .setLength(rows - grid.getRowCount())));
        }
        if (columns > grid.getColumnCount()) {
          requests.add(
              new Request()
                  .setAppendDimension(
                      new AppendDimensionRequest()
                          .setSheetId(sheet.getSheetId())
                          .setDimension("COLUMNS")
                          .setLength(columns - grid.getColumnCount())));
        }
      }
    }
    if (!requests.isEmpty()) {
      service
          .spreadsheets()
          .batchUpdate(spreadsheet, new BatchUpdateSpreadsheetRequest().setRequests(requests))
          .execute();
      initializeSheets();
    }
  }

  private static String pageName(int page) {
    return switch (page) {
      case Constants.kMainSheet -> "Current";
      case Constants.kLogSheet -> "Date Log";
      default -> throw new IllegalArgumentException("Unknown attendance worksheet.");
    };
  }

  public static String toAlphabetic(int column) {
    if (column < 0) {
      throw new IllegalArgumentException("Column must be nonnegative.");
    }
    int quotient = column / 26;
    char letter = (char) ('A' + column % 26);
    return quotient == 0 ? String.valueOf(letter) : toAlphabetic(quotient - 1) + letter;
  }
}
