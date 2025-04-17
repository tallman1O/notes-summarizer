package com.meetingsummarizer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.json.JSONObject;

public class MeetingSummarizerApp extends JFrame {
    private final JTextArea inputArea;
    private final JTextArea outputArea;
    private final String API_URL = "http://localhost:5000/summarize";

    public MeetingSummarizerApp() {
        super("AI Meeting Summarizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);

        // Create components
        inputArea = new JTextArea(15, 40);
        outputArea = new JTextArea(15, 40);
        outputArea.setEditable(false);

        JButton summarizeButton = new JButton("Summarize Notes");
        JButton uploadButton = new JButton("Upload Notes File");
        JButton clearButton = new JButton("Clear");

        // Set up scrollable text areas
        JScrollPane inputScrollPane = new JScrollPane(inputArea);
        JScrollPane outputScrollPane = new JScrollPane(outputArea);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(uploadButton);
        buttonPanel.add(summarizeButton);
        buttonPanel.add(clearButton);

        // Labels
        JLabel inputLabel = new JLabel("Raw Meeting Notes:");
        JLabel outputLabel = new JLabel("Summarized Notes:");

        // Add components to frame
        setLayout(new BorderLayout());

        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        JPanel inputPanel = new JPanel(new BorderLayout());
        JPanel outputPanel = new JPanel(new BorderLayout());

        inputPanel.add(inputLabel, BorderLayout.NORTH);
        inputPanel.add(inputScrollPane, BorderLayout.CENTER);

        outputPanel.add(outputLabel, BorderLayout.NORTH);
        outputPanel.add(outputScrollPane, BorderLayout.CENTER);

        centerPanel.add(inputPanel);
        centerPanel.add(outputPanel);

        add(centerPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Add padding
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add actions
        summarizeButton.addActionListener(this::onSummarizeClicked);
        uploadButton.addActionListener(this::onUploadClicked);
        clearButton.addActionListener(e -> {
            inputArea.setText("");
            outputArea.setText("");
        });
    }

    private void onSummarizeClicked(ActionEvent e) {
        String rawNotes = inputArea.getText().trim();
        if (rawNotes.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter or upload meeting notes first.",
                    "Empty Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Show loading indicator
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        outputArea.setText("Generating summary...");

        // Use a SwingWorker to prevent UI freezing
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                try {
                    return callSummarizeAPI(rawNotes);
                } catch (Exception ex) {
                    return "Error: " + ex.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String summary = get();
                    outputArea.setText(summary);
                } catch (Exception ex) {
                    outputArea.setText("Error: " + ex.getMessage());
                }
                setCursor(Cursor.getDefaultCursor());
            }
        };

        worker.execute();
    }

    private String callSummarizeAPI(String rawNotes) throws IOException {
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Create JSON payload
        JSONObject payload = new JSONObject();
        payload.put("meetingNotes", rawNotes);

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
            // Handle error response
            try (java.util.Scanner scanner = new java.util.Scanner(connection.getErrorStream(), StandardCharsets.UTF_8.name())) {
                scanner.useDelimiter("\\A");
                if (scanner.hasNext()) {
                    JSONObject errorJson = new JSONObject(scanner.next());
                    return "API Error: " + errorJson.optString("error", "Unknown error");
                }
            }
            throw e;
        }

        // Parse response
        JSONObject jsonResponse = new JSONObject(response.toString());
        if (jsonResponse.optBoolean("success", false)) {
            return jsonResponse.getString("summary");
        } else {
            return "API Error: " + jsonResponse.optString("error", "Unknown error");
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

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            MeetingSummarizerApp app = new MeetingSummarizerApp();
            app.setVisible(true);
        });
    }
}