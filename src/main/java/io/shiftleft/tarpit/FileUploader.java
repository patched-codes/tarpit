package io.shiftleft.tarpit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 * Servlet implementation class FileUploader
 */
@WebServlet("/FileUploader")
@MultipartConfig
public class FileUploader extends HttpServlet {

  private static final long serialVersionUID = 1L;
  private static String productSourceFolder = System.getenv("PRODUCT_SRC_FOLDER");
  private static String productDestinationFolder = System.getenv("PRODUCT_DST_FOLDER");

  /**
   * @see HttpServlet#HttpServlet()
   */
  public FileUploader() {
    super();
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {

      Part filePart = request.getPart("zipFile");
      String fileName = filePart.getSubmittedFileName();

      // removes any path information from the filename
      String sanitizedFileName = sanitizeFileName(fileName);
      if (!isFileNameAllowed(sanitizedFileName)) {
          throw new SecurityException("Invalid file name");
      }

      File targetFile = new File(productSourceFolder + sanitizedFileName);

      IOException exception = null;
      try (InputStream input = filePart.getInputStream();
          OutputStream out = new FileOutputStream(targetFile)) {

          byte[] buffer = new byte[1024];
          int bytesRead;

          while ((bytesRead = input.read(buffer)) != -1) {
              out.write(buffer, 0, bytesRead);
          }

      } catch (IOException e) {
          exception = e;
      }

      if (exception != null) {
          targetFile.delete();
          throw exception;
      }

      Unzipper.unzipFile(targetFile.getAbsolutePath(), productDestinationFolder);

      doGet(request, response);
  }

  private String sanitizeFileName(String fileName) {
        return Paths.get(fileName).getFileName().toString();
  }

  private boolean isFileNameAllowed(String fileName) {
        return fileName.matches("[a-zA-Z0-9._-]+");
  }

}
