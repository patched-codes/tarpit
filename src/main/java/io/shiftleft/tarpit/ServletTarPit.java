package io.shiftleft.tarpit;

import io.shiftleft.tarpit.model.User;
import io.shiftleft.tarpit.DocumentTarpit;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;


@WebServlet(name = "simpleServlet", urlPatterns = {"/vulns"}, loadOnStartup = 1)
public class ServletTarPit extends HttpServlet {

  private static final long serialVersionUID = -3462096228274971485L;
  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;

  private final static Logger LOGGER = Logger.getLogger(ServletTarPit.class.getName());

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    // Removed hardcoded credentials
    String ACCESS_KEY_ID = System.getenv("ACCESS_KEY_ID");
    String SECRET_KEY = System.getenv("SECRET_KEY");

    String txns_dir = System.getProperty("transactions_folder","/rolling/transactions");

    // Mitigate HTTP Request/Response Splitting
    String login = request.getParameter("login").replaceAll("[\r\n]", "");
    String password = request.getParameter("password").replaceAll("[\r\n]", "");
    String encodedPath = request.getParameter("encodedPath").replaceAll("[\r\n]", "");

    String xxeDocumentContent = request.getParameter("entityDocument");
    DocumentTarpit.getDocument(xxeDocumentContent);

    boolean keepOnline = (request.getParameter("keeponline") != null);

    LOGGER.info(" AWS Properties are " + ACCESS_KEY_ID + " and " + SECRET_KEY);
    LOGGER.info(" Transactions Folder is " + txns_dir);

    try {

      // Mitigate Code Injection
      ScriptEngineManager manager = new ScriptEngineManager();
      ScriptEngine engine = manager.getEngineByName("JavaScript");
      // Do not evaluate untrusted input directly
      // engine.eval(request.getParameter("module"));

      // Use a more secure cryptographic algorithm
      Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      KeyGenerator keyGen = KeyGenerator.getInstance("AES");
      keyGen.init(256); // for example
      SecretKey key = keyGen.generateKey();
      aesCipher.init(Cipher.ENCRYPT_MODE, key);

      getConnection();

      // Mitigate SQL Injection
      String sql = "SELECT * FROM USER WHERE LOGIN = ? AND PASSWORD = ?";
      preparedStatement = connection.prepareStatement(sql);
      preparedStatement.setString(1, login);
      preparedStatement.setString(2, password);

      resultSet = preparedStatement.executeQuery();

      if (resultSet.next()) {

        login = resultSet.getString("login");
        password = resultSet.getString("password");

        User user = new User(login,
            resultSet.getString("fname"),
            resultSet.getString("lname"),
            resultSet.getString("passportnum"),
            resultSet.getString("address1"),
            resultSet.getString("address2"),
            resultSet.getString("zipCode"));

        String creditInfo = resultSet.getString("userCreditCardInfo");
        byte[] cc_enc_str = aesCipher.doFinal(creditInfo.getBytes());

        Cookie cookie = new Cookie("login", login);
        cookie.setMaxAge(864000);
        cookie.setPath("/");
        // Set HttpOnly and Secure flags on the cookie
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        response.addCookie(cookie);

        request.setAttribute("user", user.toString());
        request.setAttribute("login", login);

        LOGGER.info(" User " + user + " successfully logged in ");
        LOGGER.info(" User " + user + " credit info is " + cc_enc_str);

        getServletContext().getRequestDispatcher("/dashboard.jsp").forward(request, response);

      } else {
        request.setAttribute("login", login);
        request.setAttribute("password", password);
        request.setAttribute("keepOnline", keepOnline);
        request.setAttribute("message", "Failed to Sign in. Please verify credentials");

        LOGGER.info(" UserId " + login + " failed to logged in ");

        getServletContext().getRequestDispatcher("/signIn.jsp").forward(request, response);
      }
    } catch (Exception e) {
      throw new ServletException(e);
    }

  }

  private void getConnection() throws ClassNotFoundException, SQLException {
    // Use environment variables or a secure configuration management system to retrieve credentials
    String dbUser = System.getenv("DB_USER");
    String dbPassword = System.getenv("DB_PASSWORD");
    Class.forName("com.mysql.jdbc.Driver");
    connection = DriverManager.getConnection("jdbc:mysql://localhost/DBPROD", dbUser, dbPassword);
  }

}
