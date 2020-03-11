package com.baixiaowen.demo.简易版的WebServer.connector;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import java.io.*;
import java.util.Locale;

/**
 * HTTP/1.1 200 OK
 */
public class Response implements ServletResponse {

    private static final int BUFFER_SIZE = 1024;

    com.baixiaowen.demo.简易版的WebServer.connector.Request request;
    //Socket所对应的OutputStream，这样就可以通过OutputStream向客户端发送数据
    OutputStream output;

    public Response(OutputStream output) {
        this.output = output;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    /**
     * 响应客户请求（把资源响应给客户端）
     */
    public void sendStaticResource() throws IOException {
        File file = new File(ConnectorUtils.WEB_ROOT, request.getRequestURI());
        try {
            // 请求成功
            write(file, HttpStatus.SC_OK);
        } catch (IOException e) {
            // 请求失败，返回404
            write(new File(ConnectorUtils.WEB_ROOT, "404.html"), HttpStatus.SC_NOT_FOUND);
        }
    }

    /**
     * 将静态资源响应给客户端
     */
    private void write(File resource, HttpStatus status) throws IOException {
        // Java 8 的语法糖 try with resource
        // 如果有一个系统资源我们是需要使用的，但是最终是要把它关闭的， 
        // 使用try-with-resources优雅关闭资源，在使用完成以后，java会自动帮我们关闭这些资源
        try (FileInputStream fis = new FileInputStream(resource)) {
            output.write(ConnectorUtils.renderStatus(status).getBytes());
            byte[] buffer = new byte[BUFFER_SIZE];
            int length = 0;
            while ((length = fis.read(buffer, 0, BUFFER_SIZE)) != -1) {
                output.write(buffer, 0, length);
            }
        }
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return null;
    }

    /**
     * 重写这个方法可以方便将动态资源写入到Socket的output中
     *          进行一层包装
     * @return
     * @throws IOException
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        // 这里用到IO装饰者模式， 用PrintWrite包装output向客户端响应资源
        // autoFlush 参数代表当我们向响应流中写入数据，会自动刷新， 但并不是所有的写入都会刷新 
        // printLine()会自动刷新， print不会自动刷新
        PrintWriter writer = new PrintWriter(output, true);
        return writer;
    }

    @Override
    public void setCharacterEncoding(String s) {

    }

    @Override
    public void setContentLength(int i) {

    }

    @Override
    public void setContentLengthLong(long l) {

    }

    @Override
    public void setContentType(String s) {

    }

    @Override
    public void setBufferSize(int i) {

    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public void flushBuffer() throws IOException {

    }

    @Override
    public void resetBuffer() {

    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {

    }

    @Override
    public void setLocale(Locale locale) {

    }

    @Override
    public Locale getLocale() {
        return null;
    }
}
