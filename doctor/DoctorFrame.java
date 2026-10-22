package doctor;

import db.DB;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.LinkedList;
import java.util.Queue;

public class DoctorFrame extends JFrame {
    private JTextArea patientDetailsArea;
    private JTextArea diagnosisArea;
    private JTextArea prescriptionArea;
    private JLabel queueCountLabel;

    private Queue<AppointmentNode> waitingQueue = new LinkedList<>();
    private AppointmentNode currentPatient = null;
    private String loggedInDoctorName;
    private int displayTokenCounter = 1;

    private static class AppointmentNode {
        int appointmentId;
        int patientId;
        String patientName;
        String session;
        int tokenNo;

        public AppointmentNode(int appointmentId, int patientId, String patientName, String session, int tokenNo) {
            this.appointmentId = appointmentId;
            this.patientId = patientId;
            this.patientName = patientName;
            this.session = session;
            this.tokenNo = tokenNo;
        }
    }

    // Constructor now requires the logged-in doctor's name
    public DoctorFrame(String doctorName) {
        this.loggedInDoctorName = (doctorName != null && !doctorName.isEmpty()) ? doctorName : "Doctor";
        
        setTitle("Doctor Consultation Module - Dr. " + loggedInDoctorName);
        setSize(700, 550);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        queueCountLabel = new JLabel("Waiting Patients in Queue: 0");
        queueCountLabel.setFont(new Font("Arial", Font.BOLD, 14));
        JButton refreshButton = new JButton("Load / Refresh Queue");
        JButton nextPatientButton = new JButton("Call Next Patient");
        
        topPanel.add(queueCountLabel);
        topPanel.add(refreshButton);
        topPanel.add(nextPatientButton);
        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        patientDetailsArea = new JTextArea();
        patientDetailsArea.setEditable(false);
        patientDetailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane detailsScroll = new JScrollPane(patientDetailsArea);
        detailsScroll.setBorder(BorderFactory.createTitledBorder("Current Patient Details"));
        centerPanel.add(detailsScroll);

        JPanel inputFormPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        
        diagnosisArea = new JTextArea();
        JScrollPane diagScroll = new JScrollPane(diagnosisArea);
        diagScroll.setBorder(BorderFactory.createTitledBorder("Diagnosis Notes"));
        
        prescriptionArea = new JTextArea();
        JScrollPane prescScroll = new JScrollPane(prescriptionArea);
        prescScroll.setBorder(BorderFactory.createTitledBorder("Prescription / Medications"));

        inputFormPanel.add(diagScroll);
        inputFormPanel.add(prescScroll);
        centerPanel.add(inputFormPanel);

        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        JButton treatButton = new JButton("Save & Treat Next Patient");
        treatButton.setFont(new Font("Arial", Font.BOLD, 13));
        bottomPanel.add(treatButton);
        add(bottomPanel, BorderLayout.SOUTH);

        refreshButton.addActionListener(e -> loadQueueFromDatabase());
        nextPatientButton.addActionListener(e -> callNextPatient());
        treatButton.addActionListener(this::handleTreatPatient);

        loadQueueFromDatabase();
    }

    private void loadQueueFromDatabase() {
        waitingQueue.clear();
        displayTokenCounter = 1;
        
        // Strictly filters queue by the logged-in doctor's name
        String sql = "SELECT a.appointment_id, a.patient_id, p.name AS patient_name, a.session, a.token_no " +
                     "FROM appointments a JOIN patients p ON a.patient_id = p.id " +
                     "WHERE a.status = 'Waiting' AND a.doctor_name = ? " +
                     "ORDER BY a.session, a.token_no ASC";

        try (Connection conn = DB.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, loggedInDoctorName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int dbToken = rs.getInt("token_no");
                    int assignedToken = (dbToken > 0) ? dbToken : displayTokenCounter++;

                    waitingQueue.add(new AppointmentNode(
                        rs.getInt("appointment_id"),
                        rs.getInt("patient_id"),
                        rs.getString("patient_name"),
                        rs.getString("session"),
                        assignedToken
                    ));
                }
            }
            queueCountLabel.setText("Waiting Patients in Queue: " + waitingQueue.size());
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading queue: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void callNextPatient() {
        if (waitingQueue.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No patients currently in your waiting queue.", "Queue Empty", JOptionPane.INFORMATION_MESSAGE);
            currentPatient = null;
            patientDetailsArea.setText("");
            return;
        }

        currentPatient = waitingQueue.poll();
        queueCountLabel.setText("Waiting Patients in Queue: " + waitingQueue.size());

        StringBuilder sb = new StringBuilder();
        sb.append("===============================\n");
        sb.append("      ACTIVE CONSULTATION      \n");
        sb.append("===============================\n");
        sb.append("Token No    : ").append(currentPatient.tokenNo).append("\n");
        sb.append("Session     : ").append(currentPatient.session).append("\n");
        sb.append("Patient ID  : ").append(currentPatient.patientId).append("\n");
        sb.append("Name        : ").append(currentPatient.patientName).append("\n");
        sb.append("===============================\n");

        patientDetailsArea.setText(sb.toString());
        diagnosisArea.setText("");
        prescriptionArea.setText("");
    }

    private void handleTreatPatient(ActionEvent e) {
        if (currentPatient == null) {
            JOptionPane.showMessageDialog(this, "No active patient under consultation. Click 'Call Next Patient' first.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String diagnosis = diagnosisArea.getText().trim();
        String prescription = prescriptionArea.getText().trim();

        if (diagnosis.isEmpty() || prescription.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in both Diagnosis and Prescription notes before completing.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String updateAppointmentSQL = "UPDATE appointments SET status = 'Treated' WHERE appointment_id = ?";
        String insertPrescriptionSQL = "INSERT INTO prescriptions (appointment_id, patient_id, doctor_name, diagnosis, medicines) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DB.connect()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmtAppt = conn.prepareStatement(updateAppointmentSQL);
                 PreparedStatement pstmtPres = conn.prepareStatement(insertPrescriptionSQL)) {

                pstmtAppt.setInt(1, currentPatient.appointmentId);
                pstmtAppt.executeUpdate();

                pstmtPres.setInt(1, currentPatient.appointmentId);
                pstmtPres.setInt(2, currentPatient.patientId);
                pstmtPres.setString(3, loggedInDoctorName);
                pstmtPres.setString(4, diagnosis);
                pstmtPres.setString(5, prescription);
                pstmtPres.executeUpdate();

                conn.commit();
                JOptionPane.showMessageDialog(this, "Patient consultation completed and records saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
                
                currentPatient = null;
                patientDetailsArea.setText("");
                diagnosisArea.setText("");
                prescriptionArea.setText("");
                loadQueueFromDatabase();

            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving treatment data: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}