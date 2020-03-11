package com.baixiaowen.demo.简易版的WebServer.Test;

import com.baixiaowen.demo.简易版的WebServer.connector.ConnectorUtils;
import com.baixiaowen.demo.简易版的WebServer.connector.Request;
import com.baixiaowen.demo.简易版的WebServer.connector.Response;
import com.baixiaowen.demo.简易版的WebServer.util.TestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ResponseTest {

    private static final String validRequest = "GET /index.html HTTP/1.1";
    
    private static final String invalidRequest = "GET /notfound.html HTTP/1.1";
    
    private static final String staus200 = "HTTP/1.1 200 OK\r\n\r\n";
    
    private static final String status404 = "HTTP/1.1 404 File Not Found\r\n\r\n";

    /**
     * 返回有效的资源
     */
    public static void givenValidRequest_thenReturnStaticResource() throws IOException {
        Request request = TestUtils.createRequest(validRequest);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Response response = new Response(out);
        response.setRequest(request);
        response.sendStaticResource();
        
        String resource = TestUtils.readFileToString(ConnectorUtils.WEB_ROOT + request.getRequestURI());
        if ((staus200 + resource).equals(out.toString())){
            System.out.println("成功请求到有效的静态资源");
        }
    }

    /**
     * 返回无效的资源
     */
    public static void givenInvalidRequest_thenReturnError() throws IOException {
        Request request = TestUtils.createRequest(invalidRequest);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Response response = new Response(out);
        response.setRequest(request);
        response.sendStaticResource();

        String resource = TestUtils.readFileToString(ConnectorUtils.WEB_ROOT + "/404.html");
        if ((status404 + resource).equals(out.toString())){
            System.out.println("请求的静态资源不存在");
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("请求有效资源测试 ------------------- ");
        givenValidRequest_thenReturnStaticResource();
        System.out.println("请求无效资源测试 ------------------- ");
        givenInvalidRequest_thenReturnError();
    }
    
}
