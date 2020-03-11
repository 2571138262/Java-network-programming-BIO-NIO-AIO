package com.baixiaowen.demo.socketandserversocket简单的服务器客户端实例;

import java.io.*;
import java.net.Socket;

public class Client {

    public static void main(String[] args) {
        
        final String QUIT = "quit";
        final String DEFAULT_SERVER_HOST = "127.0.0.1";
        final int DEFAULT_SERVER_PORT = 8888;

        Socket socket = null;
        BufferedWriter writer = null;
        
        try {
            // 创建客户端Socket
            socket = new Socket(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
            
            // 创建读取服务器端发送的信息的Reader
            BufferedReader reader = new BufferedReader(
              new InputStreamReader(socket.getInputStream())      
            );
            // 创建向服务器daunt发送信息的的Writer
            writer = new BufferedWriter(
              new OutputStreamWriter(socket.getOutputStream())      
            );
            
            // 等待用户输入信息 
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            
            while (true){
                String input = consoleReader.readLine();
                if (input != null && !"".equals(input)){
                    // 发送消息给服务器
                    writer.write(input + "\n");
                    writer.flush();

                    // 读取服务器返回的消息
                    String msg = reader.readLine();
                    System.out.println("客户端接收到服务器端返回的信息 ---- " + msg);

                    // 查看用户是否退出
                    if(QUIT.equals(input)){
                        break;
                    }
                }
            }
            
            
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (writer != null){
                try {
                    writer.close();
                    System.out.println("关闭客户端socket");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
}
