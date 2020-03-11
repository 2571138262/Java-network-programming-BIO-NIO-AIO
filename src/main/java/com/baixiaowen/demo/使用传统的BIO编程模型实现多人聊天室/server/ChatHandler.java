package com.baixiaowen.demo.使用传统的BIO编程模型实现多人聊天室.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * 1、真正的一对一的和每一个用户进行信息的交换
 * 2、在BIO（同步阻塞）模型中是一个客户对应一个线程
 */
public class ChatHandler implements Runnable{
    
    private ChatServer server;
    // 客户端socket
    private Socket socket;

    public ChatHandler(ChatServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // 存储新创建客户端用户
            server.addClient(this.socket);
            
            // 读取用户发送的消息
            BufferedReader reader = new BufferedReader(
              new InputStreamReader(socket.getInputStream())      
            );
            
            // 循环读取客户端发送来的信息
            String msg = null;
            while ((msg = reader.readLine()) != null){
                String fwdMsg = "客户端【" + socket.getPort() + "】：" + msg + "\n";
                System.out.println(fwdMsg);

                // 将消息转发给消息室里在线的其他用户
                server.forwardMessage(socket, fwdMsg);
                
                // 检查用户是否准备推出
                if (server.readyToQuit(msg)){
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // 把不需要进行消息同步的用户从集合中移除，把退出聊天室的用户移除
                server.removeClient(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
