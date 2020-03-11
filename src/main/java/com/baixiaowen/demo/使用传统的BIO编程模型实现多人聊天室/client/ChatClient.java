package com.baixiaowen.demo.使用传统的BIO编程模型实现多人聊天室.client;

import java.io.*;
import java.net.Socket;

/**
 * 客户端主线程 
 *  1、使用Socket和服务端建立连接
 *  2、不停的监听从服务端转发过来的聊天室中其他用户发送的消息，并在当前客户端显示出来
 */
public class ChatClient {
    
    private final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private final int DEFAULT_SERVER_PORT = 8888;
    private final String QUIT = "quit";

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    /**
     * 1、发送消息给服务器
     * @param msg
     * @throws IOException
     */
    public void send(String msg) throws IOException {
        if (!socket.isOutputShutdown()){
            writer.write(msg + "\n");
            writer.flush();
        }
    }

    /**
     * 2、从服务器端接收消息
     * @return
     * @throws IOException
     */
    public String receive() throws IOException {
        String msg = null;
        if (!socket.isInputShutdown()){
            msg = reader.readLine();
        }
        return msg;
    }

    /**
     * 检查用户是否准备退出
     * @param msg
     * @return
     */
    public boolean readyToQuit(String msg){
        return QUIT.equals(msg);
    }

    /**
     * 客户端启动方法
     */
    public void start(){
        try {
            // 创建socket
            socket = new Socket(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);

            // 创建IO流
            reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );
            writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())
            );

            // 处理用户的输入
            new Thread(new UserInputHandler(this)).start();
            
            // 读取服务器转发的消息
            String msg = null;
            while ((msg = reader.readLine()) != null){
                System.out.println(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            this.close();
        }
    }

    /**
     * 关闭系统资源
     */
    public void close(){
        if (writer != null){
            try {
                writer.close();
                System.out.println("关闭socket");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
        chatClient.start();
    }
    
}
