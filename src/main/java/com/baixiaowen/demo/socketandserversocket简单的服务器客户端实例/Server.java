package com.baixiaowen.demo.socketandserversocket简单的服务器客户端实例;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static void main(String[] args) {
        final int DEFAULT_PORT = 8888;
        final String QUIT = "quit";

        ServerSocket serverSocket = null;

        try {
            // 1、绑定监听端口
            serverSocket = new ServerSocket(DEFAULT_PORT);

            System.out.println("启动服务器， 监听端口 " + DEFAULT_PORT);

//            while (true) {
            // 2、等待客户端连接  连接成功以后会得到一个代表客户端的socket
            // accept()函数是阻塞式的，一旦调用了accept函数，当前的线程就会被阻塞，直到我们收到了某个客户端发来的连接请求
            Socket socket = serverSocket.accept();
            System.out.println("客户端【" + socket.getPort() + "】已连接");

            // 从客户端端点(socket)读取客户端发过来的数据
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            // 向客户端端点(socket)发送数据
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())
            );

            String msg = null;

            while ((msg = reader.readLine()) != null) {
                // 读取客户端发送的消息 readLine()可以读出一行数据 （行分割符）
                System.out.println("客户端【" + socket.getPort() + "】:" + msg);

                // 回复客户发送的消息
                writer.write("服务器:" + msg + "\n");
                writer.flush();

                // 查看客户端是否退出
                if (QUIT.equals(msg)) {
                    System.out.println("客户端【" + socket.getPort() + "】已断开");
                    break;
                }
            }
//            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                    System.out.println("关闭服务器serverSocket");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
