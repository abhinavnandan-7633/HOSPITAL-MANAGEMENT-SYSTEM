package staff;

import db.DB;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.ArrayList;

public class StaffFrame extends JFrame {

    private JTextField regNameField, regAgeField, regPhoneField;
    private JComboBox<String> regGenderBox;
    
    private JTextField tokenPatientIdField;
    private JComboBox<String> tokenDoctorBox, tokenSessionBox;

    private JTextField doctorSearchField;
    private JTable doctorTable;
    private DefaultTableModel doctorTableModel;

    private JTable queueTable;
    private DefaultTableModel queueTableModel;

    public StaffFrame() {
        setTitle("Hospital Management System - Reception Desk Portal");
        setSize(750, 550);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Register & Generate Token", createRegistrationAndTokenPanel());
        tabbedPane.addTab("Doctor Directory", createDoctorDirectoryPanel());
        tabbedPane.addTab("Live OPD Queue", createLiveQueuePanel());

        add(tabbedPane);
    }

    private JPanel createRegistrationAndTokenPanel() {
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel regPanel = new JPanel(new GridBagLayout());
        regPanel.setBorder(BorderFactory.createTitledBorder("1. Register Walk-in Patient"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; regPanel.add(new JLabel("Full Name:"), gbc);
        regNameField = new JTextField(12);
        gbc.gridx = 1; regPanel.add(regNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; regPanel.add(new JLabel("Age:"), gbc);
        regAgeField = new JTextField(12);
        gbc.gridx = 1; regPanel.add(regAgeField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; regPanel.add(new JLabel("Gender:"), gbc);
        regGenderBox = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        gbc.gridx = 1; regPanel.add(regGenderBox, gbc);

        gbc.gridx = 0; gbc.gridy = 3; regPanel.add(new JLabel("Phone Number:"), gbc);
        regPhoneField = new JTextField(12);
        gbc.gridx = 1; regPanel.add(regPhoneField, gbc);

        JButton registerBtn = new JButton("Register Patient");
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        regPanel.add(registerBtn, gbc);
        registerBtn.addActionListener(this::handleRegistration);

        JPanel tokenPanel = new JPanel(new GridBagLayout());
        tokenPanel.setBorder(BorderFactory.createTitledBorder("2. Generate OPD Token"));
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(5, 5, 5, 5);
        gbc2.fill = GridBagConstraints.HORIZONTAL;

        gbc2.gridx = 0; gbc2.gridy = 0; tokenPanel.add(new JLabel("Patient ID:"), gbc2);
        tokenPatientIdField = new JTextField(12);
        gbc2.gridx = 1; tokenPanel.add(tokenPatientIdField, gbc2);

        gbc2.gridx = 0; gbc2.gridy = 1; tokenPanel.add(new JLabel("Select Doctor:"), gbc2);
        tokenDoctorBox = new JComboBox<>();
        loadActiveDoctors(tokenDoctorBox);
        gbc2.gridx = 1; tokenPanel.add(tokenDoctorBox, gbc2);

        gbc2.gridx = 0; gbc2.gridy = 2; tokenPanel.add(new JLabel("Session:"), gbc2);
        tokenSessionBox = new JComboBox<>(new String[]{"Morning", "Evening"});
        gbc2.gridx = 1; tokenPanel.add(tokenSessionBox, gbc2);

        JButton generateBtn = new JButton("Generate Token");
        gbc2.gridx = 0; gbc2.gridy = 3; gbc2.gridwidth = 2;
        tokenPanel.add(generateBtn, gbc2);
        generateBtn.addActionListener(this::handleTokenGeneration);

        mainPanel.add(regPanel);
        mainPanel.add(tokenPanel);
        return mainPanel;
    }

    private JPanel createDoctorDirectoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search Doctor:"));
        doctorSearchField = new JTextField(20);
        searchPanel.add(doctorSearchField);
        JButton searchBtn = new JButton("Filter");
        searchPanel.add(searchBtn);

        panel.add(searchPanel, BorderLayout.NORTH);

        String[] columns = {"Doctor ID", "Name", "Role"};
        doctorTableModel = new DefaultTableModel(columns, 0);
        doctorTable = new JTable(doctorTableModel);
        panel.add(new JScrollPane(doctorTable), BorderLayout.CENTER);

        loadDoctorDirectoryData();

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(doctorTableModel);
        doctorTable.setRowSorter(sorter);

        searchBtn.addActionListener(e -> {
            String text = doctorSearchField.getText().trim();
            if (text.length() == 0) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
            }
        });

        return panel;
    }

    private JPanel createLiveQueuePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columns = {"Appointment ID", "Token No", "Patient Name", "Doctor Name", "Session", "Status"};
        queueTableModel = new DefaultTableModel(columns, 0);
        queueTable = new JTable(queueTableModel);
        panel.add(new JScrollPane(queueTable), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        JButton refreshBtn = new JButton("Refresh Live Queue");
        bottomPanel.add(refreshBtn);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        refreshBtn.addActionListener(e -> loadLiveQueueData());
        loadLiveQueueData();

        return panel;
    }

    private void handleRegistration(ActionEvent e) {
        String name = regNameField.getText().trim();
        String ageText = regAgeField.getText().trim();
        String gender = (String) regGenderBox.getSelectedItem();
        String phone = regPhoneField.getText().trim();

        if (name.isEmpty() || ageText.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill out all fields.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int age = Integer.parseInt(ageText);
            try (Connection conn = DB.connect();
                 PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO patients (name, age, gender, phone) VALUES (?, ?, ?, ?)", 
                     Statement.RETURN_GENERATED_KEYS)) {
                
                pstmt.setString(1, name);
                pstmt.setInt(2, age);
                pstmt.setString(3, gender);
                pstmt.setString(4, phone);
                pstmt.executeUpdate();

                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    JOptionPane.showMessageDialog(this, 
                        "Patient Registered Successfully!\nAssigned Patient ID: " + generatedId, 
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                    regNameField.setText("");
                    regAgeField.setText("");
                    regPhoneField.setText("");
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Age must be a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleTokenGeneration(ActionEvent e) {
        String patientIdText = tokenPatientIdField.getText().trim();
        String selectedDoctorItem = (String) tokenDoctorBox.getSelectedItem();
        String session = (String) tokenSessionBox.getSelectedItem();

        if (patientIdText.isEmpty() || selectedDoctorItem == null) {
            JOptionPane.showMessageDialog(this, "Please provide Patient ID and select a doctor.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int patientId = Integer.parseInt(patientIdText);
            String doctorName = selectedDoctorItem.contains(" - ") ? selectedDoctorItem.split(" - ")[1] : selectedDoctorItem;

            try (Connection conn = DB.connect()) {
                PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM patients WHERE id = ?");
                checkStmt.setInt(1, patientId);
                if (!checkStmt.executeQuery().next()) {
                    JOptionPane.showMessageDialog(this, "Patient ID not found! Please register first.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Auto-increment token per session and doctor name
                String tokenQuery = "SELECT COALESCE(MAX(token_no), 0) + 1 AS next_token FROM appointments WHERE session = ? AND doctor_name = ?";
                int nextToken = 1;
                try (PreparedStatement tokenStmt = conn.prepareStatement(tokenQuery)) {
                    tokenStmt.setString(1, session);
                    tokenStmt.setString(2, doctorName);
                    try (ResultSet rs = tokenStmt.executeQuery()) {
                        if (rs.next()) {
                            nextToken = rs.getInt("next_token");
                        }
                    }
                }

                PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO appointments (patient_id, doctor_name, session, token_no, status) VALUES (?, ?, ?, ?, 'Waiting')");
                insertStmt.setInt(1, patientId);
                insertStmt.setString(2, doctorName);
                insertStmt.setString(3, session);
                insertStmt.setInt(4, nextToken);
                insertStmt.executeUpdate();

                JOptionPane.showMessageDialog(this, 
                    "Token Generated Successfully!\nToken No: " + nextToken + "\nSession: " + session, 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                tokenPatientIdField.setText("");
                loadLiveQueueData();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Patient ID must be a numeric value.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Token Generation Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadActiveDoctors(JComboBox<String> comboBox) {
        try (Connection conn = DB.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT patient_id, name FROM users WHERE LOWER(role) = 'doctor'")) {
            
            while (rs.next()) {
                comboBox.addItem(rs.getInt("patient_id") + " - " + rs.getString("name"));
            }
        } catch (Exception e) {
            comboBox.addItem("1 - Dr. Aibel Boby Sam");
        }
    }

    private void loadDoctorDirectoryData() {
        doctorTableModel.setRowCount(0);
        try (Connection conn = DB.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT patient_id, name, role FROM users WHERE LOWER(role) = 'doctor'")) {
            
            while (rs.next()) {
                doctorTableModel.addRow(new Object[]{
                    rs.getInt("patient_id"),
                    rs.getString("name"),
                    rs.getString("role")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load doctor directory: " + e.getMessage());
        }
    }

    private void loadLiveQueueData() {
        queueTableModel.setRowCount(0);
        ArrayList<QueueItem> queueList = new ArrayList<>();

        String query = "SELECT a.appointment_id, a.token_no, p.name AS patient_name, a.doctor_name, a.session, a.status " +
                       "FROM appointments a JOIN patients p ON a.patient_id = p.id " +
                       "WHERE a.status = 'Waiting' ORDER BY a.session, a.token_no ASC";

        try (Connection conn = DB.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                queueList.add(new QueueItem(
                    rs.getInt("appointment_id"),
                    rs.getInt("token_no"),
                    rs.getString("patient_name"),
                    rs.getString("doctor_name"),
                    rs.getString("session"),
                    rs.getString("status")
                ));
            }

            for (QueueItem item : queueList) {
                queueTableModel.addRow(new Object[]{
                    item.appointmentId,
                    item.tokenNo,
                    item.patientName,
                    item.doctorName,
                    item.session,
                    item.status
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load live queue: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class QueueItem {
        int appointmentId;
        int tokenNo;
        String patientName;
        String doctorName;
        String session;
        String status;

        public QueueItem(int appointmentId, int tokenNo, String patientName, String doctorName, String session, String status) {
            this.appointmentId = appointmentId;
            this.tokenNo = tokenNo;
            this.patientName = patientName;
            this.doctorName = doctorName;
            this.session = session;
            this.status = status;
        }
    }
}