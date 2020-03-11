package com.baixiaowen.demo.简易版的WebServer.processor;

import com.baixiaowen.demo.简易版的WebServer.connector.*;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * 真正的去处理用户发送来的请求并且把请求所对应的Response准备好并返还给用户
 *  这里处理动态资源 ---- 这里主要是请求Servlet， 就是把Servlet当中动态资源来请求
 */
public class ServletProcessor {

    /**
     * 处理请求动态资源(Servlet)的函数
     *      对于当前的需求，这个ServletProcessor这个类需要做的其实很简单，就是加载TimeServlet类，通过反射得到TimeServlet类的实例，
     *      之后直接调用service函数就可以了
     *      
     *      但是TimeServlet类和我们之前引用的类是不同的，它不是一个引用的类，
     *      对于Servlet可以用 URLClassLoader 来加载
     * @param request
     * @param response
     */
    public void process(Request request, Response response) throws MalformedURLException {
        // 首先得到对应的ClassLoader
        URLClassLoader loader = getServletLoader();
        try {
            Servlet servlet = getServlet(loader, request);
            // 通过Request和Response这俩个类去创建他们对应的Facade对象
            RequestFacade requestFacade = new RequestFacade(request);
            ResponseFacade responseFacade = new ResponseFacade(response);
            servlet.service(requestFacade, responseFacade);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ServletException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 得到Servlet（动态资源）的URLClassLoader ，方便后面加载这个Servlet          访问权限可以不用开放到public
     * @return
     * @throws MalformedURLException
     */
    public URLClassLoader getServletLoader() throws MalformedURLException {
        // 对应的Servlet所在的路径
        File webroot = new File(ConnectorUtils.WEB_ROOT);
        URL webrootUrl = webroot.toURI().toURL();
        return new URLClassLoader(new URL[]{webrootUrl});
    }

    /**
     * 加载我们要用到的Servlet                                                  访问权限可以不用开放到public
     *      我们从Request中得到的请求Servlet资源的URI 一般格式都是 /servlet/TimeServlet
     * @param loader
     * @param request
     * @return
     */
    public Servlet getServlet(URLClassLoader loader, Request request) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        // Request中Servlet的URI 一般格式为 /servlet/TimeServlet
        String uri = request.getRequestURI();
        // 得到Servlet的名字
        String servletName = uri.substring(uri.lastIndexOf("/") + 1);
        
        Class servletClass = loader.loadClass(servletName);

        Servlet servlet = (Servlet) servletClass.newInstance();

        return servlet;
    }
    
}
