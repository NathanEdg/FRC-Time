package scenes;

import activities.MemberRegistration;
import helpers.InputValidation;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class RegistrationView extends VBox {
  private final GridPane form = new GridPane();
  private final TextField first = addField("First name", "firstName", 0);
  private final TextField last = addField("Last name", "lastName", 2);
  private final TextField email = addField("Email", "email", 4);
  private final Button register = new Button("Register and check in");
  private final Button back = new Button("Back");
  private final Label status = new Label();

  RegistrationView(String id, Consumer<MemberRegistration> submit, Runnable cancel) {
    super(8);
    setId("registration");
    setMaxWidth(480);
    setAlignment(Pos.CENTER_LEFT);
    form.setVgap(8);
    form.setPadding(new Insets(12, 0, 12, 0));
    email.setPromptText("you@example.com");
    Label heading = new Label("Register as a new member");
    heading.getStyleClass().add("heading");
    heading.setWrapText(true);
    Label memberId = new Label("Member ID " + id);
    memberId.getStyleClass().add("instruction");
    register.setId("registerMember");
    back.setId("cancelRegistration");
    back.getStyleClass().add("secondary-button");
    back.setCancelButton(true);
    register.setDefaultButton(true);
    register
        .disableProperty()
        .bind(
            Bindings.createBooleanBinding(
                () ->
                    form.isDisabled()
                        || !InputValidation.isValidMember(
                            first.getText(), last.getText(), email.getText()),
                form.disabledProperty(),
                first.textProperty(),
                last.textProperty(),
                email.textProperty()));
    register.setOnAction(
        event ->
            submit.accept(
                new MemberRegistration(first.getText(), last.getText(), email.getText())));
    back.setOnAction(event -> cancel.run());
    status.setWrapText(true);
    status.getStyleClass().add("status");
    getChildren().addAll(heading, memberId, form, new HBox(8, back, register), status);
    Platform.runLater(first::requestFocus);
  }

  void setBusy(boolean busy) {
    form.setDisable(busy);
    back.setDisable(busy);
    status.getStyleClass().remove("error");
    status.setText(busy ? "Saving registration…" : "");
  }

  void showError(String message) {
    setBusy(false);
    status.getStyleClass().add("error");
    status.setText(message);
  }

  private TextField addField(String name, String id, int row) {
    Label label = new Label(name);
    label.getStyleClass().add("field-label");
    TextField field = new TextField();
    field.setId(id);
    field.setMaxWidth(Double.MAX_VALUE);
    GridPane.setHgrow(field, Priority.ALWAYS);
    label.setLabelFor(field);
    form.add(label, 0, row);
    form.add(field, 0, row + 1);
    return field;
  }
}
