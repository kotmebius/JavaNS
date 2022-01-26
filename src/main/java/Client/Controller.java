package Client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Controller {
    final String IP_ADRESS = "localhost";
    final int PORT = 8189;

    @FXML
    Button chooseFile;
    @FXML
    TextField filePath;
    @FXML
    Button sendFileButton;
    @FXML
    TextField MessageLabel;
    @FXML
    VBox body = new VBox();
    @FXML
    HBox upperPanel;
    @FXML
    TextField loginField;
    @FXML
    PasswordField passwordField;

    Socket socket;
    DataInputStream in;
    DataOutputStream out;
    File fileToSend;

    private boolean isAuthorized;

    public void setAuthorized(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
        if (!isAuthorized) {
            upperPanel.setVisible(true);
            upperPanel.setManaged(true);
            chooseFile.setVisible(false);
            chooseFile.setManaged(false);
            filePath.setVisible(false);
            filePath.setManaged(false);
            sendFileButton.setVisible(false);
            sendFileButton.setManaged(false);
        } else {
            upperPanel.setVisible(false);
            upperPanel.setManaged(false);
            chooseFile.setVisible(true);
            chooseFile.setManaged(true);
            filePath.setVisible(true);
            filePath.setManaged(true);
            sendFileButton.setVisible(true);
            sendFileButton.setManaged(true);
        }
    }



    public void connect() {
        try {
            socket = new Socket(IP_ADRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            setAuthorized(false);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            byte inFlag = in.readByte();
                            //Если вернулся флаг 0x21 - авторизация успешная, 0x22 - неуспешная
                            if (inFlag == 0x21) {
                                setAuthorized(true);
                                MessageLabel.setText("Выберите файл для отправки");
                            } else if (inFlag == 0x22) {
                                MessageLabel.setText("Пароль неверный");
                            }
                            if (inFlag == 0x25) {
                                sendFile();
                            } else if (inFlag == 0x26) {
                                MessageLabel.setText("На сервере недостаточно места");
                            } else if (inFlag == 0x27) {
                                MessageLabel.setText("указанный файл уже существует");
                            } else if (inFlag == 0x28) {
                                MessageLabel.setText("файл "+fileToSend.getName()+ " получен сервером");
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            in.close();
                            out.close();
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        setAuthorized(false);
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void tryToAuth() {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            //отсылаем флаг начала авторизации
            out.writeByte(0x11);
            //Преобразуем в массивы байт логин и хэш пароля
            byte[] loginBytes = loginField.getText().getBytes();
            int passHash = passwordField.getText().hashCode();
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.putInt(passHash);
            byte[] hashBytes = bb.array();
            //отправляем длину логина и хэша пароля
            out.writeByte(loginBytes.length);
            out.writeByte(hashBytes.length);
            //отправляем логин и хэш
            for (int i = 0; i < loginBytes.length; i++) {
                out.writeByte(loginBytes[i]);
            }
            for (int i = 0; i < hashBytes.length; i++) {
                out.writeByte(hashBytes[i]);
            }
            loginField.clear();
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void chooseFile() {
        Stage stage = (Stage) chooseFile.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileToSend = fileChooser.showOpenDialog(stage);
        filePath.setText(fileToSend.toString());
    }

    public void readyToSend() {
        try {
            //отсылаем флаг желания передать файл
            out.writeByte(0x15);
            //отсылаем имя файла и иего размер
            String fileName = fileToSend.getName();
            byte[] fileNameBytes = fileName.getBytes();
            out.writeByte(fileNameBytes.length);
            for (int i = 0; i < fileNameBytes.length; i++) {
                out.writeByte(fileNameBytes[i]);
            }
            long fileSize = fileToSend.length();
            ByteBuffer bb = ByteBuffer.allocate(Long.BYTES);
            bb.putLong(fileSize);
            byte[] fileSizeBytes = bb.array();
            for (int i = 0; i < Long.BYTES; i++) {
                out.writeByte(fileSizeBytes[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendFile() {
        try{
            FileInputStream outFileStream = new FileInputStream(fileToSend);
            byte[] buf = new byte[1024];
            int i;
            while((i=outFileStream.read(buf))>=0) {
                out.write(buf);
            }
            outFileStream.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}

