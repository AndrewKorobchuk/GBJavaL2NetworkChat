package server;

public interface AuthService {

    String getNicknameByLoginAndPassword(String login, String password);
    boolean registration(String login, String password, String nickname);
    boolean changeNick(String oldNickname, String newNickname);
    default void disconnect() {

    }
}
