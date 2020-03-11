package com.baixiaowen.demo.简易版的WebServer.util;

import com.baixiaowen.demo.简易版的WebServer.connector.Request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestUtils {

    /**
     * 通过客户端传入的请求字符串得到客户端请求的Request
     * @param requestStr
     * @return
     */
    public static Request createRequest(String requestStr){
        // 创建Request需要的客户端输入流InputStream
        InputStream input = new ByteArrayInputStream(requestStr.getBytes());
        Request request = new Request(input);
        request.parse();
        return request;
    }

    /**
     * 从文件中读出所有已的字节，返回字符串
     * @param fileName
     * @return
     * @throws IOException
     */
    public static String readFileToString(String fileName) throws IOException {
        return new String(Files.readAllBytes(Paths.get(fileName)));
    }
    
}
