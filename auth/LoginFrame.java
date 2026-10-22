package auth;
import db.DB;
import patient.UserFrame;
import doctor.DoctorFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private String targetRole;

    public LoginFrame(String role) {
        this.targetRole = role;
        setTitle(role + " - Login Portal");
        setSize(380, 260);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel(role.toUpperCase() + " LOGIN", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 15));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Username:"), gbc);
        usernameField = new JTextField(15);
        gbc.gridx = 1; 
        add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Password:"), gbc);
        passwordField = new JPasswordField(15);
        gbc.gridx = 1; 
        add(passwordField, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton loginButton = new JButton("Login");
        btnPanel.add(loginButton);

        if (role.equals("Patient")) {
            JButton signUpButton = new JButton("Sign Up");
            btnPanel.add(signUpButton);
            signUpButton.addActionListener(e -> openPatientSignUpDialog());
        }

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        add(btnPanel, gbc);

        loginButton.addActionListener(this::handleLogin);
    }

    private void handleLogin(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DB.connect()) {
            if (conn == null) {
                JOptionPane.showMessageDialog(this, "Database connection failed. Check your MySQL server.", "Connection Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String query = "SELECT * FROM users WHERE username = ? AND password = ? AND role = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                pstmt.setString(3, targetRole);
                
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    String name = rs.getString("name");
                    int patientId = 0;

                    if (targetRole.equals("Patient")) {
                        try (PreparedStatement pStmt = conn.prepareStatement("SELECT id FROM patients WHERE name = ?")) {
                            pStmt.setString(1, name);
                            try (ResultSet pRs = pStmt.executeQuery()) {
                                if (pRs.next()) {
                                    patientId = pRs.getInt("id");
                                }
                            }
                        }
                    }

                    JOptionPane.showMessageDialog(this, "Login Successful!");
                    dispose();

                    switch (targetRole) {
                        case "Admin":
                            new AdminFrame().setVisible(true);
                            break;
                        case "Desk Staff":
                            new staff.StaffFrame().setVisible(true);
                            break;
                        case "Doctor":
                            new DoctorFrame(name).setVisible(true);
                            break;
                        case "Patient":
                            new UserFrame(username, patientId).setVisible(true);
                            break;
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid credentials or role mismatch.", "Access Denied", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openPatientSignUpDialog() {
        JDialog signUpDialog = new JDialog(this, "Patient Sign-Up", true);
        signUpDialog.setSize(350, 300);
        signUpDialog.setLocationRelativeTo(this);
        signUpDialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField regUser = new JTextField(15);
        JPasswordField regPass = new JPasswordField(15);
        JTextField regName = new JTextField(15);
        JTextField regPhone = new JTextField(15);

        gbc.gridx = 0; gbc.gridy = 0; signUpDialog.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1; signUpDialog.add(regName, gbc);

        gbc.gridx = 0; gbc.gridy = 1; signUpDialog.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; signUpDialog.add(regUser, gbc);

        gbc.gridx = 0; gbc.gridy = 2; signUpDialog.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; signUpDialog.add(regPass, gbc);

        gbc.gridx = 0; gbc.gridy = 3; signUpDialog.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1; signUpDialog.add(regPhone, gbc);

        JButton submitBtn = new JButton("Register Account");
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        signUpDialog.add(submitBtn, gbc);

        submitBtn.addActionListener(e -> {
            String u = regUser.getText().trim();
            String p = new String(regPass.getPassword()).trim();
            String n = regName.getText().trim();
            String ph = regPhone.getText().trim();

            if (u.isEmpty() || p.isEmpty() || n.isEmpty()) {
                JOptionPane.showMessageDialog(signUpDialog, "Name, Username, and Password are required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try (Connection conn = DB.connect();
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users (username, password, name, phone, role) VALUES (?, ?, ?, ?, 'Patient')")) {
                pstmt.setString(1, u);
                pstmt.setString(2, p);
                pstmt.setString(3, n);
                pstmt.setString(4, ph);
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(signUpDialog, "Sign-up successful! You can now log in.");
                signUpDialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(signUpDialog, "Error signing up: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        signUpDialog.setVisible(true);
    }
}