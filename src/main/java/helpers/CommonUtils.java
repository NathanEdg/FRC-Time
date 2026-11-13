package helpers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.application.Application;

public final class CommonUtils {
  private CommonUtils() {}

  public static Application application;

  public static String getCurrentDir() {
    return System.getProperty("user.dir");
  }

  public static Path getDataDir() {
    String configured = System.getenv("TEAM293_TIME_DATA_DIR");
    return configured == null || configured.isBlank()
        ? Path.of(System.getProperty("user.home"), ".team293-time")
        : Path.of(configured).toAbsolutePath();
  }

  public static String readFile(String filePath) throws FileNotFoundException {
    try {
      return Files.readString(Path.of(filePath));
    } catch (IOException e) {
      throw new FileNotFoundException("Unable to read configuration.");
    }
  }

  public static void exitApplication() {
    System.exit(1);
  }
}
