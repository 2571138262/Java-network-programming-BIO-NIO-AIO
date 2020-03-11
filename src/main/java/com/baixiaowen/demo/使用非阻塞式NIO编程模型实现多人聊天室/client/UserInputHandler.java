package com.baixiaowen.demo.使用非阻塞式NIO编程模型实现多人聊天室.client;




import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 处理用户在控制台中的输入， 因为等待用户的输入本身是一个阻塞式的调用
 * 并且把用户的输入发送到服务器端
 */
public class UserInputHandler implements Runnable {

    private ChatClient chatClient;

    public UserInputHandler(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run() {
        try {
            // 等待用户输入信息
            BufferedReader consoleReader = new BufferedReader(
                    new InputStreamReader(System.in)
            );
            while (true) {
                String input = consoleReader.readLine();
                
                // 向服务器发送消息
                chatClient.send(input);
                
                // 检查用户是否准备退出
                if (chatClient.readyToQuit(input)){
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
