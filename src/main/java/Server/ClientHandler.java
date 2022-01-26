package Server;

//Входящие флаги:
//0x11 - попытка авторизации
//0x15 - клиент хочет передать нам файл


//Исходящие флаги
//0x21 - Auth OK
//0x22 - Auth Fail
//0x25 - можем принять файл
//0x26 - не можем принять файл, нет свободного места
//0x27 - такой файл уже существует
//0x28 - файл получен сервером


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ClientHandler {
    private static final Logger LOGGER = LogManager.getLogger(ClientHandler.class);
    private MainServ serv;
    private Socket socket;
    private String nick;
    private boolean isAuth = false;
    DataInputStream in;
    DataOutputStream out;
    ExecutorService threadService;

    public ClientHandler(MainServ serv, Socket socket) {
        try {
            this.serv = serv;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.threadService = serv.threadService;

            threadService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            byte flag = in.readByte();
                            //попытка авторизации
                            if (flag == 0x11) {
                                byte loginLength = in.readByte();
                                LOGGER.info(loginLength);
                                byte hashLength = in.readByte();
                                LOGGER.info(hashLength);
                                byte[] loginBytes = new byte[loginLength];
                                byte[] hashBytes = new byte[hashLength];
                                for (int i = 0; i < loginLength; i++) {
                                    loginBytes[i] = in.readByte();
                                }
                                for (int i = 0; i < hashLength; i++) {
                                    hashBytes[i] = in.readByte();
                                }
                                String login = new String(loginBytes);
                                ByteBuffer bb = ByteBuffer.wrap(hashBytes);
                                int hash = bb.getInt();
                                LOGGER.info(login + " " + hash);
                                if (AuthService.isAuth(login, hash)) {
                                    out.writeByte(0x21);
                                } else {
                                    out.writeByte(0x22);
                                }
                            }
                            //нам предлагают принять файл, указав его имя и размер
                            if (flag == 0x15) {
                                byte nameLength = in.readByte();
                                byte[] fileNameBytes = new byte[nameLength];
                                byte[] fileSizeBytes = new byte[Long.BYTES];
                                for (int i = 0; i < nameLength; i++) {
                                    fileNameBytes[i] = in.readByte();
                                }
                                for (int i = 0; i < Long.BYTES; i++) {
                                    fileSizeBytes[i] = in.readByte();
                                }
                                String fileName = new String(fileNameBytes);
                                LOGGER.info(fileName);
                                ByteBuffer bb = ByteBuffer.wrap(fileSizeBytes);
                                long fileSize = bb.getLong();
                                File fileToSave = new File(fileName);
                                LOGGER.info(fileSize + " Свободного места на диске: " + fileToSave.getUsableSpace());
                                LOGGER.info(fileToSave + " Путь сохранения: " + fileToSave.getAbsolutePath());
                                //Если такого файла ещё нет и на диске есть место, то отправляем флаг 0x25 готовности к приёму
                                //в противном случае флаг 0x26, принять файл не можем
                                if (!fileToSave.exists()) {
                                    fileToSave.createNewFile();
                                    if (fileToSave.getUsableSpace() > fileSize) {
                                        out.writeByte(0x25);
                                    }else {
                                        out.writeByte(0x26);
                                    }
                                    fileToSave.delete();
                                } else {
                                    out.writeByte(0x27);
                                }
                                FileOutputStream inFileStream = new FileOutputStream(fileToSave, true);
                                long packetsNum = fileSize/1024;
                                int lastPacketSize = (int) fileSize%1024;
                                byte[] buf = new byte[1024];
                                for (int i = 0; i < packetsNum; i++) {
                                    int j=in.read(buf);
                                    inFileStream.write(buf);
                                }
                                if (lastPacketSize != 0){
                                    byte [] lastPacket = new byte[lastPacketSize];
                                    in.read(lastPacket);
                                    inFileStream.write(lastPacket);
                                }
                                out.writeByte(0x28);
                                LOGGER.info("Файл "+fileName+" получен");
                                inFileStream.close();
                            }

                        }

                    } catch (IOException e) {
                        LOGGER.error(e.getMessage());
                    } finally {
                        try {
                            in.close();
                        } catch (IOException e) {
                            LOGGER.error(e.getMessage());
                        }
                        try {
                            out.close();
                        } catch (IOException e) {
                            LOGGER.error(e.getMessage());
                        }
                        try {
                            socket.close();
                        } catch (IOException e) {
                            LOGGER.error(e.getMessage());
                        }
                    }

                }
            });

        } catch (
                IOException e) {
            LOGGER.error(e.getMessage());
        }

    }
}
