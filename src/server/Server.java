package server;

import netscape.javascript.JSObject;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;

public class Server {
    private static final  String URL_DB = "jdbc:mysql://127.0.0.1:3306/registrator_for_to";
    private static final String LOGIN_DB = "root";
    private static final String PASS_DB = "";
    private static ArrayList<User> users = new ArrayList<>(); // список подключенных пользователей
    private static Connection connection;
    private static Statement statement;
    public static void main(String[] args) {
        ArrayList<User> users= new ArrayList<>();
        try {
            // Создаем сокет сервера (открываем порт для прослушивания)
            ServerSocket serverSocket = new ServerSocket(9537);
            System.out.println("Сервер запущен");
            // Загружаем класс для работы с БД
            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
            while (true){
                // ожидаем подключения и сохраняем его ip и порт (Socket)
                Socket socket = serverSocket.accept();
                System.out.println("Клиент подлючился");
                User user = new User(socket); // Создаем пользователя
                users.add(user); // добавили пользователя в коллекцию подключенных пользователей
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONParser jsonParser = new JSONParser();
                            JSONObject jsonObject;
                            // Авторизация/Регистрация
                            while (true){
                                jsonObject = (JSONObject) jsonParser.parse(user.getIn().readUTF());
                                String action = jsonObject.get("action").toString();
                                String login = jsonObject.get("login").toString();
                                String pass = jsonObject.get("pass").toString();
                                if(action.equals("reg")){
                                    String name = jsonObject.get("name").toString();
                                    if(user.reg(URL_DB, LOGIN_DB, PASS_DB, name, login, pass))  break;
                                } else if (action.equals("login")) {
                                    if(user.login(URL_DB, LOGIN_DB, PASS_DB, login, pass)) break;
                                }
                            }
                            users.add(user);
                            System.out.println(user.getName()+" подключился");
                            jsonObject = new JSONObject();
                            jsonObject.put("msg", user.getName()+ " добро пожаловать на сервер");
                            user.getOut().writeUTF(jsonObject.toJSONString());
                        }
                        catch (IOException e) {
                        System.out.println("Потеряно соединение с клиентом");
                        users.remove(user);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                thread.start();
            }

        } catch (Exception e) {
            System.out.println("класс JDBC не найден");;
        }

    }
}
