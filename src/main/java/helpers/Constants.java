package helpers;

public final class Constants {
  private Constants() {}

  // Logical columns in the current, minimal attendance schema.
  public static final int kStudentIdColumn = 0;
  public static final int kFirstNameColumn = 1;
  public static final int kLastNameColumn = 2;
  public static final int kEmailColumn = 3;
  public static final int kLastLoginColumn = 4;
  public static final int kLastLogoutColumn = 5;
  public static final int kHoursColumn = 6;
  public static final int kTotalHoursColumn = 7;
  public static final int kLoggedInColumn = 8;

  public static final int kMainSheet = 0;
  public static final int kLogSheet = 1;
  public static final String kConfigName = "config.json";
  public static final String kRootStylesheet = "styles/root.css";
  public static final String kApplicationIcon = "images/icon.png";
  public static final String kTeamLogo = "images/spike.png";
  public static final String kApplicationName = "Team 293 Time";
  public static final String kTeamName = "FRC Team 293 · SPIKE";
  public static final String kVersion = "3.0.0";
  public static final int kMainStageWidth = 840;
  public static final int kMainStageHeight = 760;
  public static final String kCreditsSummary =
      "Attendance and meeting hours for FRC Team 293, SPIKE.\n\n"
          + "Based on GrizzlyTime by Dalton Smith and FRC Team 66, Grizzly Robotics. "
          + "The original software and this adaptation are licensed under MIT. "
          + "Team 293's logo remains the property of its owners.";
  public static final String kCreditsList =
      "Built with JavaFX, Google API Client, Google Sheets API, and org.json.";
  public static final int kIdDoesNotExist = -1;
  public static final int kIdLoggedIn = 0;
  public static final int kIdNotLoggedIn = 1;
  public static final int kMainSceneState = 1;
  public static final int kCreditsSceneState = 2;
}
