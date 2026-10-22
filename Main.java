import auth.LoginFrame;

import javax.swing.*;
import java.awt.*;

public class Main extends JFrame {

    public Main() {
        setTitle("Hospital Management System - Portal Hub");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Hospital Management System", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridx = 0; gbc.gridy = 0;
        add(titleLabel, gbc);

        // Role-specific login buttons
        JButton patientLoginBtn = new JButton("PATIENT LOGIN");
        JButton doctorLoginBtn = new JButton("DOCTOR LOGIN");
        JButton staffLoginBtn = new JButton("STAFF LOGIN");
        JButton adminLoginBtn = new JButton("ADMIN LOGIN");

        gbc.gridy = 1; add(patientLoginBtn, gbc);
        gbc.gridy = 2; add(doctorLoginBtn, gbc);
        gbc.gridy = 3; add(staffLoginBtn, gbc);
        gbc.gridy = 4; add(adminLoginBtn, gbc);

        // Action Listeners opening the targeted role login page
        patientLoginBtn.addActionListener(e -> new LoginFrame("Patient").setVisible(true));
        doctorLoginBtn.addActionListener(e -> new LoginFrame("Doctor").setVisible(true));
        staffLoginBtn.addActionListener(e -> new LoginFrame("Desk Staff").setVisible(true));
        adminLoginBtn.addActionListener(e -> new LoginFrame("Admin").setVisible(true));
    }

    public static void main(String[] args) {
        db.DB.ensureSchema(); // Ensure database schema and bootstrap admin exist on startup
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}