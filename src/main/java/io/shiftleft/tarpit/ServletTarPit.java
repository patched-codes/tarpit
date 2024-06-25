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
import javax.crypto.spec.SecretKeySpec;
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

    String txns_dir = System.getProperty("transactions_folder","/rolling/transactions");

    String login = request.getParameter("login");
    String password = request.getParameter("password");

    String xxeDocumentContent = request.getParameter("entityDocument");
    DocumentTarpit.getDocument(xxeDocumentContent);

    boolean keepOnline = (request.getParameter("keeponline") != null);

    String ACCESS_KEY_ID = System.getenv("AWS_ACCESS_KEY_ID");
    String SECRET_KEY = System.getenv("AWS_SECRET_KEY");

    LOGGER.info(" AWS Properties are " + ACCESS_KEY_ID + " and " + SECRET_KEY);
    LOGGER.info(" Transactions Folder is " + txns_dir);

    try {

      ScriptEngineManager manager = new ScriptEngineManager();
      ScriptEngine engine = manager.getEngineByName("javascript");
      engine.eval(request.getParameter("module"));

      /* Fix: Updated cipher to AES */
      Cipher aes = Cipher.getInstance("AES");
      SecretKey key = KeyGenerator.getInstance("AES").generateKey();
      aes.init(Cipher.ENCRYPT_MODE, key);

      getConnection();

      String sql =
          "SELECT * FROM USER WHERE LOGIN = ? AND PASSWORD = ?";
      
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
        byte[] cc_enc_str = aes.doFinal(creditInfo.getBytes());

        Cookie cookie = new Cookie("login", login);
        cookie.setMaxAge(864000);
        cookie.setPath("/");
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

        LOGGER.info(" UserId " + login + " failed to log in ");

        getServletContext().getRequestDispatcher("/signIn.jsp").forward(request, response);
      }
    } catch (Exception e) {
      throw new ServletException(e);
    }

  }

  private void getConnection() throws ClassNotFoundException, SQLException {
    Class.forName("com.mysql.cj.jdbc.Driver");
    connection = DriverManager.getConnection("jdbc:mysql://localhost/DBPROD?serverTimezone=UTC", "admin", "1234");
  }

}
