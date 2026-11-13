import java.util.Arrays;
import javafx.application.Application;

/** Separate launcher allows JavaFX to run from the distribution's classpath. */
public class Main {
  public static void main(String[] args) {
    Application.launch(
        Arrays.asList(args).contains("--preview") ? PreviewApplication.class : Team293Time.class,
        args);
  }
}
