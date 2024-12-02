package cwk2;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

public class AppServer {
    public static void main(String[] args) throws Exception {
        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(AppServlet.class, "/");

        Server server = new Server(8080);
        server.setHandler(handler);

        System.out.println("Starting server on http://localhost:8080...");
        server.start();
        server.join();
    }
}