package com.geekbrains.cloud.jan;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static com.geekbrains.cloud.jan.Sender.getFile;
import static com.geekbrains.cloud.jan.Sender.sendFile;
import static java.nio.file.Files.isDirectory;

public class Handler implements Runnable {

    private static final int SIZE = 256;

    private Path clientDir;
    private Path rootDir;
    private String dirToDown;
    private DataInputStream is;
    private DataOutputStream os;
    private final byte[] buf;
    private String login;
    private int hash;
    boolean isAuth = false;

    public Handler(Socket socket) throws IOException {
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        buf = new byte[SIZE];
    }

    public void sendServerFiles() throws IOException {
        List<String> files = Files.list(clientDir)
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
        os.writeUTF("#list#");
        System.out.println(clientDir.toString());
        os.writeUTF(clientDir.toString());
        os.writeInt(files.size());
        for (String file : files) {
            os.writeUTF(file);
        }
        os.flush();
    }

    @Override
    public void run() {
        try {
            while (true) {
                String command = is.readUTF();
                System.out.println("received: " + command);
                if (command.equals("#file#") && isAuth) {
                    getFile(is, clientDir, SIZE, buf);
                    sendServerFiles();
                } else if (command.equals("#get_file#") && isAuth) {
                    String fileName = is.readUTF();
                    sendFile(fileName, os, clientDir);
                } else if (command.equals("#dirUp#") && isAuth) {
                    if (!clientDir.equals(rootDir)) {
                        clientDir = clientDir.getParent();
                    }
                    sendServerFiles();
                } else if (command.equals("#dirDown#") && isAuth) {
                    dirToDown = is.readUTF();
                    if (isDirectory(Paths.get(clientDir.toString() + "\\" + dirToDown))) {
                        clientDir = Paths.get(clientDir.toString() + "\\" + dirToDown);
                    }
                    sendServerFiles();
                } else if (command.equals("#auth#")) {
                    login = is.readUTF();
                    hash = is.readInt();
                    AuthService.connect();
                    if (AuthService.isAuth(login, hash)){
                        os.writeUTF("#authOk#");
                        isAuth=true;
                        rootDir = Paths.get("data\\"+login+"\\");
                        if (! rootDir.toFile().exists()){
                            rootDir.toFile().mkdir();
                        }
                        clientDir=rootDir;
                        sendServerFiles();
                        AuthService.disconnect();
                    } else {
                        os.writeUTF("#authFail#");
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
