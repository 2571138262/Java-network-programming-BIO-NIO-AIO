package com.baixiaowen.demo.简易版的WebServer.processor;

import com.baixiaowen.demo.简易版的WebServer.connector.Request;
import com.baixiaowen.demo.简易版的WebServer.connector.Response;

import java.io.IOException;

/**
 * 真正的去处理用户发送来的请求并且把请求所对应的Response准备好并返还给用户
 *  这里只负责静态的资源文件
 */
public class StaticProcessor {

    /**
     * 处理请求
     * @param request 接收请求
     * @param response 响应Response
     */
    public void process(Request request, Response response){
        try {
            response.sendStaticResource();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
