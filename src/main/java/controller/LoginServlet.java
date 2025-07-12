package controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import dao.UserDAO;
import model.User;
import util.DatabaseConnection;
import util.EmailUtil;


/**
 * Servlet implementation class LoginServlet
 */
@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public LoginServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		 HttpSession session = request.getSession(false);
	        if (session != null && session.getAttribute("user") != null) {
	            User user = (User) session.getAttribute("user");
	            redirectBasedOnRole(request, response, user);
	            return;
	        }
	        
	        // Rest of your doGet method remains the same...
	        request.getRequestDispatcher("/Auth/index.jsp").forward(request, response);
	    }

	    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	        String email = request.getParameter("email");
	        String password = request.getParameter("password");
	        String rememberMe = request.getParameter("rememberMe");
	        
	        Connection connection = null;
	        try {
	            connection = DatabaseConnection.getConnection();
	            UserDAO userDAO = new UserDAO(connection);
	            User user = userDAO.getUserByEmail(email);
	            
	            if (user == null || !user.getPassword().equals(password)) {
	                request.setAttribute("errorMessage", "Invalid email or password");
	                request.getRequestDispatcher("/Auth/index.jsp").forward(request, response);
	                return;
	            }
	            
	            if (!user.isVerified()) {
	                request.setAttribute("errorMessage", "Account not verified. Please check your email for verification link.");
	                request.getRequestDispatcher("/Auth/index.jsp").forward(request, response);
	                return;
	            }
	            
	            HttpSession session = request.getSession();
	            session.setAttribute("user", user);
	            
	            if ("on".equals(rememberMe)) {
	                createRememberMeCookies(response, user);
	            }
	            
	            sendLoginNotificationEmail(user);
	            redirectBasedOnRole(request, response, user);
	            
	        } catch (SQLException e) {
	            e.printStackTrace();
	            request.setAttribute("errorMessage", "Database connection error. Please try again later.");
	            request.getRequestDispatcher("/Auth/index.jsp").forward(request, response);
	        } finally {
	            if (connection != null) {
	                try {
	                    connection.close();
	                } catch (SQLException e) {
	                    e.printStackTrace();
	                }
	            }
	        }
	    }
	    
	    private void redirectBasedOnRole(HttpServletRequest request, HttpServletResponse response, User user) throws IOException {
	        String contextPath = request.getContextPath();
	        if ("ADMIN".equals(user.getRole())) {
	            response.sendRedirect(contextPath + "/Admin/Admindashboard.jsp");
	        } else {
	            response.sendRedirect(contextPath + "/cashier/dashboard.jsp");
	        }
	    }
	    
	    private void createRememberMeCookies(HttpServletResponse response, User user) {
	        Cookie emailCookie = new Cookie("rememberMeEmail", user.getEmail());
	        emailCookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
	        emailCookie.setHttpOnly(true);
	        emailCookie.setPath("/");
	        
	        Cookie tokenCookie = new Cookie("rememberMeToken", user.getPassword());
	        tokenCookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
	        tokenCookie.setHttpOnly(true);
	        tokenCookie.setPath("/");
	        
	        response.addCookie(emailCookie);
	        response.addCookie(tokenCookie);
	    }
	    
	    private void sendLoginNotificationEmail(User user) {
	        String subject = "Login Notification - Book Management System";
	        String content = "Dear " + user.getName() + ",\n\n"
	                + "You have successfully logged into the Book Management System at " + new Date() + ".\n\n"
	                + "If this was not you, please contact the system administrator immediately.\n\n"
	                + "Best regards,\n"
	                + "Book Management System Team";
	        
	        EmailUtil.sendEmail(user.getEmail(), subject, content);
	    }
	}