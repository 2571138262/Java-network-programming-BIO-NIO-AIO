package com.baixiaowen.demo.使用非阻塞式NIO编程模型实现多人聊天室.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Set;

public class ChatServer {

    private static int DEFAULT_PORT = 8888;
    private final String QUIT = "quit";
    private static final int BUFFER = 1024;

    // 处理服务器端的IO的通道
    private ServerSocketChannel server;
    // 监控Channel的Selector
    private Selector selector;
    // 处理从通道里边读取数据，相对应的就是将通道中的数据写入到rBuffer中
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    // 处理向通道里边写入数据，相对应的就是将Buffer中的数据写入到同道中    ----  用来写入其他客户通道的缓冲区
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);

    // 文本消息在不同的操作系统都会使用不同的编码，为了避免解码的不同
    private Charset charset = Charset.forName("UTF-8");

    // 用来存储用户自定义的服务器端的端口
    private int port;

    public ChatServer() {
        this(DEFAULT_PORT);
    }

    public ChatServer(int port) {
        this.port = port;
    }

    private void start() {
        try {
            /**
             * ServerSocketChannel通道本身可以支持非阻塞式的调用， 也可以支持阻塞式调用
             * 默认使用open创建的是阻塞式调用的模式
             * NIO网络编程模型的核心是要支持非阻塞式调用，可以进行IO操作读和写的这样的一个操作
             */
            server = ServerSocketChannel.open();
            // 设置ServerSocketChannel处于非阻塞式调用
            server.configureBlocking(false);
            /**
             * 绑定监听端口
             *      注意并不是把通道绑定监听端口
             *      而是把通道所关联的ServerSocket绑定到监听端口
             */
            server.socket().bind(new InetSocketAddress(port));

            selector = Selector.open();
            /**
             * 把ServerSocketChannel注册在Selector上, 让Selector监控Channel的accept状态，
             * 一旦服务器端通道接受了新的客户端发送的连接请求的时候，就能发现这个事件，这个事件相关的信息都会包含在SelectionKey中
             */
            server.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("启动服务器， 监听端口：" + port + "...");

            /**
             * 在调用select()函数的时候都没有Selector在监听的事件发生，这个select()函数是不会返回的，会一直阻塞在哪
             * 知道有监听的事件发生了才会返回， 返回的结果是一个整数，返回是有多少被监听的事件发生了
             * 所以这里要循环调用，然后对这些事件处理
             */
            while (true) {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey key : selectionKeys) {
                    // 处理被触发的事件
                    handles(key);
                }
                // 完成处理以后把SelectionKey清空
                selectionKeys.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭资源的时候没必要多次关闭，把最外层资源关闭里边相应的资源也就都关闭了
            close(selector);
        }
    }

    /**
     * 处理SelectionKey事件
     *
     * @param key
     */
    private void handles(SelectionKey key) throws IOException {
        // ACCEPT 事件  -- 和客户端建立了连接
        if (key.isAcceptable()) {
            // 从触发的SelectionKey事件中获取客户端Channel通道
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            // accept客户端发送的连接请求
            SocketChannel client = server.accept();

            // SocketChannel 和 ServerSocketChannel一样， 默认情况下是阻塞式调用模式，修改成非阻塞式调用
            client.configureBlocking(false);
            // 在Selector上注册客户端SocketChannel，监听时间为READ事件
            client.register(selector, SelectionKey.OP_READ);
            System.out.println(getClientName(client) + "已连接");
        }
        // READ 事件    -- 客户端发送了消息给服务端
        else if (key.isReadable()) {
            // 从触发的SelectionKey事件中获取客户端Channel通道
            SocketChannel client = (SocketChannel) key.channel();
            // 从客户端Channel中取得传送来的数据
            String fwdMsg = receive(client);
            if (fwdMsg.isEmpty()) {
                // 客户端异常，
                // 从Selector中取消当前客户端Channel的当前SelectionKey事件的注册
                key.cancel();
                // 要求Selector把当前被阻塞的select函数强制返回，相当于强制Selector重新审视一下它目前所监听的各个通道的各个事件的最新情况
                selector.wakeup();
            } else {
                // 把当前用户的信息转发给其他用户(自身除外)
                forwardMessage(client, fwdMsg);

                // 检查用户是否退出
                if (readyToQuit(fwdMsg)) {
                    // 如果客户端要退出，就取消事件的注册监听
                    key.cancel();
                    selector.wakeup();
                    System.out.println(getClientName(client) + "已断开");
                }
            }
        }
    }

    /**
     * 将当前客户端发送来的消息转发给其他在线的客户端， 不包含自己本身
     *
     * @param client
     * @param fwdMsg
     */
    private void forwardMessage(SocketChannel client, String fwdMsg) throws IOException {
        /**
         * selector.keys() 和 selector.selectedKeys()不一样
         *  selector.keys() : 返回所有注册在当前Selector上的SelectionKey事件的集合
         *  selector.selectedKeys() : 返回的是注册再当前Selector上的已经触发了的SelectionKey事件的集合
         *  这里可以认为凡是注册在Selector上的都是在线的
         */
        for (SelectionKey key : selector.keys()) {
            Channel connectedClient = key.channel();
            // 不需要给服务端的Channel转发消息
            if (connectedClient instanceof ServerSocketChannel) {
                continue;
            }

            // 保证注册的SelectionKey有效，并且不是发送信息的Channel
            if (key.isValid() && !client.equals(connectedClient)) {
                wBuffer.clear();
                // 向wBuffer中写入数据
                wBuffer.put(charset.encode(getClientName(client) + ":" + fwdMsg));
                // 将wBuffer从写模式转换为读模式
                wBuffer.flip();
                // 将从wBuffer中读到的数据写入到通道中去
                while (wBuffer.hasRemaining()) {
                    ((SocketChannel) connectedClient).write(wBuffer);
                }
            }
        }
    }

    /**
     * 获取客户端名称
     *
     * @param client
     * @return
     */
    private String getClientName(SocketChannel client) {
        return "客户端【" + client.socket().getPort() + "】";
    }

    /**
     * 从客户端Channel中取得数据
     *
     * @param client
     * @return
     */
    private String receive(SocketChannel client) throws IOException {
        // 清空rBuffer中的信息
        rBuffer.clear();
        // 循环的将客户端通道中的数据读入到buffer中
        while (client.read(rBuffer) > 0) ;
        // 将rBuffer转换为读模式， 即从Buffer中出数据
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }

    /**
     * 测试用户发来的信息是否是退出指令
     *
     * @param msg
     * @return
     */
    private boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    /**
     * 资源关闭方法
     *
     * @param closeable
     */
    private void close(Closeable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer(7777);
        chatServer.start();
    }

}
