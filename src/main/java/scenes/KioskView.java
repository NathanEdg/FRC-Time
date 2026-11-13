package scenes;

import activities.LocalDbActivity;
import helpers.Constants;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

public class KioskView extends VBox {
  private final TextField memberId = new TextField();
  private final Button confirm = new Button("Check in / out");
  private final Label status = new Label("Ready for your ID");
  private final Label connection = new Label();
  private final Hyperlink about = new Hyperlink("About");
  private final Hyperlink fullscreen = new Hyperlink("Full screen");
  private final Timeline clock;

  public KioskView(boolean preview) {
    setId("kiosk");
    setFillWidth(true);

    ImageView mark = logo(52);
    Label appName = new Label(Constants.kApplicationName);
    appName.getStyleClass().add("app-name");
    Label team = new Label(Constants.kTeamName);
    team.getStyleClass().add("muted");
    VBox identity = new VBox(3, appName, team);
    HBox header = new HBox(14, mark, identity);
    header.setAlignment(Pos.CENTER_LEFT);
    header.getStyleClass().add("app-header");

    Label date = new Label();
    date.getStyleClass().add("meeting-date");
    Runnable updateClock =
        () ->
            date.setText(
                LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("EEEE, MMMM d", java.util.Locale.US)));
    updateClock.run();
    clock = new Timeline(new KeyFrame(Duration.minutes(1), event -> updateClock.run()));
    clock.setCycleCount(Timeline.INDEFINITE);
    sceneProperty()
        .addListener(
            (observable, previous, scene) -> {
              if (scene == null) {
                clock.stop();
              } else {
                clock.play();
              }
            });

    ImageView teamLogo = logo(144);
    Label heading = new Label("Check in or check out");
    heading.getStyleClass().add("heading");
    heading.setWrapText(true);
    heading.setAlignment(Pos.CENTER);
    heading.setMaxWidth(Double.MAX_VALUE);
    Label instruction = new Label("Scan your badge or enter your member ID.");
    instruction.getStyleClass().add("instruction");
    instruction.setWrapText(true);
    instruction.setAlignment(Pos.CENTER);
    instruction.setMaxWidth(Double.MAX_VALUE);

    Label idLabel = new Label("Member ID");
    idLabel.setLabelFor(memberId);
    idLabel.getStyleClass().add("field-label");
    memberId.setId("memberId");
    memberId.setPromptText("Enter your ID");
    memberId.setAccessibleText("Member ID");
    memberId.setTextFormatter(
        new TextFormatter<String>(
            change -> change.getControlNewText().matches("[0-9]{0,18}") ? change : null));
    memberId.setMaxWidth(Double.MAX_VALUE);
    confirm.setId("checkIn");
    confirm.setDefaultButton(true);
    confirm.setMaxWidth(Double.MAX_VALUE);
    HBox entry = new HBox(12, memberId, confirm);
    HBox.setHgrow(memberId, Priority.ALWAYS);
    memberId.setPrefWidth(300);
    confirm.setMinWidth(148);
    VBox form = new VBox(8, idLabel, entry);
    form.getStyleClass().add("entry-form");

    status.setId("status");
    status.getStyleClass().add("status");
    status.setWrapText(true);
    status.setAlignment(Pos.CENTER);
    status.setMaxWidth(Double.MAX_VALUE);
    status.setMinHeight(72);
    status.setAccessibleRole(javafx.scene.AccessibleRole.TEXT);

    String lengths =
        LocalDbActivity.kIdLength == LocalDbActivity.kIdLengthFallback
            ? LocalDbActivity.kIdLength + "-digit"
            : LocalDbActivity.kIdLength + "- or " + LocalDbActivity.kIdLengthFallback + "-digit";
    Label firstTime = new Label("New to the team? Enter an unused " + lengths + " ID to register.");
    firstTime.getStyleClass().add("first-time");
    firstTime.setWrapText(true);
    firstTime.setAlignment(Pos.CENTER);
    firstTime.setMaxWidth(Double.MAX_VALUE);

    VBox work = new VBox(12, date, teamLogo, heading, instruction, form, status, firstTime);
    work.setAlignment(Pos.CENTER);
    work.setMaxWidth(560);
    StackPane center = new StackPane(work);
    center.getStyleClass().add("work-area");
    VBox.setVgrow(center, Priority.ALWAYS);

    connection.setText(preview ? "Preview · no data is saved" : "Connected to Google Sheets");
    connection.getStyleClass().add("connection");
    HBox links = new HBox(12, fullscreen, about);
    links.setAlignment(Pos.CENTER_RIGHT);
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    HBox footer = new HBox(8, connection, spacer, links);
    footer.setAlignment(Pos.CENTER_LEFT);
    footer.getStyleClass().add("app-footer");
    getChildren().addAll(header, center, footer);

    widthProperty()
        .addListener(
            (observable, previous, width) -> {
              boolean compact = width.doubleValue() < 600;
              teamLogo.setFitHeight(compact ? 104 : 144);
              if (compact && !getStyleClass().contains("compact")) {
                getStyleClass().add("compact");
              } else if (!compact) {
                getStyleClass().remove("compact");
              }
            });
  }

  public static ImageView logo(double height) {
    ImageView logo = new ImageView(new Image(Constants.kTeamLogo));
    logo.setFitHeight(height);
    logo.setPreserveRatio(true);
    logo.setAccessibleText("SPIKE Team 293 logo");
    return logo;
  }

  public void onSubmit(Consumer<String> action) {
    Runnable submit =
        () -> {
          if (!confirm.isDisabled()) {
            action.accept(memberId.getText());
          }
        };
    memberId.setOnAction(event -> submit.run());
    confirm.setOnAction(event -> submit.run());
  }

  public void onAbout(Runnable action) {
    about.setOnAction(event -> action.run());
  }

  public void onFullscreen(Runnable action) {
    fullscreen.setOnAction(event -> action.run());
  }

  public void setBusy(boolean busy) {
    memberId.setDisable(busy);
    confirm.setDisable(busy);
    confirm.setText(busy ? "Saving…" : "Check in / out");
    if (busy) {
      setStatus("Saving your attendance…", false);
    }
  }

  public void setStatus(String message, boolean error) {
    status.setText(message);
    status.getStyleClass().remove("error");
    if (error) {
      status.getStyleClass().add("error");
    }
  }

  public void clearAndFocus() {
    memberId.clear();
    Platform.runLater(memberId::requestFocus);
  }

  public void focusInput() {
    Platform.runLater(
        () -> {
          memberId.requestFocus();
          memberId.selectAll();
        });
  }
}
