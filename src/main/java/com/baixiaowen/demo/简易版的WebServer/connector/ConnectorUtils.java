package com.baixiaowen.demo.简易版的WebServer.connector;

import java.io.File;

/**
 * 连接工具类
 */
public class ConnectorUtils {
    // E:\code\Java网络编程-全面理解BOI-NIO-AIO\Java-network-programming-BIO-NIO-AIO\src\main\resources\static
    public static final String WEB_ROOT = System.getProperty("user.dir") + File.separator + "webroot";
    // 配置Servlet路径
    public static final String SERVLET_WEB_ROOT = System.getProperty("user.dir") + File.separator + "src\\main\\java\\com\\baixiaowen\\demo\\简易版的WebServer\\servlet";
    
    public static final String PROTOCOL = "HTTP/1.1";
    
    public static final String CARRIAGE = "\r";
    
    public static final String NEWLINE = "\n";
    
    public static final String SPACE = " ";

    /**
     * 渲染Response的返回状态
     * @param status
     * @return
     */
    public static String renderStatus(com.baixiaowen.demo.简易版的WebServer.connector.HttpStatus status){
        StringBuilder sb = new StringBuilder(PROTOCOL)
                .append(SPACE)
                .append(status.getStatusCode())
                .append(SPACE)
                .append(status.getReason())
                .append(CARRIAGE).append(NEWLINE)       // 加一个回车符和一个换行符
                .append(CARRIAGE).append(NEWLINE);
        return sb.toString();
    }
}
