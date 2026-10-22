package patient;

import db.DB;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;

public class UserFrame extends JFrame {

    private JTextField regNameField, regAgeField, regPhoneField;
    private JComboBox<String> regGenderBox;

    private JTextField bookPatientIdField;
    private JComboBox<String> bookDoctorBox, bookSessionBox;

    private JTextField lookupIdField;
    private JTextArea prescriptionResultArea;

    private String loggedInUsername; 
    private int loggedInPatientId;   

    public UserFrame() {
        this("guest", 0);
    }

    public UserFrame(String username, int patientId) {
        this.loggedInUsername = username;
        this.loggedInPatientId = patientId;

        setTitle("Hospital Management System - Patient Portal (" + username + ")");
        setSize(650, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Register / Sign-Up", createRegistrationPanel());
        tabbedPane.addTab("Book Appointment", createBookingPanel());
        tabbedPane.addTab("View / Print Prescription", createPrescriptionPanel());

        add(tabbedPane);
    }

    private JPanel createRegistrationPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("New Patient Registration", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0; panel.add(new JLabel("Full Name:"), gbc);
        regNameField = new JTextField(15);
        gbc.gridx = 1; panel.add(regNameField, gbc);

        gbc.gridy = 2; gbc.gridx = 0; panel.add(new JLabel("Age:"), gbc);
        regAgeField = new JTextField(15);
        gbc.gridx = 1; panel.add(regAgeField, gbc);

        gbc.gridy = 3; gbc.gridx = 0; panel.add(new JLabel("Gender:"), gbc);
        regGenderBox = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        gbc.gridx = 1; panel.add(regGenderBox, gbc);

        gbc.gridy = 4; gbc.gridx = 0; panel.add(new JLabel("Phone Number:"), gbc);
        regPhoneField = new JTextField(15);
        gbc.gridx = 1; panel.add(regPhoneField, gbc);

        JButton registerButton = new JButton("Register Patient");
        gbc.gridy = 5; gbc.gridx = 0; gbc.gridwidth = 2;
        panel.add(registerButton, gbc);

        registerButton.addActionListener(this::handleRegistration);

        return panel;
    }

    private JPanel createBookingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Book OPD Appointment", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0; panel.add(new JLabel("Patient ID:"), gbc);
        bookPatientIdField = new JTextField(15);
        if (loggedInPatientId > 0) {
            bookPatientIdField.setText(String.valueOf(loggedInPatientId));
        }
        gbc.gridx = 1; panel.add(bookPatientIdField, gbc);

        gbc.gridy = 2; gbc.gridx = 0; panel.add(new JLabel("Select Doctor:"), gbc);
        bookDoctorBox = new JComboBox<>();
        loadActiveDoctors(bookDoctorBox);
        gbc.gridx = 1; panel.add(bookDoctorBox, gbc);

        gbc.gridy = 3; gbc.gridx = 0; panel.add(new JLabel("Session:"), gbc);
        bookSessionBox = new JComboBox<>(new String[]{"Morning", "Evening"});
        gbc.gridx = 1; panel.add(bookSessionBox, gbc);

        JButton bookButton = new JButton("Confirm Booking & Get Token");
        gbc.gridy = 4; gbc.gridx = 0; gbc.gridwidth = 2;
        panel.add(bookButton, gbc);

        bookButton.addActionListener(this::handleBooking);

        return panel;
    }

    private JPanel createPrescriptionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Enter Patient ID:"));
        lookupIdField = new JTextField(10);
        if (loggedInPatientId > 0) {
            lookupIdField.setText(String.valueOf(loggedInPatientId));
        }
        topPanel.add(lookupIdField);

        JButton lookupButton = new JButton("Fetch Prescription");
        topPanel.add(lookupButton);

        panel.add(topPanel, BorderLayout.NORTH);

        prescriptionResultArea = new JTextArea();
        prescriptionResultArea.setEditable(false);
        prescriptionResultArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        panel.add(new JScrollPane(prescriptionResultArea), BorderLayout.CENTER);

        JButton downloadButton = new JButton("Download Prescription (.txt)");
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(downloadButton);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        lookupButton.addActionListener(this::handlePrescriptionLookup);
        
        downloadButton.addActionListener(e -> {
            String content = prescriptionResultArea.getText().trim();
            if (content.isEmpty() || content.startsWith("No medical history")) {
                JOptionPane.showMessageDialog(this, "No valid prescription content to download.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String currentId = lookupIdField.getText().trim();
            String fileName = (loggedInUsername != null && !loggedInUsername.equals("guest") ? loggedInUsername : "patient_" + currentId) + ".txt";

            try (FileWriter writer = new FileWriter(fileName)) {
                writer.write(content);
                JOptionPane.showMessageDialog(this, "Prescription successfully saved to file: " + fileName, "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

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
                        "Registration Successful!\nYour unique Patient ID is: " + generatedId, 
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                    regNameField.setText("");
                    regAgeField.setText("");
                    regPhoneField.setText("");
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Age must be a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

    private void handleBooking(ActionEvent e) {
        String patientIdText = bookPatientIdField.getText().trim();
        String selectedDoctorItem = (String) bookDoctorBox.getSelectedItem();
        String session = (String) bookSessionBox.getSelectedItem();

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
                    "Appointment Booked Successfully!\n\nAssigned Token No: " + nextToken + "\nSession: " + session, 
                    "Token Assigned", JOptionPane.INFORMATION_MESSAGE);
                bookPatientIdField.setText("");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Patient ID must be a numeric value.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Booking Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handlePrescriptionLookup(ActionEvent e) {
        String patientIdText = lookupIdField.getText().trim();
        if (patientIdText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a Patient ID.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int patientId = Integer.parseInt(patientIdText);

            try (Connection conn = DB.connect();
                 PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT p.name AS patient_name, pr.diagnosis, pr.medicines, a.session " +
                     "FROM prescriptions pr " +
                     "JOIN appointments a ON pr.appointment_id = a.appointment_id " +
                     "JOIN patients p ON pr.patient_id = p.id " +
                     "WHERE pr.patient_id = ?")) {
                
                pstmt.setInt(1, patientId);
                ResultSet rs = pstmt.executeQuery();

                StringBuilder sb = new StringBuilder();
                sb.append("=========================================\n");
                sb.append("          HOSPITAL PRESCRIPTION RECORD     \n");
                sb.append("=========================================\n\n");

                boolean found = false;
                while (rs.next()) {
                    found = true;
                    sb.append("Patient Name : ").append(rs.getString("patient_name")).append("\n");
                    sb.append("Session      : ").append(rs.getString("session")).append("\n");
                    sb.append("-----------------------------------------\n");
                    sb.append("Diagnosis    :\n").append(rs.getString("diagnosis")).append("\n\n");
                    sb.append("Prescription :\n").append(rs.getString("medicines")).append("\n");
                    sb.append("=========================================\n\n");
                }

                if (!found) {
                    prescriptionResultArea.setText("No medical history or completed consultations found for Patient ID: " + patientId);
                } else {
                    prescriptionResultArea.setText(sb.toString());
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Patient ID must be a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            prescriptionResultArea.setText("Error retrieving data: " + ex.getMessage());
        }
    }
}