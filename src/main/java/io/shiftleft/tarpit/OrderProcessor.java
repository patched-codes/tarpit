// ...
PrintWriter out = response.getWriter();
try {
    Order customerOrder = Order.createOrder();
    out.println(serializer.writeValueAsString(customerOrder));

    getConnection();

    Statement statement = connection.createStatement();
    statement.executeUpdate("INSERT INTO Order " +
        "VALUES ('12','56', '04/10/2019', 'PENDING', '04/10/2019', 'Lakeside Drive', 'Santa Clara', 'CA', '95054', 'mike@waltz.com')");

    String customerEmail = customerOrder.getEmailAddress();
    String subject = "Transactions Status of Order : " + customerOrder.getOrderId();
    String verifyUri = fromAddress + "/order/" + customerOrder.getOrderId();
    String message = " Your Order was successfully processed. For Order status please verify on page : " +  verifyUri;
    emailService.sendMail(fromAddress, customerEmail, subject, message);

} catch (JsonGenerationException e) {
    e.printStackTrace();
} catch (JsonMappingException e) {
    e.printStackTrace();
} catch (IOException e) {
    e.printStackTrace();
} catch (ParseException e) {
    e.printStackTrace();
} catch (Exception e) {
    e.printStackTrace();
}

// HTML attributes encoding
String customerEmailEncoded = java.net.URLEncoder.encode(customerEmail, "UTF-8");
String verifyUriEncoded = java.net.URLEncoder.encode(verifyUri, "UTF-8");
String messageEncoded = java.net.URLEncoder.encode(message, "UTF-8");

out.println("<a href='" + verifyUriEncoded + "'>Verify Order Status</a>");
out.println("<p>Your Order was successfully processed. For Order status please verify on page : <a href='" + verifyUriEncoded + "'>here</a>");
out.println("<p>Your Order Email : " + customerEmailEncoded + "</p>");
out.close();

// ...
