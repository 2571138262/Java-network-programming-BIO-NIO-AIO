package com.baixiaowen.demo.简易版的WebServer.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * 测试简易版WebServer（只访问静态资源）的客户端模拟器
 */
public class TestClient {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 8888);
        OutputStream output = socket.getOutputStream();
        // 模拟客户端发送请求
        
        // 请求静态资源
//        output.write("GET /index.html HTTP/1.1".getBytes());
        // 请求动态资源
        output.write("GET /servlet/TimeServlet HTTP/1.1".getBytes());
        
        // 发送完请求之后关闭output流
        socket.shutdownOutput();
        
        // 发送完请求就会得到客户端的响应， 此时使用InputStream流来读取客户端的响应
        InputStream input = socket.getInputStream();
        byte[] buffer = new byte[2048];
        int length = input.read(buffer);
        StringBuilder response = new StringBuilder();
        for (int i = 0; i < length; i++) {
            response.append((char) buffer[i]);
        }
        System.out.println(response.toString());
        // 响应完之后关闭output
        socket.shutdownInput();
        
        // 关闭客户端
        socket.close();
    }
}
