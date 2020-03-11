package com.baixiaowen.demo.使用AIO异步编程模型来实现多人聊天室.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 使用AIO异步编程模型来实现多人聊天室
 * 服务器端
 */
public class ChatServer {

    private static final String LOCALHOST = "localhost";
    private static final int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;
    private static final int THREADPOOL_SIZE = 8;

    // 这里打算自定义AsynchronousChannelGroup异步通道群组，来处理异步通道任务和回调函数的调用
    // 这里可以自定义AsynchronousChannelGroup中包含的线程池
    private AsynchronousChannelGroup channelGroup;
    private AsynchronousServerSocketChannel serverChannel;
    private List<ClientHandler> connectedClients;
    private Charset charset = Charset.forName("UTF-8");
    private int port;

    public ChatServer() {
        this(DEFAULT_PORT);
    }

    private ChatServer(int port) {
        this.port = port;
        this.connectedClients = new ArrayList<>();
    }

    private boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void start() {
        try {
            // 创建一个用户AsynchronousChannelGroup使用的线程池
            ExecutorService executorService = Executors.newFixedThreadPool(THREADPOOL_SIZE);
            // 创建一个AsynchronousChannelGroup
            channelGroup = AsynchronousChannelGroup.withThreadPool(executorService);
            // 在创建AsynchronousServerSocketChannel的时候传入自定义的AsynchronousChannelGroup
            // 这个时候创建的AsynchronousServerSocketChannel就不在属于默认的AsynchronousChannelGroup，而是属于我们刚刚创建的ChannelGroup
            serverChannel = AsynchronousServerSocketChannel.open(channelGroup);
            // 绑定我们在服务器端想要监听的端口
            serverChannel.bind(new InetSocketAddress(LOCALHOST, port));
            System.out.println("启动服务器，监听端口：【" + port + "】");

            // 然后就开始监听客户端发送来的连接请求了
            while (true) {
                serverChannel.accept(null, new AcceptHandler());
                // 阻塞式调用，相当于一直在等待System.in流上输入的数据
                // 为什么要设置一条这阻塞式调用呢？因为不想浪费系统资源，在while loop中不停的去调用accept函数
                // 希望使用AcceptHandler函数来进一步持续的让我们的ServerChannel来接收客户端发来的请求
                System.in.read();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(serverChannel);
        }
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer(7777);
        server.start();
    }

    /**
     * AsynchronousSocketChannel ： 指的是AcceptHandler处理结果完成之后返回的对象类型   ----  其实就是指上边调用的serverChannel.accept()返回的结果对象
     * Object ；指的是attachment(附件)的类型
     */
    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {
        /**
         * completed函数被调用，表示服务器之前调用Accept函数被返回了，也就是服务器接收了一个客户端的请求
         * 然后服务器需要继续进行接收客户单发送的请求
         *
         * @param clientChannel 指的是建立连接的客户端那一边的Channel
         * @param attachment
         */
        @Override
        public void completed(AsynchronousSocketChannel clientChannel, Object attachment) {
            // 保证服务器持续监听客户端发送来的请求
            if (serverChannel.isOpen()) {
                serverChannel.accept(null, this);
            }
            // 只要是客户端Channel没有失效，就开始对客户端Channel进行数据的操作
            if (clientChannel != null && clientChannel.isOpen()) {
                // 处理客户端channel中数据进行处理的CompletionHandler
                ClientHandler handler = new ClientHandler(clientChannel);
                // 通过Buffer来对客户端Channel中的数据进行操作(读或者写)
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER);

                // 要将新用户添加到在线用户列表中去 （持续更新状态）
                addClient(handler);

                // 当从AsynchronousSocketChannel客户端Channel中读到任何数据的时候，请写到buffer中
                // 参数1（buffer）：告诉系统把从ClientChannel中读到的数据写入到这个buffer中
                // 参数2（buffer）：为了后面的操作，当clientHandler的回调函数被调用的时候，需要attachment对象的客户端传来的数据
                clientChannel.read(buffer, buffer, handler);
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("连接失败：" + exc);
        }
    }

    /**
     * 要将新用户添加到在线用户列表中去 （持续更新状态）
     * @param handler
     */
    private synchronized void addClient(ClientHandler handler) {
        connectedClients.add(handler);
        System.out.println(getClientName(handler.clientChannel) + ":已经连接到服务器");
    }

    /**
     * 让当前客户端Channel下线
     * @param handler
     */
    private synchronized void removeClient(ClientHandler handler) {
        connectedClients.add(handler);
        System.out.println(getClientName(handler.clientChannel) + ":已经断开连接");
        close(handler.clientChannel);
    }
    
    /**
     * Integer ： clientChannel.read()方法的返回结果就是操作的字节数，所以这里就是Integer
     * Object ；指的是attachment(附件)的类型
     */
    private class ClientHandler implements CompletionHandler<Integer, Object> {
        
        private AsynchronousSocketChannel clientChannel;
        
        public ClientHandler(AsynchronousSocketChannel clientChannel) {
            this.clientChannel = clientChannel;
        }

        @Override
        public void completed(Integer result, Object attachment) {
            ByteBuffer buffer = (ByteBuffer) attachment;
            // 如果当前是客户端的写操作，那就说明这是要将用户发来的信息转发给其他用户
            // 通过判断attachment是否为空来确定要处理是一个已经完成的读操作还一个写操作
            if (buffer != null){
                if (result <= 0){
                    // 客户端异常，让当前客户端下线
                    removeClient(this);
                } else {
                    // 将buffer从写模式转换为读模式， 这里就是从buffer中读取数据
                    buffer.flip();
                    // 从buffer中提取出数据， 并且进行解密操作
                    String fwdMsg = receive(buffer);
                    // 打印日志
                    System.out.println(getClientName(clientChannel) + ":" + fwdMsg);
                    // 向其他客户端转发消息
                    forwardMessage(clientChannel, fwdMsg);
                    // 将buffer中的数据情况， 也就是将buffer从读模式转换为写模式
                    buffer.clear();
                    
                    // 检查用户是否决定退出
                    if (readyToQuit(fwdMsg)){
                        removeClient(this);
                    } else {
                        clientChannel.read(buffer, buffer, this);
                    }
                }
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("读写失败：" + exc);
        }
    }

    /**
     * 向其他客户端转发消息
     * @param clientChannel
     * @param fwdMsg
     */
    private synchronized void forwardMessage(AsynchronousSocketChannel clientChannel, String fwdMsg) {
        for (ClientHandler handler : connectedClients){
            if (!clientChannel.equals(handler.clientChannel)){
                try {
                    ByteBuffer buffer = charset.encode(getClientName(clientChannel) + ":" + fwdMsg);
                    handler.clientChannel.write(buffer, null, handler);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getClientName(AsynchronousSocketChannel clientChannel) {
        int clientPort = -1;
        try {
            InetSocketAddress remoteAddress = (InetSocketAddress) clientChannel.getRemoteAddress();
            clientPort = remoteAddress.getPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "客户端【" + clientPort + "】";
    }

    /**
     * 从buffer中取出数据，并且解码，得到字符串
     * @param buffer
     * @return
     */
    private String receive(ByteBuffer buffer) {
        CharBuffer decode = charset.decode(buffer);
        return String.valueOf(decode);
    }
}
