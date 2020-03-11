package com.baixiaowen.demo.简易版的WebServer.connector;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * Response 的门面模式的体现
 *      让Servlet 的开发人员只能调用ResponseFacade，这样他们就不能调用Response中的服务端内部函数
 *
 *
 *      因为Servlet本身属于动态资源，是由用户自定义的，不属于服务端服务，
 *      所以如果用户了解到这个继承关系，就可以向下转型，从而调用了这个服务端的方法，那么在以后的迭代更新中都是会影响到用户的
 *      对用用户自定义的Servlet，本身是通过ServletRequest这种接口方式为其提供服务，所以这里需要进行优化，保证用户不可以调用调服务端的内部服务方法
 *      ------ 门面模式（外观模式） facade 
 */
public class ResponseFacade implements ServletResponse {

    private ServletResponse response = null;

    public ResponseFacade(Response response) {
        this.response = response;
    }

    @Override
    public String getCharacterEncoding() {
        return response.getCharacterEncoding();
    }

    @Override
    public String getContentType() {
        return response.getContentType();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return response.getOutputStream();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return response.getWriter();
    }

    @Override
    public void setCharacterEncoding(String s) {
        response.setCharacterEncoding(s);
    }

    @Override
    public void setContentLength(int i) {
        response.setContentLength(i);
    }

    @Override
    public void setContentLengthLong(long l) {
        response.setContentLengthLong(l);
    }

    @Override
    public void setContentType(String s) {
        response.setContentType(s);
    }

    @Override
    public void setBufferSize(int i) {
        response.setBufferSize(i);
    }

    @Override
    public int getBufferSize() {
        return response.getBufferSize();
    }

    @Override
    public void flushBuffer() throws IOException {
        response.flushBuffer();
    }

    @Override
    public void resetBuffer() {
        response.resetBuffer();
    }

    @Override
    public boolean isCommitted() {
        return response.isCommitted();
    }

    @Override
    public void reset() {
        response.reset();
    }

    @Override
    public void setLocale(Locale locale) {
        response.setLocale(locale);
    }

    @Override
    public Locale getLocale() {
        return response.getLocale();
    }
}
