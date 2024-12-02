package cwk2;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@SuppressWarnings("serial")
public class AppServlet extends HttpServlet {

    private static final String CONNECTION_URL = "jdbc:sqlite:db.sqlite3";
    private final Configuration fm = new Configuration(Configuration.VERSION_2_3_31);
    private Connection database;

    @Override
    public void init() throws ServletException {
        configureTemplateEngine();
        connectToDatabase();
    }

    private void configureTemplateEngine() throws ServletException {
        try {
            fm.setDirectoryForTemplateLoading(new File("./templates"));
            fm.setDefaultEncoding("UTF-8");
            fm.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            fm.setLogTemplateExceptions(false);
            fm.setWrapUncheckedExceptions(true);
        } catch (IOException e) {
            throw new ServletException("Failed to configure template engine", e);
        }
    }

    private void connectToDatabase() throws ServletException {
        try {
            database = DriverManager.getConnection(CONNECTION_URL);
        } catch (SQLException e) {
            throw new ServletException("Failed to connect to database", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            Template template = fm.getTemplate("login.html");
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            template.process(null, response.getWriter());
        } catch (TemplateException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Template processing error");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String surname = request.getParameter("surname");

        try {
            if (authenticated(username, password)) {
                Map<String, Object> model = new HashMap<>();
                model.put("records", searchResults(surname));

                Template template = fm.getTemplate("details.html");
                response.setContentType("text/html");
                response.setStatus(HttpServletResponse.SC_OK);
                template.process(model, response.getWriter());
            } else {
                Template template = fm.getTemplate("invalid.html");
                response.setContentType("text/html");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                template.process(null, response.getWriter());
            }
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    private boolean authenticated(String username, String password) throws SQLException {
        String query = "SELECT password FROM user WHERE username = ?";

        try (PreparedStatement stmt = database.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password");
                String hashedInput = hashPassword(password);
                return storedHash.equals(hashedInput);
            }
        }

        return false;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not available", e);
        }
    }

    private List<Record> searchResults(String surname) throws SQLException {
        String query = "SELECT * FROM patient WHERE surname LIKE ? COLLATE NOCASE";

        try (PreparedStatement stmt = database.prepareStatement(query)) {
            stmt.setString(1, surname + "%");
            ResultSet rs = stmt.executeQuery();

            List<Record> records = new ArrayList<>();
            while (rs.next()) {
                Record record = new Record();
                record.setSurname(rs.getString("surname"));
                record.setForename(rs.getString("forename"));
                record.setAddress(rs.getString("address"));
                record.setDateOfBirth(rs.getString("dateOfBirth"));
                record.setDoctorId(rs.getString("doctorId"));
                record.setDiagnosis(rs.getString("diagnosis"));
                records.add(record);
            }
            return records;
        }
    }

    @Override
    public void destroy() {
        try {
            if (database != null && !database.isClosed()) {
                database.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
