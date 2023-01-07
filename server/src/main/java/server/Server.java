package server;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private ServerSocket server;
    private Socket socket;
    private final int PORT = 8189;

    private List<ClientHandler> clients;
    private AuthService authService;
    private ExecutorService executorService;

    public Server() {
        LogManager manager = LogManager.getLogManager();
        try {
            manager.readConfiguration(new FileInputStream("logging.properties"));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error reading logging configuration file");
        }

        executorService = Executors.newCachedThreadPool();
        clients = new CopyOnWriteArrayList<>();
        if(!SQLHandler.connect()){
            logger.log(Level.SEVERE, "Failed to connect to database");
            throw new RuntimeException("Failed to connect to database");
        }
        authService = new DBAuthService();
        try {
            server = new ServerSocket(PORT);
            logger.log(Level.INFO, "Server started!");

            while (true) {
                socket = server.accept();
                logger.log(Level.INFO, "Client connected: " + socket.getRemoteSocketAddress());
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.toString());
            e.printStackTrace();
        } finally {
            executorService.shutdown();
            SQLHandler.disconnect();
            logger.log(Level.INFO, "Server stop!");
            try {
                server.close();
                authService.disconnect();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Ошибка при остановке сервера: " + e.toString());
                e.printStackTrace();
            }
        }
    }

    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientList();
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientList();
    }

    public void broadcastMsg(ClientHandler sender, String msg) {
        String message = String.format("[ %s ]: %s", sender.getNickname(), msg);
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
    }

    public void privateMsg(ClientHandler sender, String receiver, String msg) {
        String message = String.format("[ %s ] to [ %s ]: %s", sender.getNickname(), receiver, msg);
        for (ClientHandler c : clients) {
            if (c.getNickname().equals(receiver)) {
                c.sendMsg(message);
                if (!sender.getNickname().equals(receiver)) {
                    sender.sendMsg(message);
                }
                return;
            }
        }
        sender.sendMsg("not found user: " + receiver);
    }

    public boolean isLoginAuthenticated(String login) {
        for (ClientHandler c : clients) {
            if (c.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    public void broadcastClientList() {
        StringBuilder sb = new StringBuilder("/clientlist");

        for (ClientHandler c : clients) {
            sb.append(" ").append(c.getNickname());
        }

        String message = sb.toString();

        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
    }

    public AuthService getAuthService() {
        return authService;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }
}
