package com.baixiaowen.demo.使用AIO异步编程模型来实现多人聊天室.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * 使用AIO异步编程模型来实现多人聊天室
 * 客户端
 */
public class ChatClient {

    private static final String LOCALHOST = "localhost";
    private static final int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;
    private static final int THREADPOOL_SIZE = 8;
    
    private AsynchronousSocketChannel clientChannel;
    private Charset charset = Charset.forName("UTF-8");
    private int port;
    private String host;

    public ChatClient(String host, int port) {
        this.port = port;
        this.host = host;
    }

    public ChatClient() {
        this(LOCALHOST, DEFAULT_PORT);
    }

    private void close(Closeable closeable){
        if (closeable != null){
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void start(){
        try {
            // 创建客户端上的SocketChannel
            clientChannel = AsynchronousSocketChannel.open();
            // clientChannel.connect的返回值是Void，所以这里Future的泛型是Void
            Future<Void> future = clientChannel.connect(new InetSocketAddress(host, port));
            // 调用Future对象的get()方法，阻塞式调用，知道服务器接受了我们的请求
            future.get();
            // 启动一个新的线程处理用户的输入
            // 在UserInputHandler这个线程中处理用户输入数据的操作
            new Thread(new UserInputHandler(this)).start();
            
            // 在主线程中处理用户接受数据的操作
            
            // ByteBuffer用来存放从服务端读到的数据
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER);
            while (true){
                // 通过客户端通道读取服务端发送来的数据，并写入到buffer中，此时buffer处于写状态
                Future<Integer> readResult = clientChannel.read(buffer);
                // 使用本地变量来存放客户端通道读取的结果，这里也是阻塞式调用，
                int result = readResult.get();
                if (result <= 0){
                    // 服务器异常，服务断开
                    System.out.println("服务器断开");
                    close(clientChannel);
                    // System.exit()方法可以强行的关闭整个JVM，将所有的线程一起关闭，
                    // 参数如果为 0 代表正常关闭， 参数如果为 1 代表异常关闭
                    System.exit(1);
                } else {
                    // 如果有读到的数据，就将数据读出来并且打印到控制台上
                    // 此时需要将Buffer从写模式变成读模式
                    buffer.flip();
                    String msg = String.valueOf(charset.decode(buffer));
                    // 清空Buffer
                    buffer.clear();
                    System.out.println(msg);
                }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }

    /**
     * 客户端实现发送消息的功能
     * @param msg
     */
    public void send(String msg){
        if (msg.isEmpty()){
            return;
        }
        // 将用户输入的msg转码成Buffer
        ByteBuffer buffer = charset.encode(msg);
        // 调用客户端通到的write()方法，将buffer中的数据写入到通道中，此时Buffer处于读操作
        // 返回的结果是操作了数据的行数
        Future<Integer> writeResult = clientChannel.write(buffer);
        // 调用Future对象的get()方法等待clientChannel.write(buffer)的执行结果
        try {
            writeResult.get();
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("消息发送失败");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient("127.0.0.1", 7777);
        client.start();
    }

    /**
     * 判断用户是否要退出
     * @param input
     * @return
     */
    public boolean readyToQuit(String input) {
        return QUIT.equals(input);
    }
}
