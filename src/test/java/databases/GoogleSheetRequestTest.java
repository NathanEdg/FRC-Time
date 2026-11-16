package databases;

import static org.junit.jupiter.api.Assertions.*;

import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.api.services.sheets.v4.Sheets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class GoogleSheetRequestTest {
  private static class Transport extends MockHttpTransport {
    final List<JSONObject> mutations = new ArrayList<>();
    int columns = 26;
    boolean failValues;

    @Override
    public LowLevelHttpRequest buildRequest(String method, String url) {
      return new MockLowLevelHttpRequest(url) {
        @Override
        public LowLevelHttpResponse execute() throws IOException {
          String response;
          if ("GET".equals(method)) {
            response =
                "{\"sheets\":[{\"properties\":{\"sheetId\":0,\"title\":\"Current\","
                    + "\"gridProperties\":{\"rowCount\":2000,\"columnCount\":26}}},"
                    + "{\"properties\":{\"sheetId\":1,\"title\":\"Date Log\","
                    + "\"gridProperties\":{\"rowCount\":2000,\"columnCount\":"
                    + columns
                    + "}}},"
                    + "{\"properties\":{\"sheetId\":2,\"title\":\"Meetings\","
                    + "\"gridProperties\":{\"rowCount\":2000,\"columnCount\":26}}}]}";
          } else {
            JSONObject mutation = new JSONObject(getContentAsString());
            mutations.add(mutation);
            if (mutation.has("requests")) {
              for (Object item : mutation.getJSONArray("requests")) {
                JSONObject append = ((JSONObject) item).getJSONObject("appendDimension");
                if ("COLUMNS".equals(append.getString("dimension"))) {
                  columns += append.getInt("length");
                }
              }
            }
            if (failValues && mutation.has("data")) {
              return new MockLowLevelHttpResponse()
                  .setStatusCode(403)
                  .setContentType("application/json")
                  .setContent(
                      "{\"error\":{\"code\":403,\"message\":\"synthetic private details\"}}");
            }
            response = "{}";
          }
          return new MockLowLevelHttpResponse()
              .setStatusCode(200)
              .setContentType("application/json")
              .setContent(response);
        }
      };
    }
  }

  private DatabaseProcess database(Transport transport) throws IOException {
    Sheets client =
        new Sheets.Builder(
                transport,
                GsonFactory.getDefaultInstance(),
                request -> request.setNumberOfRetries(0))
            .setApplicationName("test")
            .build();
    return new DatabaseProcess(client, "synthetic-sheet");
  }

  @Test
  void realGoogleClientBuildsCrossPageA1BatchAndOnlyAppendsGrid() throws Exception {
    Transport transport = new Transport();
    DatabaseProcess database = database(transport);
    database.write(
        List.of(
            new SheetStore.Cell(0, 1500, 8, "FALSE"),
            new SheetStore.Cell(1, 1500, 26, "01:00:00")));
    assertEquals(2, transport.mutations.size());
    JSONObject growth = transport.mutations.get(0);
    JSONObject append =
        growth.getJSONArray("requests").getJSONObject(0).getJSONObject("appendDimension");
    assertEquals("COLUMNS", append.getString("dimension"));
    assertEquals(1, append.getInt("length"));
    assertFalse(growth.toString().contains("updateSheetProperties"));
    JSONObject batch = transport.mutations.get(1);
    assertEquals("RAW", batch.getString("valueInputOption"));
    assertEquals("'Current'!I1501", batch.getJSONArray("data").getJSONObject(0).getString("range"));
    assertEquals(
        "'Date Log'!AA1501", batch.getJSONArray("data").getJSONObject(1).getString("range"));
  }

  @Test
  void googleSaveErrorsPropagateWithoutProviderDetails() throws Exception {
    Transport transport = new Transport();
    transport.failValues = true;
    DatabaseProcess database = database(transport);
    IllegalStateException failure =
        assertThrows(
            IllegalStateException.class,
            () -> database.write(List.of(new SheetStore.Cell(0, 1, 8, "FALSE"))));
    assertTrue(failure.getMessage().contains("could not be saved"));
    assertFalse(failure.getMessage().contains("synthetic private details"));
    assertNull(failure.getCause());
  }
}
