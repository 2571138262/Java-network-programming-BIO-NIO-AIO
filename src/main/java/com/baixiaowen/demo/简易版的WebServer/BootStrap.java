package com.baixiaowen.demo.简易版的WebServer;

import com.baixiaowen.demo.简易版的WebServer.connector.Connector;

/**
 * 启动这个简易的WebServer的类
 *      最简单的WebServer服务器设计成功， 只能请求静态资源
 */
public class BootStrap {

    public static void main(String[] args) {
        Connector connector = new Connector();
        connector.start();
    }
    
}
