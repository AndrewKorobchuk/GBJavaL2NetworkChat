package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class RegController {
    @FXML
    public TextArea textArea;
    @FXML
    public PasswordField passwordField;
    @FXML
    public TextField nicknameField;
    @FXML
    public TextField loginField;

    private Controller controller;

    public void setController(Controller controller) {
        this.controller = controller;
    }

    @FXML
    public void clickBtnReg(ActionEvent actionEvent) {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        String nickname = nicknameField.getText().trim();
        controller.tryToReg(login, password, nickname);
    }

    public void regStatus(String result) {
        if (result.equals("/reg_ok")) {
            textArea.appendText("registration completed successfully\n");
        } else {
            textArea.appendText("Registration failed. Login or nickname taken\n");
        }
    }
}
