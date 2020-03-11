package com.baixiaowen.demo.简易版的WebServer.Test;

import com.baixiaowen.demo.简易版的WebServer.connector.Request;
import com.baixiaowen.demo.简易版的WebServer.processor.ServletProcessor;
import com.baixiaowen.demo.简易版的WebServer.util.TestUtils;

import javax.servlet.Servlet;
import java.net.MalformedURLException;
import java.net.URLClassLoader;

/**
 * 测试请求动态资源(Servlet)， 真正处理请求的Processor
 */
public class PorcessorTest {

    private static final String servletRequest = "GET /servlet/TimeServlet HTTP/1.1";
    
    private static void givenServletRequest_thenLoadServlet() throws MalformedURLException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        // 创建客户端请求Request
        Request request = TestUtils.createRequest(servletRequest);
        ServletProcessor processor = new ServletProcessor();
        URLClassLoader loader = processor.getServletLoader();
        Servlet servlet = processor.getServlet(loader, request);
        
        if ("TimeServlet".equals(servlet.getClass().getName())){
            System.out.println("成功访问到动态资源Servlet");
        }
    }
    
    public static void main(String[] args) throws ClassNotFoundException, MalformedURLException, InstantiationException, IllegalAccessException {
        givenServletRequest_thenLoadServlet();
    }
    
}
