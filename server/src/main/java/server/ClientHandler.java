package server;

import service.ServiceMessages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class ClientHandler {
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean authenticated;
    private String nickname;
    private String login;

    public ClientHandler(Server server, Socket socket) {
        LogManager manager = LogManager.getLogManager();
        try {
            manager.readConfiguration(new FileInputStream("logging.properties"));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error reading logging configuration file");
        }

        this.server = server;
        this.socket = socket;

        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            server.getExecutorService().execute(() -> {
                try {
                    socket.setSoTimeout(120000);
                    while (true) {
                        String str = in.readUTF();

                        if (str.equals("/end")) {
                            sendMsg("/end");
                            break;
                        }
                        if (str.startsWith(ServiceMessages.AUTH)) {
                            String[] token = str.split(" ", 3);
                            if (token.length < 3) {
                                continue;
                            }
                            String newNick = server.getAuthService()
                                    .getNicknameByLoginAndPassword(token[1], token[2]);
                            login = token[1];
                            if (newNick != null) {
                                if (!server.isLoginAuthenticated(login)) {
                                    authenticated = true;
                                    nickname = newNick;
                                    sendMsg(ServiceMessages.AUTH_OK + " " + nickname);
                                    server.subscribe(this);
                                    //System.out.println("Client: " + nickname + " authenticated");
                                    logger.log(Level.INFO, "Client: " + login + " - authenticated");
                                    break;
                                } else {
                                    sendMsg("Already logged into the chat with this username");
                                }
                            } else {
                                logger.log(Level.WARNING, "Client: " + login + " - incorrect login / password!");
                                sendMsg("Incorrect login / password");
                            }
                        }
                        if (str.startsWith("/reg")) {
                            String[] token = str.split(" ", 4);
                            if (token.length < 4) {
                                continue;
                            }
                            if (server.getAuthService()
                                    .registration(token[1], token[2], token[3])) {
                                sendMsg("/reg_ok");
                            } else {
                                sendMsg("/reg_no");
                            }
                        }
                    }
                    if(authenticated){
                        socket.setSoTimeout(0);
                    }
                    while (authenticated) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                sendMsg("/end");
                                break;
                            }
                            if (str.startsWith("/w")) {
                                String[] token = str.split(" ", 3);
                                if (token.length < 3) {
                                    continue;
                                }
                                server.privateMsg(this, token[1], token[2]);
                            }
                            if(str.startsWith(ServiceMessages.CHANGE_NICKNAME)){

                                String[] token = str.split(" ", 2);
                                if (token.length < 2) {
                                    sendMsg(ServiceMessages.CHANGE_NICKNAME_NO);
                                    continue;
                                }
                                if (token[1].contains(" ")) {
                                    sendMsg(ServiceMessages.CHANGE_NICKNAME_NO + "; Nickname cannot contain spaces");
                                    continue;
                                }
                                if(server.getAuthService().changeNick(this.nickname,token[1])){
                                    this.nickname = token[1];
                                    sendMsg(ServiceMessages.CHANGE_NICKNAME_OK + "; Your nickname changed at " + token[1]);
                                    server.broadcastClientList();
                                }else{
                                    sendMsg(ServiceMessages.CHANGE_NICKNAME_NO + "; Failed to change nickname. Nickname " + token[1] + " already exists");
                                }
                            }
                        } else {
                            server.broadcastMsg(this, str);
                        }
                    }

                }catch (SocketTimeoutException e){
                    sendMsg("/end");
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    logger.log(Level.INFO, "Client: " + login + " disconnect!");
                    server.unsubscribe(this);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }
}
