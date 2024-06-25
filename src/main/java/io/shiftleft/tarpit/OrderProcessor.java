package io.shiftleft.tarpit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shiftleft.tarpit.model.Order;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import io.shiftleft.tarpit.util.EmailService;

@WebServlet(name = "simpleServlet", urlPatterns = { "/processOrder" }, loadOnStartup = 1)
public class OrderProcessor extends HttpServlet {

  private static ObjectMapper deserializer = new ObjectMapper();
  private static ObjectMapper serializer = new ObjectMapper();
  private static String uri = "http://mycompany.com";
  private EmailService emailService = new EmailService("smtp.mailtrap.io", 25, "<EMAIL_USERNAME>", "<EMAIL_PASSWORD>");
  private String fromAddress = "orders@mycompany.com";

  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    PrintWriter out = response.getWriter();
    try {
      Order customerOrder = Order.createOrder();
      out.println(serializer.writeValueAsString(customerOrder));

      getConnection();

      Statement statement = connection.createStatement();
      statement.executeUpdate("INSERT INTO Order " +
          "VALUES ('1234','5678', '04/10/2019', 'PENDING', '04/10/2019', 'Lakeside Drive', 'Santa Clara', 'CA', '95054', 'mike@waltz.com')");

      String customerEmail = customerOrder.getEmailAddress();
      String subject = "Transactions Status of Order : " + customerOrder.getOrderId();
      String verifyUri = fromAddress + "/order/" + customerOrder.getOrderId();
      String message = " Your Order was successfully processed. For Order status please verify on page : " +  verifyUri;
      emailService.sendMail(fromAddress, customerEmail, subject, message);

    } catch (Exception e) {
      e.printStackTrace();
    }
    out.close();
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    PrintWriter out = response.getWriter();

    try {
      // read from file, convert it to user class
      Order order = deserializer.readValue(request.getReader(), Order.class);
      out.println(order);
    } catch (IOException e) {
      e.printStackTrace();
    }
    out.close();
  }

  private void getConnection() throws ClassNotFoundException, SQLException {
    Class.forName("com.mysql.jdbc.Driver");
    connection = DriverManager.getConnection("jdbc:mysql://localhost/DBPROD", "admin", "1234");
  }
}
