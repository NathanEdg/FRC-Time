package activities;

import databases.DatabaseUtils;
import databases.SheetStore;
import helpers.Constants;
import java.util.List;

public class LoginActivity {
  private final DatabaseUtils database;

  public LoginActivity(DatabaseUtils database) {
    this.database = database;
  }

  public void loginUser(int row, String time) {
    database.write(
        List.of(
            new SheetStore.Cell(Constants.kMainSheet, row, Constants.kLastLoginColumn, time),
            new SheetStore.Cell(Constants.kMainSheet, row, Constants.kLoggedInColumn, "TRUE"),
            new SheetStore.Cell(
                Constants.kMainSheet, row, Constants.kLastLogoutColumn, "LOGGED IN")));
  }
}
