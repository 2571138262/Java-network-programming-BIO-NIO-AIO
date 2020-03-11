package com.baixiaowen.demo.异步调用机制EasyDemo实现回音壁效果;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.Map;

public class Server {

    final String LOCALHOST = "localhost";
    final int DEFAULT_PORT = 8888;

    //异步的服务端Channel
    AsynchronousServerSocketChannel serverChannel;

    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        try {
            // 绑定监听端口
            // AsynchronousChannelGroup 类似于一个线程池，提供一些异步的通道可以共享系统资源
            // 当我们需要针对这些异步的通道执行一些异步的Handler或者Future的时候，
            // 系统就可以去到AsynchronousChannelGroup线程池中找到可以使用线程，来进行这些回调函数的执行
            // 每一个AsynchronousChannel都有所属的AsynchronousChannelGroup，他们共享资源
            // 当没有指定当前AsynchronousChannelGroup，系统默认指定一个AsynchronousChannelGroup
            serverChannel = AsynchronousServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(LOCALHOST, DEFAULT_PORT));
            System.out.println("启动服务器，监听端口：" + DEFAULT_PORT);

            // 循环等待客户端连接，这里使用到了一个小技巧
            while (true) {
                // 使用CompletionHandler来处理服务端的异步的操作
                serverChannel.accept(null, new AcceptHandler());
                System.in.read();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(serverChannel);
        }
    }

    /**
     * CompletionHandler异步调用类
     */
    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {

        /**
         * 异步调用的函数返回的时候调用这个方法
         *
         * @param result
         * @param attachment
         */
        @Override
        public void completed(AsynchronousSocketChannel result, Object attachment) {
            // 如果异步serverChannel没有关闭
            if (serverChannel.isOpen()) {
                // 让它继续去监听从客户端可能发来的连接请求
                serverChannel.accept(null, this);
            }

            /**
             * 服务端接收了客户端的连接请求之后需要进行数据的交互
             */
            AsynchronousSocketChannel clientChannel = result;
            if (clientChannel != null && clientChannel.isOpen()) {
                ClientHandler handler = new ClientHandler(clientChannel);

                /**
                 * 读取客户端发送来的数据，这里也是异步操作
                 *  attachment 对象指的是对操作有意义的对象数据
                 */
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                Map<String, Object> info = new HashMap<>();
                info.put("type", "read");
                // 将读到数据对象传入给ClientHandler
                info.put("buffer", buffer);
                clientChannel.read(buffer, info, handler);
            }
        }

        /**
         * 异步调用出现了错误的时候调用这个方法
         *
         * @param exc
         * @param attachment
         */
        @Override
        public void failed(Throwable exc, Object attachment) {
            // 处理错误 
        }
    }

    /**
     * 当客户端Channel执行一些异步操作，异步读写等， 使用ClientHandler来执行回调处理
     */
    private class ClientHandler implements CompletionHandler<Integer, Object> {

        private AsynchronousSocketChannel clientChannel;

        public ClientHandler(AsynchronousSocketChannel clientChannel) {
            this.clientChannel = clientChannel;
        }

        @Override
        public void completed(Integer result, Object attachment) {
            Map<String, Object> info = (Map<String, Object>) attachment;
            String type = (String) info.get("type");
            // 判断完结的操作是读操作还是写操作
            if ("read".equals(type)){
                // 取得从异步客户端通道读取数据的Buffer，此时客户端通道AsyncSocketChannel处于读状态，
                // 但是这个Buffer处于写状态
                ByteBuffer buffer = (ByteBuffer) info.get("buffer");
                // 将Buffer从写状态转换为读状态, 此时是将Buffer中的数据读出来，然后写入到客户端Channel中
                buffer.flip();
                info.put("type", "write");
                // 调用异步写入操作，完成调用时还是调用ClientHandler来处理
                clientChannel.write(buffer, info, this);
                buffer.clear();
            } else if ("write".equals(type)){
                // 服务器已经将之前的数据发回给客户端的时候
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                info.put("type", "read");
                info.put("buffer", buffer);
                clientChannel.read(buffer, info, this);
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {

        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }

}
