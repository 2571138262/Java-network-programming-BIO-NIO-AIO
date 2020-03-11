import com.baixiaowen.demo.简易版的WebServer.connector.ConnectorUtils;
import com.baixiaowen.demo.简易版的WebServer.connector.HttpStatus;

import javax.servlet.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 每次请求过来的时候都把请求的时间返回回去,这个就相当于客户端请求的动态资源
 */
public class TimeServlet implements Servlet {

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {

    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        PrintWriter out = servletResponse.getWriter();
        // 先展示响应台头
        out.println(ConnectorUtils.renderStatus(HttpStatus.SC_OK));
        out.println("what time is it now");
        out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        
    }

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {

    }
}
