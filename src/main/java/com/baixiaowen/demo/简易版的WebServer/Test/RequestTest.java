package com.baixiaowen.demo.简易版的WebServer.Test;



import com.baixiaowen.demo.简易版的WebServer.connector.Request;
import com.baixiaowen.demo.简易版的WebServer.util.TestUtils;

public class RequestTest {
    private static final String validRequest = "GET /index.html HTTP/1.1";
    
    public static void main(String[] args){
        Request request = TestUtils.createRequest(validRequest);
        if ("/index.html".equals(request.getRequestURI())){
            System.out.println("获取URI成功");
        }
    }
    
    
}
