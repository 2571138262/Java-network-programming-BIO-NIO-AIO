package com.baixiaowen.demo.使用非阻塞式NIO编程模型实现多人聊天室.client;


import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Set;

public class ChatClient {

    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private static final int DEFAULT_SERVER_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;

    private String host;
    private int port;
    private SocketChannel client;
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);
    private Selector selector;
    private Charset charset = Charset.forName("UTF-8");

    public ChatClient() {
        this(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
    }

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean readyToQuit(String msg) {
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
            // 创建客户端的通道
            client = SocketChannel.open();
            // 将通道修改为非阻塞调用模型 --- NIO模型
            client.configureBlocking(false);

            // 实例化Selector
            selector = Selector.open();

            // 在Selector上注册客户端SocketChannel， 让它监听CONNECT状态
            client.register(selector, SelectionKey.OP_CONNECT);

            // 将当前通道向服务器通道发送链接请求
            client.connect(new InetSocketAddress(host, port));

            while (true) {
                // 让Selector不停的监听通道上有哪些事件被触发
                selector.select();
                // 一旦select()函数返回了，我们就可以得到一个被触发的事件的集合
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey key : selectionKeys) {
                    // 处理被触发的SelectionKey事件信息
                    handles(key);
                }
                // 当被触发的信息都被处理之后，需要将集合清空
                selectionKeys.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClosedSelectorException e){
            // 用户正常退出
        } finally {
            close(selector);
        }
    }

    /**
     * 处理被触发的SelectionKey事件信息
     *
     * @param key
     */
    private void handles(SelectionKey key) throws IOException {
        // CONNECT事件   ---   连接就绪事件
        if (key.isConnectable()) {
            // 取得当前SelectionKey事件所对应的客户端Channel
            SocketChannel client = (SocketChannel) key.channel();
            // 查看当前通道和服务器通道连接是否是就绪
            if (client.isConnectionPending()){
                // 如果连接准备就绪，就finish来正式建立连接
                client.finishConnect();
                // 处理用户输入的信息
                new Thread(new UserInputHandler(this)).start();
            }
            // 将当前客户端通道的READ事件注册到Selector上
            client.register(selector, SelectionKey.OP_READ);
        }

        // READ事件      ---   一旦服务器转发一条其它客户端信息，客户端就会触发READ事件，并将这条消息显示出来
        else if (key.isReadable()){
            SocketChannel client = (SocketChannel) key.channel();
            // 从客户端通道中读取数据
            String msg = receive(client);
            if (msg.isEmpty()){
                // 服务器异常
                close(selector);
            } else {
                System.out.println(msg);
            }
        }
    }

    /**
     * 通过客户端通道读取服务器端通道发送来的信息
     * @param client
     * @return
     */
    private String receive(SocketChannel client) throws IOException {
        // 清空rBuffer中所有的数据
        rBuffer.clear();
        while (client.read(rBuffer) > 0);
        // 将Buffer从写模式转换为读模式
        rBuffer.flip();
        // 将Buffer中的信息读出来
        return String.valueOf(charset.decode(rBuffer));
    }

    /**
     * 把消息从客户端发送到服务端了
     * @param msg
     */
    public void send(String msg) throws IOException {
        if (msg.isEmpty()){
            return;
        }
        
        // 清空wBuffer上残留的数据
        wBuffer.clear();
        // 把要发送的数据写入到wBuffer中
        wBuffer.put(charset.encode(msg));
        // 将wBuffer从写模式转换为读模式
        wBuffer.flip();
        // 只要是还能从wBuffer中读到数据，就一直读
        while (wBuffer.hasRemaining()){
            // 然后把从wBuffer中读到的数据写入到客户端Channel中，以便让客户端通道将这一部分消息发送到服务端
            client.write(wBuffer);
        }
        
        // 检查用户是否准备退出
        if (readyToQuit(msg)){
            close(selector);
        }
    }
    
    public static void main(String[] args) {
        ChatClient client = new ChatClient("127.0.0.1", 7777);
        client.start();
    }

}
