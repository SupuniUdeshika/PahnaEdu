package controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dao.UserDAO;
import model.User;
import util.DatabaseConnection;

/**
 * Servlet implementation class ResetPasswordServlet
 */
@WebServlet("/ResetPasswordServlet")
public class ResetPasswordServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ResetPasswordServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	      String token = request.getParameter("token");
	        
	        try (Connection connection = DatabaseConnection.getConnection()) {
	            UserDAO userDAO = new UserDAO(connection);
	            User user = userDAO.getUserByResetToken(token);
	            
	            if (user == null || user.getResetTokenExpiry().before(new Date())) {
	                request.setAttribute("message", "Invalid or expired password reset link.");
	                request.setAttribute("messageType", "alert-danger");
	                request.getRequestDispatcher("/Auth/forgot-password.jsp").forward(request, response);
	                return;
	            }
	            
	            request.setAttribute("token", token);
	            request.getRequestDispatcher("/Auth/reset-password.jsp").forward(request, response);
	            
	        } catch (SQLException e) {
	            e.printStackTrace();
	            request.setAttribute("message", "An error occurred. Please try again.");
	            request.setAttribute("messageType", "alert-danger");
	            request.getRequestDispatcher("/Auth/forgot-password.jsp").forward(request, response);
	        }
	    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		 String token = request.getParameter("token");
	        String newPassword = request.getParameter("newPassword");
	        String confirmPassword = request.getParameter("confirmPassword");
	        
	        if (!newPassword.equals(confirmPassword)) {
	            request.setAttribute("message", "Passwords do not match.");
	            request.setAttribute("messageType", "alert-danger");
	            request.setAttribute("token", token);
	            request.getRequestDispatcher("/Auth/reset-password.jsp").forward(request, response);
	            return;
	        }
	        
	        try (Connection connection = DatabaseConnection.getConnection()) {
	            UserDAO userDAO = new UserDAO(connection);
	            User user = userDAO.getUserByResetToken(token);
	            
	            if (user == null || user.getResetTokenExpiry().before(new Date())) {
	                request.setAttribute("message", "Invalid or expired password reset link.");
	                request.setAttribute("messageType", "alert-danger");
	                request.getRequestDispatcher("/Auth/forgot-password.jsp").forward(request, response);
	                return;
	            }
	            
	            boolean passwordUpdated = userDAO.updatePassword(user.getId(), newPassword);
	            
	            if (passwordUpdated) {
	                request.setAttribute("message", "Password updated successfully. You can now login with your new password.");
	                request.setAttribute("messageType", "alert-success");
	                request.getRequestDispatcher("/Auth/index.jsp").forward(request, response);
	            } else {
	                request.setAttribute("message", "Failed to update password. Please try again.");
	                request.setAttribute("messageType", "alert-danger");
	                request.getRequestDispatcher("/Auth/reset-password.jsp").forward(request, response);
	            }
	            
	        } catch (SQLException e) {
	            e.printStackTrace();
	            request.setAttribute("message", "An error occurred. Please try again.");
	            request.setAttribute("messageType", "alert-danger");
	            request.getRequestDispatcher("/Auth/reset-password.jsp").forward(request, response);
	        }
	    }
	}
