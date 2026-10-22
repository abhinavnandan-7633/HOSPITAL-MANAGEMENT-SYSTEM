package auth;
import db.DB;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;

public class AdminFrame extends JFrame {
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField usernameField, passwordField, nameField;
    private JComboBox<String> roleField;
    private JTextField idRemoveField;

    public AdminFrame() {
        setTitle("Admin Control Panel - Hospital Management System");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JLabel headerLabel = new JLabel("System Administration Dashboard", JLabel.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 18));
        add(headerLabel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"ID", "Username", "Role"}, 0);
        table = new JTable(tableModel);
        loadUserData();
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new GridLayout(2, 1, 5, 5));

        JPanel addPanel = new JPanel(new FlowLayout());
        addPanel.setBorder(BorderFactory.createTitledBorder("Add New Personnel / User"));
        
        usernameField = new JTextField(8);
        passwordField = new JTextField(8);
        nameField = new JTextField(10);
        roleField = new JComboBox<>(new String[]{"Admin", "Desk Staff", "Doctor", "Patient"});
        JButton addButton = new JButton("Add User");

        addPanel.add(new JLabel("User:"));
        addPanel.add(usernameField);
        addPanel.add(new JLabel("Pass:"));
        addPanel.add(passwordField);
        addPanel.add(new JLabel("Name:"));
        addPanel.add(nameField);
        addPanel.add(new JLabel("Role:"));
        addPanel.add(roleField);
        addPanel.add(addButton);

        JPanel removePanel = new JPanel(new FlowLayout());
        removePanel.setBorder(BorderFactory.createTitledBorder("Remove Personnel by ID"));
        
        idRemoveField = new JTextField(5);
        JButton removeButton = new JButton("Delete User ID");

        removePanel.add(new JLabel("User ID:"));
        removePanel.add(idRemoveField);
        removePanel.add(removeButton);

        controlPanel.add(addPanel);
        controlPanel.add(removePanel);
        add(controlPanel, BorderLayout.SOUTH);

        addButton.addActionListener(this::handleAddUser);
        removeButton.addActionListener(this::handleRemoveUser);
    }

    private void loadUserData() {
        tableModel.setRowCount(0);
        try (Connection conn = DB.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT patient_id, username, role FROM users")) {
             
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("patient_id"),
                    rs.getString("username"),
                    rs.getString("role")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load user data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleAddUser(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String name = nameField.getText().trim();
        String role = (String) roleField.getSelectedItem();

        if (username.isEmpty() || password.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username, Password, and Name cannot be empty.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection conn = DB.connect();
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users (username, password, name, role) VALUES (?, ?, ?, ?)")) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, name);
            pstmt.setString(4, role);
            pstmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "User added successfully!");
            usernameField.setText("");
            passwordField.setText("");
            nameField.setText("");
            loadUserData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error adding user: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRemoveUser(ActionEvent e) {
        String idText = idRemoveField.getText().trim();
        if (idText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a valid User ID to delete.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int userId = Integer.parseInt(idText);
            try (Connection conn = DB.connect();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM users WHERE patient_id = ?")) {
                
                pstmt.setInt(1, userId);
                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(this, "User ID " + userId + " removed successfully.");
                    idRemoveField.setText("");
                    loadUserData();
                } else {
                    JOptionPane.showMessageDialog(this, "User ID not found.", "Not Found", JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "User ID must be a number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error removing user: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}