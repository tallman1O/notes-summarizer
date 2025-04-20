package com.studynotes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.json.JSONObject;

public class StudyNotesApp extends JFrame {
    private final JTextArea inputArea;
    private final JTextArea outputArea;
    private final JTextArea quizArea;
    private final String API_URL = "http://localhost:5000/generate_notes";
    private JComboBox<String> subjectDropdown;

    public StudyNotesApp() {
        super("AI Study Notes Generator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);

        // Create components
        inputArea = new JTextArea(15, 40);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);

        outputArea = new JTextArea(10, 40);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setEditable(false);

        quizArea = new JTextArea(5, 40);
        quizArea.setLineWrap(true);
        quizArea.setWrapStyleWord(true);
        quizArea.setEditable(false);

        JButton generateButton = new JButton("Generate Study Notes");
        JButton uploadButton = new JButton("Upload Lecture File");
        JButton exportButton = new JButton("Export Notes");
        JButton clearButton = new JButton("Clear");

        // Subject dropdown
        String[] subjects = {"General", "Math", "Science", "History", "Biology", "Physics", "Chemistry"};
        subjectDropdown = new JComboBox<>(subjects);

        // Set up scrollable text areas
        JScrollPane inputScrollPane = new JScrollPane(inputArea);
        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        JScrollPane quizScrollPane = new JScrollPane(quizArea);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(uploadButton);
        buttonPanel.add(generateButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(clearButton);

        // Labels
        JLabel inputLabel = new JLabel("Lecture Text or Notes:");
        JLabel outputLabel = new JLabel("Study Notes:");
        JLabel quizLabel = new JLabel("Quiz Questions:");

        // Add components to frame
        setLayout(new BorderLayout());

        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        JPanel inputPanel = new JPanel(new BorderLayout());
        JPanel outputPanel = new JPanel(new BorderLayout());
        JPanel quizPanel = new JPanel(new BorderLayout());

        inputPanel.add(inputLabel, BorderLayout.NORTH);
        inputPanel.add(inputScrollPane, BorderLayout.CENTER);

        outputPanel.add(outputLabel, BorderLayout.NORTH);
        outputPanel.add(outputScrollPane, BorderLayout.CENTER);

        quizPanel.add(quizLabel, BorderLayout.NORTH);
        quizPanel.add(quizScrollPane, BorderLayout.CENTER);

        centerPanel.add(inputPanel);
        centerPanel.add(outputPanel);
        centerPanel.add(quizPanel);

        add(centerPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Add padding
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add actions
        generateButton.addActionListener(this::onGenerateClicked);
        uploadButton.addActionListener(this::onUploadClicked);
        exportButton.addActionListener(this::onExportClicked);
        clearButton.addActionListener(e -> {
            inputArea.setText("");
            outputArea.setText("");
            quizArea.setText("");
        });
    }

    private void onGenerateClicked(ActionEvent e) {
        String lectureText = inputArea.getText().trim();
        if (lectureText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter or upload lecture text first.",
                    "Empty Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String subject = subjectDropdown.getSelectedItem().toString();

        // Show loading indicator
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        outputArea.setText("Generating study notes...");
        quizArea.setText("");

        // Use a SwingWorker to prevent UI freezing
        SwingWorker<String[], Void> worker = new SwingWorker<>() {
            @Override
            protected String[] doInBackground() {
                try {
                    return callGenerateNotesAPI(lectureText, subject);
                } catch (Exception ex) {
                    return new String[] {"Error: " + ex.getMessage(), ""};
                }
            }

            @Override
            protected void done() {
                try {
                    String[] result = get();
                    outputArea.setText(result[0]);
                    quizArea.setText(result[1]);
                } catch (Exception ex) {
                    outputArea.setText("Error: " + ex.getMessage());
                }
                setCursor(Cursor.getDefaultCursor());
            }
        };

        worker.execute();
    }

    private String[] callGenerateNotesAPI(String lectureText, String subject) throws IOException {
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Create JSON payload
        JSONObject payload = new JSONObject();
        payload.put("lectureNotes", lectureText);
        payload.put("subject", subject);

        // Send request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Read response
        StringBuilder response = new StringBuilder();
        try (java.util.Scanner scanner = new java.util.Scanner(connection.getInputStream(), StandardCharsets.UTF_8.name())) {
            scanner.useDelimiter("\\A");
            if (scanner.hasNext()) {
                response.append(scanner.next());
            }
        } catch (IOException e) {
            try (java.util.Scanner scanner = new java.util.Scanner(connection.getErrorStream(), StandardCharsets.UTF_8.name())) {
                scanner.useDelimiter("\\A");
                if (scanner.hasNext()) {
                    JSONObject errorJson = new JSONObject(scanner.next());
                    return new String[] {"API Error: " + errorJson.optString("error", "Unknown error"), ""};
                }
            }
            throw e;
        }

        // Parse response
        JSONObject jsonResponse = new JSONObject(response.toString());
        if (jsonResponse.optBoolean("success", false)) {
            String notes = jsonResponse.getString("notes");
            String quiz = jsonResponse.optString("quiz", "No quiz questions available.");
            return new String[] {notes, quiz};
        } else {
            return new String[] {"Error: " + jsonResponse.optString("error", "Unknown error"), ""};
        }
    }

    private void onUploadClicked(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                String content = new String(Files.readAllBytes(selectedFile.toPath()));
                inputArea.setText(content);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Error reading file: " + ex.getMessage(),
                        "File Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onExportClicked(ActionEvent e) {
        String studyNotes = outputArea.getText();
        if (studyNotes.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No study notes to export.",
                    "Export Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Save to file
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Study Notes");
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
                writer.write(studyNotes);
                JOptionPane.showMessageDialog(this, "Study notes exported successfully!");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Error exporting notes: " + ex.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            StudyNotesApp app = new StudyNotesApp();
            app.setVisible(true);
        });
    }
}
