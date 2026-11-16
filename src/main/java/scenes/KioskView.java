package scenes;

import activities.LocalDbActivity;
import activities.MemberRegistration;
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
  private final Button confirm = new Button("Check in or out");
  private final Label status = new Label();
  private final Label connection = new Label();
  private final Hyperlink about = new Hyperlink("About");
  private final Hyperlink fullscreen = new Hyperlink("Full screen");
  private final Timeline clock;
  private final StackPane center;
  private final VBox work;

  public KioskView(boolean preview) {
    setId("kiosk");
    setFillWidth(true);

    ImageView mark = logo(44);
    Label appName = new Label(Constants.kApplicationName);
    appName.getStyleClass().add("app-name");
    Label team = new Label(Constants.kTeamName);
    team.getStyleClass().add("muted");
    VBox identity = new VBox(3, appName, team);
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

    Region headerSpacer = new Region();
    HBox.setHgrow(headerSpacer, Priority.ALWAYS);
    HBox header = new HBox(12, mark, identity, headerSpacer, date);
    header.setAlignment(Pos.CENTER_LEFT);
    header.getStyleClass().add("app-header");

    Label heading = new Label("Meeting attendance");
    heading.getStyleClass().add("heading");
    heading.setWrapText(true);
    heading.setMaxWidth(Double.MAX_VALUE);
    Label instruction = new Label("Use your member ID to check in or out.");
    instruction.getStyleClass().add("instruction");
    instruction.setWrapText(true);
    instruction.setMaxWidth(Double.MAX_VALUE);
    VBox introduction = new VBox(8, heading, instruction);

    Label idLabel = new Label("Member ID");
    idLabel.setLabelFor(memberId);
    idLabel.getStyleClass().add("field-label");
    memberId.setId("memberId");
    memberId.setPromptText(memberIdFormat());
    memberId.setAccessibleText("Member ID");
    memberId.setTextFormatter(
        new TextFormatter<String>(
            change -> change.getControlNewText().matches("[0-9]{0,18}") ? change : null));
    memberId.setMaxWidth(Double.MAX_VALUE);
    memberId.setMinHeight(60);
    confirm.setId("checkIn");
    confirm.setDefaultButton(true);
    confirm.setMaxWidth(Double.MAX_VALUE);
    confirm.setMinHeight(54);
    VBox form = new VBox(10, idLabel, memberId, confirm);
    form.getStyleClass().add("entry-form");

    status.setId("status");
    status.getStyleClass().add("status");
    status.setWrapText(true);
    status.setMaxWidth(Double.MAX_VALUE);
    status.setMinHeight(48);
    status.setAccessibleRole(javafx.scene.AccessibleRole.TEXT);

    Label firstTime = new Label("First time here? Enter your ID to register.");
    firstTime.getStyleClass().add("first-time");
    firstTime.setWrapText(true);
    firstTime.setMaxWidth(Double.MAX_VALUE);

    VBox feedback = new VBox(8, status, firstTime);
    work = new VBox(24, introduction, form, feedback);
    work.setAlignment(Pos.CENTER_LEFT);
    work.setMaxWidth(480);
    center = new StackPane(work);
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
              date.setVisible(!compact);
              date.setManaged(!compact);
              if (compact && !getStyleClass().contains("compact")) {
                getStyleClass().add("compact");
              } else if (!compact) {
                getStyleClass().remove("compact");
              }
            });
  }

  public static String memberIdFormat() {
    if (LocalDbActivity.usesDefaultIdLengths()) {
      return "6–8 digits";
    }
    return LocalDbActivity.kIdLength == LocalDbActivity.kIdLengthFallback
        ? LocalDbActivity.kIdLength + " digits"
        : LocalDbActivity.kIdLength + " or " + LocalDbActivity.kIdLengthFallback + " digits";
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

  public void showRegistration(String id, Consumer<MemberRegistration> submit, Runnable cancel) {
    about.setDisable(true);
    center.getChildren().setAll(new RegistrationView(id, submit, cancel));
  }

  void setRegistrationBusy() {
    ((RegistrationView) center.getChildren().get(0)).setBusy(true);
  }

  void showRegistrationError(String message) {
    ((RegistrationView) center.getChildren().get(0)).showError(message);
  }

  public void finishRegistration(String message) {
    center.getChildren().setAll(work);
    setBusy(false);
    setStatus(message, false);
    clearAndFocus();
  }

  public void setBusy(boolean busy) {
    about.setDisable(busy);
    memberId.setDisable(busy);
    confirm.setDisable(busy);
    confirm.setText(busy ? "Saving…" : "Check in or out");
    status.getStyleClass().remove("saving");
    if (busy) {
      setStatus("Saving attendance…", false);
      status.getStyleClass().add("saving");
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
