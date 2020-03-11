package com.baixiaowen.demo.使用传统的BIO编程模型实现多人聊天室.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 1、监听有没有客户端发起建立连接的要求，有要建立连接的请求就accept
 * 
 *      为了让服务器把在线的用户信息发送给所有客户端，所以需要在服务器端保存在线用户信息
 */
public class ChatServer {
    
    private int DEFAULT_PORT = 8888;
    private final String QUIT = "quit";
    
    private ExecutorService executorService;
    private ServerSocket serverSocket;
    
    // 保存所有向客户端的输出流信息， 
    private Map<Integer, Writer> connectedClients;
    
    public ChatServer(){
        connectedClients = new HashMap<>();
        executorService = Executors.newFixedThreadPool(10);
    }

    /**
     * 保存所有客户端信息  保存所有像客户端的输出流的信息
     * @param socket 要保存的客户端用户
     * @throws IOException
     */
    public synchronized void addClient(Socket socket) throws IOException {
        if (socket != null){
            int port = socket.getPort();
            BufferedWriter writer = new BufferedWriter(
              new OutputStreamWriter(socket.getOutputStream())      
            );
            
            connectedClients.put(port, writer);
            System.out.println("客户端【" + port + "】已经连接到服务器");
        }
    }

    /**
     * 将要下线的用户移除掉
     * @param socket 要移除的客户端用户
     * @throws IOException
     */
    public synchronized void removeClient(Socket socket) throws IOException {
        if (socket != null){
            int port = socket.getPort();
            if (connectedClients.containsKey(port)){
                connectedClients.get(port).close();
            }
            connectedClients.remove(port);
            System.out.println("客户端【" + port + "】已断开连接");
        }
    }

    /**
     * 当服务器收到任何一个用户发送过来的一个消息后，服务器要把这条消息发送给所有的用户，除了发送者之外
     * @param socket 发送消息的客户端用户
     * @param fwdMsg 发送的消息内容
     * @throws IOException              
     */
    public synchronized void forwardMessage(Socket socket, String fwdMsg) throws IOException {
        for (Integer id : connectedClients.keySet()) {
            // 主要不是发送消息的人，就要把这条消息转发给他
            if (!id.equals(socket.getPort())){
                Writer writer = connectedClients.get(id);
                writer.write(fwdMsg);
                writer.flush();
            }
        }
    }

    /**
     * 启动服务器端的主要逻辑
     */
    public void start(){
        try {
            // 绑定监听端口
            serverSocket = new ServerSocket(DEFAULT_PORT);
            System.out.println("启动服务器，监听端口 " + DEFAULT_PORT + "...");
            
            while(true){
                // 等待客户端连接
                Socket socket = serverSocket.accept();
                // 创建一个ChatHandler线程
//                new Thread(new ChatHandler(this, socket)).start();
                // 通过线程池来进行优化
                executorService.execute(new ChatHandler(this, socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            this.close();
        }
    }
    
    public boolean readyToQuit(String msg){
        return QUIT.equals(msg);
    }

    /**
     * 关闭资源    --   serverSocket
     */
    public synchronized void close(){
        if (serverSocket != null){
            try {
                serverSocket.close();
                System.out.println("关闭serverSocket");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.start();
    }
    
}
