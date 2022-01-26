package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class MainServ {
    private Vector<Server.ClientHandler> clients;
    ExecutorService threadService = Executors.newCachedThreadPool();
    private static final Logger LOGGER = LogManager.getLogger(MainServ.class);

    public MainServ() {
        clients = new Vector<>();
        ServerSocket server = null;
        Socket socket = null;

        try {
            AuthService.connect();
//            AuthService.addUser("login1", "pass1", "nick1");
//            AuthService.addUser("login2", "pass2", "nick2");
//            AuthService.addUser("login3", "pass3", "nick3");

            server = new ServerSocket(8189);
            LOGGER.info("Сервер запущен!");
            while (true) {
                socket = server.accept();
                LOGGER.info("Клиент подключился!");
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } finally {
            try {
                socket.close();
                LOGGER.info("Клиент отключился");
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }
            try {
                server.close();
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }
            AuthService.disconnect();
            threadService.shutdownNow();
        }
    }
}
