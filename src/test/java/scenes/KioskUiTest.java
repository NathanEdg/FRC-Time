package scenes;

import static org.junit.jupiter.api.Assertions.*;

import helpers.AlertUtils;
import helpers.Constants;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ui")
class KioskUiTest {
  @BeforeAll
  static void startup() throws Exception {
    FutureTask<Void> initialized =
        new FutureTask<>(
            () -> {
              Platform.setImplicitExit(false);
              return null;
            });
    Platform.startup(initialized);
    initialized.get(15, TimeUnit.SECONDS);
  }

  @AfterAll
  static void shutdown() {
    Platform.exit();
  }

  private static void fx(Runnable action) throws Exception {
    FutureTask<Void> task =
        new FutureTask<>(
            () -> {
              action.run();
              return null;
            });
    Platform.runLater(task);
    task.get(20, TimeUnit.SECONDS);
  }

  private static void capture(Node node, String filename) {
    try {
      Path directory = Path.of(System.getProperty("spike.review.dir"));
      Files.createDirectories(directory);
      node.applyCss();
      if (node instanceof javafx.scene.Parent parent) {
        parent.layout();
      }
      ImageIO.write(
          SwingFXUtils.fromFXImage(node.snapshot(null, null), null),
          "png",
          directory.resolve(filename).toFile());
    } catch (Exception e) {
      throw new AssertionError("Unable to capture the UI.", e);
    }
  }

  private static void assertFits(KioskView view, Node... nodes) {
    for (Node node : nodes) {
      Bounds bounds = view.sceneToLocal(node.localToScene(node.getBoundsInLocal()));
      assertTrue(bounds.getMinX() >= 0 && bounds.getMaxX() <= view.getWidth() + 1);
      assertTrue(bounds.getMinY() >= 0 && bounds.getMaxY() <= view.getHeight() + 1);
    }
  }

  @Test
  void nativeKioskSupportsKeyboardEntryBusyErrorAndWindowSizes() throws Exception {
    fx(
        () -> {
          KioskView view = new KioskView(true);
          Stage stage = new Stage();
          Scene scene = new Scene(view, 840, 760);
          scene
              .getStylesheets()
              .add(KioskUiTest.class.getResource("/" + Constants.kRootStylesheet).toExternalForm());
          stage.setScene(scene);
          stage.show();
          view.applyCss();
          view.layout();
          TextField id = (TextField) view.lookup("#memberId");
          Button submit = (Button) view.lookup("#checkIn");
          AtomicReference<String> entered = new AtomicReference<>();
          view.onSubmit(entered::set);
          id.setText("001234");
          submit.fire();
          assertEquals("001234", entered.get());
          entered.set(null);
          id.fireEvent(new ActionEvent());
          assertEquals("001234", entered.get());
          id.setText("invalid");
          assertEquals("001234", id.getText());
          id.setText("");
          capture(view, "desktop.png");
          view.setBusy(true);
          assertTrue(submit.isDisabled());
          assertTrue(id.isDisabled());
          entered.set(null);
          id.fireEvent(new ActionEvent());
          submit.fire();
          assertNull(entered.get());
          capture(view, "saving.png");
          view.setBusy(false);
          view.setStatus(
              "Attendance could not be saved. Check your connection and try again.", true);
          capture(view, "error.png");

          stage.setWidth(436);
          view.resize(420, 720);
          view.layout();
          assertTrue(view.getStyleClass().contains("compact"));
          assertFits(view, id, submit, view.lookup("#status"));
          capture(view, "narrow.png");
          view.resize(1280, 800);
          view.layout();
          view.setStatus("You're checked in.", false);
          assertFits(view, id, submit, view.lookup("#status"));
          capture(view, "wide.png");
          stage.close();
        });
  }

  @Test
  void registrationHasOnlyThreeFieldsAndClosingCancelsSafely() throws Exception {
    fx(
        () -> {
          Stage owner = new Stage();
          owner.setScene(new Scene(new javafx.scene.layout.VBox(), 300, 200));
          owner.show();
          AlertUtils.stage = owner;
          AtomicReference<Throwable> assertions = new AtomicReference<>();
          PauseTransition inspect = new PauseTransition(Duration.millis(200));
          inspect.setOnFinished(
              event -> {
                Stage dialog =
                    Window.getWindows().stream()
                        .filter(window -> window != owner && window instanceof Stage)
                        .map(window -> (Stage) window)
                        .findFirst()
                        .orElseThrow();
                try {
                  DialogPane pane = (DialogPane) dialog.getScene().getRoot();
                  List<TextField> fields =
                      pane.lookupAll(".text-field").stream().map(node -> (TextField) node).toList();
                  assertEquals(3, fields.size());
                  assertTrue(pane.lookupAll(".radio-button").isEmpty());
                  Button create = (Button) pane.lookupButton(pane.getButtonTypes().get(0));
                  assertTrue(create.isDisabled());
                  TextField first = (TextField) pane.lookup("#firstName");
                  TextField last = (TextField) pane.lookup("#lastName");
                  TextField email = (TextField) pane.lookup("#email");
                  first.setText("Test");
                  last.setText("Member");
                  email.setText("test@example.com");
                  assertFalse(create.isDisabled());
                  email.setText("invalid");
                  assertTrue(create.isDisabled());
                  email.setText("test@example.com");
                  capture(pane, "registration.png");
                } catch (Throwable failure) {
                  assertions.set(failure);
                } finally {
                  dialog.close();
                }
              });
          inspect.play();
          assertTrue(new AlertUtils().getUserInfo().isEmpty());
          owner.close();
          if (assertions.get() != null) {
            throw new AssertionError(assertions.get());
          }
        });
  }
}
