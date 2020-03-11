package com.baixiaowen.demo.异步调用机制EasyDemo实现回音壁效果;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Client {
    
    final String LOCALHOST = "127.0.0.1";
    final int DEFAULT_PORT = 8888;
    // 客户端异步通道
    AsynchronousSocketChannel clientChannel;
    
    private void close(Closeable closeable){
        if (closeable != null){
            try {
                closeable.close();
                System.out.println("关闭" + closeable);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void start(){
        // 创建客户端AsynchronousChannel
        try {
            clientChannel = AsynchronousSocketChannel.open();
            // 返回Future对象
            Future<Void> future = clientChannel.connect(new InetSocketAddress(LOCALHOST,DEFAULT_PORT));
            future.get();
            
            // 等待用户的输入
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            while (true){
                String input = consoleReader.readLine();
                
                // 将用户输入信息写入到客户端的AsynchronousChannel中去
                byte[] inputBytes = input.getBytes();
                ByteBuffer buffer = ByteBuffer.wrap(inputBytes);
                // 调用客户端Channel的write方法，此时Buffer处于读模式
                Future<Integer> writeResult = clientChannel.write(buffer);
                // 阻塞式调用，等待clientChannel.write(buffer)成功返回，说明客户端的数据成功的写入到了ClientChannel中
                writeResult.get();
                // 将Buffer从读模式装换为写模式
                buffer.flip();
                Future<Integer> readResult = clientChannel.read(buffer);
                
                // 阻塞式调用，等待clientChannel.read(buffer)执行完毕，说明客户端ClientChannel成功从服务端接收到了数据
                readResult.get();
                // 得到服务器中返回的数据
                String echo = new String(buffer.array());
                System.out.println(echo);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e){
            e.printStackTrace();
        } catch (ExecutionException e){
            e.printStackTrace();
        } finally {
            close(clientChannel
            );
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }
    
}
