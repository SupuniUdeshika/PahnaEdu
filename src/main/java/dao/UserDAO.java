package dao;

import model.User;
import java.sql.*;
import java.util.Date;
import java.util.UUID;

public class UserDAO {
    private Connection connection;

    public UserDAO(Connection connection) {
        this.connection = connection;
    }

    // Add a new user
    public boolean addUser(User user) throws SQLException {
        String sql = "INSERT INTO users (name, email, password, role, verified, verification_token, verification_token_expiry, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPassword());
            statement.setString(4, user.getRole());
            statement.setBoolean(5, user.isVerified());
            statement.setString(6, user.getVerificationToken());
            
            if (user.getVerificationTokenExpiry() != null) {
                statement.setTimestamp(7, new Timestamp(user.getVerificationTokenExpiry().getTime()));
            } else {
                statement.setTimestamp(7, null);
            }
            
            statement.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
            statement.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
            
            return statement.executeUpdate() > 0;
        }
    }

    // Get user by email
    public User getUserByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return extractUserFromResultSet(resultSet);
                }
            }
        }
        return null;
    }

    // Get user by verification token
    public User getUserByVerificationToken(String token) throws SQLException {
        String sql = "SELECT * FROM users WHERE verification_token = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, token);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return extractUserFromResultSet(resultSet);
                }
            }
        }
        return null;
    }

    // Get user by reset token (with expiry check)
    public User getUserByResetToken(String token) throws SQLException {
        String sql = "SELECT * FROM users WHERE reset_token = ? AND reset_token_expiry > NOW()";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, token);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return extractUserFromResultSet(resultSet);
                }
            }
        }
        return null;
    }

    // Update user verification status
    public boolean verifyUser(String token) throws SQLException {
        String sql = "UPDATE users SET verified = true, verification_token = NULL, verification_token_expiry = NULL, updated_at = ? WHERE verification_token = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            statement.setString(2, token);
            return statement.executeUpdate() > 0;
        }
    }

    // Update user password (improved version)
    public boolean updatePassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password = ?, reset_token = NULL, reset_token_expiry = NULL, updated_at = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newPassword);
            statement.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            statement.setInt(3, userId);
            return statement.executeUpdate() > 0;
        }
    }

    // Create password reset token
    public boolean createPasswordResetToken(String email, String token, Date expiryDate) throws SQLException {
        String sql = "UPDATE users SET reset_token = ?, reset_token_expiry = ?, updated_at = ? WHERE email = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, token);
            statement.setTimestamp(2, new Timestamp(expiryDate.getTime()));
            statement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            statement.setString(4, email);
            return statement.executeUpdate() > 0;
        }
    }

    // Helper method to extract user from ResultSet
    private User extractUserFromResultSet(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getInt("id"));
        user.setName(resultSet.getString("name"));
        user.setEmail(resultSet.getString("email"));
        user.setPassword(resultSet.getString("password"));
        user.setRole(resultSet.getString("role"));
        user.setVerified(resultSet.getBoolean("verified"));
        user.setVerificationToken(resultSet.getString("verification_token"));
        
        Timestamp verificationTokenExpiry = resultSet.getTimestamp("verification_token_expiry");
        if (verificationTokenExpiry != null) {
            user.setVerificationTokenExpiry(new Date(verificationTokenExpiry.getTime()));
        }
        
        user.setResetToken(resultSet.getString("reset_token"));
        
        Timestamp resetTokenExpiry = resultSet.getTimestamp("reset_token_expiry");
        if (resetTokenExpiry != null) {
            user.setResetTokenExpiry(new Date(resetTokenExpiry.getTime()));
        }
        
        user.setCreatedAt(new Date(resultSet.getTimestamp("created_at").getTime()));
        user.setUpdatedAt(new Date(resultSet.getTimestamp("updated_at").getTime()));
        
        return user;
    }
}