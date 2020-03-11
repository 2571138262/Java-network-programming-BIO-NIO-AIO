package com.baixiaowen.demo.简易版的WebServer.connector;

import com.baixiaowen.demo.简易版的WebServer.processor.ServletProcessor;
import com.baixiaowen.demo.简易版的WebServer.processor.StaticProcessor;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

/**
 * Connector负责和客户之间创建好连接，
 * 将由这个连接上发送来的任何请求数据交个Processor去做进一步的处理
 * <p>
 * 这个组件是和网络IO关系最大的组件
 */
public class Connector implements Runnable {

    private static final int DEFAULT_PORT = 8888;

    // BIO
    private ServerSocket server;
    
    // NIO
    private ServerSocketChannel serverChannel;
    private Selector selector;
    
    private int port;

    public Connector() {
        this(DEFAULT_PORT);
    }

    public Connector(int port) {
        this.port = port;
    }

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        // BIO 模式实现（没有使用线程池进行优化）
//        try {
//            server = new ServerSocket(port);
//            System.out.println("启动服务器，监听端口：" + port);
//
//            while (true) {
//                // BIO 获取客户端Socket的input 和 output
//                Socket socket = server.accept();
//                InputStream input = socket.getInputStream();
//                OutputStream output = socket.getOutputStream();
//
//                // 创建客户端Request 传入 客户端Socket input
//                Request request = new Request(input);
//                request.parse();
//
//                // 创建响应客户端Response 传入 客户端Socket output
//                Response response = new Response(output);
//                response.setRequest(request);
//
//                // 创建Processor 处理客户端发来的Request请求，并且返回Response响应
//                if (request.getRequestURI().startsWith("/servlet/")) {
//                    ServletProcessor processor = new ServletProcessor();
//                    processor.process(request, response);
//                } else {
//                    StaticProcessor processor = new StaticProcessor();
//                    processor.process(request, response);
//                }
//
//                // 在这个简易版的WebServer中就让客户端和服务器端建立长连接了
//                // 这个和聊天室不同, 这里只是进行处理完请求就进行关闭
//                close(socket);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            // 如果在服务器处理Request请求中发生了错误，那么这里要关闭Server资源
//            close(server);
//        }
        
        // NIO模式实现
        try {
            /**
             * ServerSocketChannel通道本身可以支持非阻塞式的调用， 也可以支持阻塞式调用
             * 默认使用open创建的是阻塞式调用的模式
             * NIO网络编程模型的核心是要支持非阻塞式调用，可以进行IO操作读和写的这样的一个操作
             */
            serverChannel = ServerSocketChannel.open();
            // 设置ServerSocketChannel处于非阻塞式调用
            serverChannel.configureBlocking(false);
            /**
             * 绑定监听端口
             *      注意并不是把通道绑定监听端口
             *      而是把通道所关联的ServerSocket绑定到监听端口
             */
            serverChannel.socket().bind(new InetSocketAddress(port));

            selector = Selector.open();
            /**
             * 把ServerSocketChannel注册在Selector上, 让Selector监控Channel的accept状态，
             * 一旦服务器端通道接受了新的客户端发送的连接请求的时候，就能发现这个事件，这个事件相关的信息都会包含在SelectionKey中
             */
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("启动服务器， 监听端口：" + port + "...");

            /**
             * 在调用select()函数的时候都没有Selector在监听的事件发生，这个select()函数是不会返回的，会一直阻塞在哪
             * 直到有监听的事件发生了才会返回， 返回的结果是一个整数，返回是有多少被监听的事件发生了
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

    private void handles(SelectionKey key) throws IOException {
        // 处理ACCEPT事件
        if (key.isAcceptable()) {
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            SocketChannel client = server.accept();

            // 处理客户端的Read事件
            // 保证Channel处于非阻塞的状态
            client.configureBlocking(false);
            // 为Channel在Selector上注册Read事件
            client.register(selector, SelectionKey.OP_READ);
        }
        // 处理READ事件
        else {
            SocketChannel client = (SocketChannel) key.channel();
            // TODO 在接收到客户端发送来的请求的以后，我们需要先获取到客户端Socket的输入里和输出流，
            //  然后通过输入流的得到的Request来判断：当前用户请求的静态资源还是非静态资源， 
            //  所以如果我们想要使用客户端的InputStream 和 OutputStream 这本身是阻塞式调用的
            
            // TODO 但是对于NIO来说，虽然即可以支持阻塞操作，也可以支持非阻塞操作，但如果和Selector一起使用的时候我们必须保证这条Channel是处于一个非阻塞的状态
            //  并且处在Selector注册器上的Channel，只能是非阻塞的Channel，如果改成阻塞式Channel，此时会抛出异常
            
            // TODO 因为这里实现的Web服务器并不是长连接服务，一次请求操作成功之后就会断开连接， 和多人聊天室那种长连接不一样
            //   所以我们可以调用key.cancel()方法可以使Selector和当前SocketChannel解锁（不再需要Selector来监听当前Channel），
            //   此时可以把当前的Channel恢复成阻塞状态，
            key.cancel();
            
            // 将当前的SocketChannel变会阻塞式调用模式
            client.configureBlocking(true);
            Socket clientSocket = client.socket();
            InputStream input = clientSocket.getInputStream();
            OutputStream output = clientSocket.getOutputStream();
            
            Request request = new Request(input);
            request.parse();
            
            Response response = new Response(output);
            response.setRequest(request);
            
            if (request.getRequestURI().startsWith("/servlet")){
                ServletProcessor processor = new ServletProcessor();
                processor.process(request, response);
            } else {
                StaticProcessor processor = new StaticProcessor();
                processor.process(request, response);
            }
            
            close(client);
        }
        
        
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
}
