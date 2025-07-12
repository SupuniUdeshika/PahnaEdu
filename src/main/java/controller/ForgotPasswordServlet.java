package controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dao.UserDAO;
import model.User;
import util.DatabaseConnection;
import util.EmailUtil;

/**
 * Servlet implementation class ForgotPasswordServlet
 */
@WebServlet("/ForgotPasswordServlet")
public class ForgotPasswordServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ForgotPasswordServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.getRequestDispatcher("/Auth/forgot-password.jsp").forward(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
String email = request.getParameter("email");
        
        try (Connection connection = DatabaseConnection.getConnection()) {
            UserDAO userDAO = new UserDAO(connection);
            User user = userDAO.getUserByEmail(email);
            
            if (user == null) {
                request.setAttribute("message", "If an account with this email exists, a password reset link has been sent.");
                request.setAttribute("messageType", "alert-success");
                request.getRequestDispatcher("/Auth/forgot-password.jsp").forward(request, response);
                return;
            }
            
            // Generate reset token
            String resetToken = UUID.randomUUID().toString();
            Date expiryDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000); // 24 hours
            
            // Save token to database
            boolean tokenSaved = userDAO.createPasswordResetToken(email, resetToken, expiryDate);
            
            if (tokenSaved) {
                // Send reset email
            	String resetLink = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + 
                        request.getContextPath() + "/ResetPasswordServlet?token=" + resetToken;
            	
                String subject = "Password Reset Request - Book Management System";
                String content = "Dear " + user.getName() + ",\n\n"
                        + "You have requested to reset your password. Please click the link below to reset your password:\n\n"
                        + resetLink + "\n\n"
                        + "This link will expire in 24 hours.\n\n"
                        + "If you didn't request this, please ignore this email.\n\n"
                        + "Best regards,\n"
                        + "Book Management System Team";
                
                EmailUtil.sendEmail(user.getEmail(), subject, content);
            }
            
            request.setAttribute("message", "If an account with this email exists, a password reset link has been sent.");
            request.setAttribute("messageType", "alert-success");
            request.getRequestDispatcher("/Auth/forgot-password.jsp").forward(request, response);
            
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("message", "An error occurred. Please try again.");
            request.setAttribute("messageType", "alert-danger");
            request.getRequestDispatcher("/Auth/forgot-password.jsp").forward(request, response);
        }
	}

}
