package server;

import java.sql.*;

public class MySQLAuthService implements AuthService{
    private static Connection connection;
    private static Statement stmt;
    private static PreparedStatement psInsert;
    private static PreparedStatement psSelectLoginWithSelectionLogin;
    private static PreparedStatement psSelectWithSelectionLogin;
    private static PreparedStatement psUpdateNickname;
    private static PreparedStatement psSelectUser;

    public MySQLAuthService(){
        try {

            connect();
            psInsert = connection.prepareStatement("INSERT INTO users (login, password, nickname) VALUES ( ? , ? , ?);");
            psSelectLoginWithSelectionLogin = connection.prepareStatement("SELECT  login FROM users WHERE login = ? OR nickname = ?;");
            psSelectWithSelectionLogin = connection.prepareStatement("SELECT login, password, nickname FROM users WHERE login = ?;");
            psSelectUser = connection.prepareStatement("SELECT login, nickname FROM users WHERE nickname = ?;");
            psUpdateNickname = connection.prepareStatement("UPDATE users SET nickname = ? WHERE login = ?;");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        try {
            psSelectWithSelectionLogin.setString(1,login);
            ResultSet rs = psSelectWithSelectionLogin.executeQuery();
            if(!rs.next()){
                return null;
            }
            if (rs.getString("login").equals(login) && rs.getString("password").equals(password)) {
                //System.out.println(rs.getString("name") + " " + rs.getInt("score"));
                return rs.getString("nickname");
            }else{
                return null;
            }
        }catch (SQLException e) {
            return null;
        }

    }

    @Override
    public boolean registration(String login, String password, String nickname) {
        try {
            psSelectLoginWithSelectionLogin.setString(1,login);
            psSelectLoginWithSelectionLogin.setString(2,nickname);
            ResultSet rs = psSelectLoginWithSelectionLogin.executeQuery();
            if (rs.next()) {
                return false;
            }else{
                exInsert(login, password, nickname);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean changeNick(String login, String nickname) {
        try{

            psSelectUser.setString(1,nickname);
            ResultSet rs = psSelectLoginWithSelectionLogin.executeQuery();
            if (rs.next()) {
                return false;
            }

            psUpdateNickname.setString(1,nickname);
            psUpdateNickname.setString(2,login);
            psUpdateNickname.executeUpdate();
            return true;
        }catch (SQLException e){
            e.printStackTrace();
        }

        return false;
    }

    /*
     Будет вызывать загрузчик классов
    */
    public void connect() throws Exception {
        Class.forName("org.sqlite.JDBC");  //позволяет по имени класса загружать его в память
        connection = DriverManager.getConnection("jdbc:sqLite:MagicChat.db"); //url или путь в зависимости от типа базы
        stmt = connection.createStatement(); //необходим для реализации запросов
    }

    public void disconnect(){
        try {
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try{
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("SQLite disconnected");
    }

    public static void exInsert(String login, String password, String nickname) throws SQLException {

        psInsert.setString(1, login);
        psInsert.setString(2, password);
        psInsert.setString(3, nickname);
        psInsert.executeUpdate();

    }

}
