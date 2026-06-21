package com.tcp.tgk.client.ui;

import com.tcp.tgk.client.service.ConnectionStateChangeEvent;
import com.tcp.tgk.client.service.ConnectionStateMachine;
import com.tcp.tgk.client.service.ConnectionStateMachine.ConnectionState;
import com.tcp.tgk.client.service.TcpClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

@Component
public class SwingUiFrame {

    @Autowired
    private TcpClientService clientService;

    @Autowired
    private ConnectionStateMachine stateMachine;

    @Value("${tcp.client.mode:both}")
    private String mode;

    private JFrame frame;
    private BooleanIndicator statusIndicator;
    private JLabel statusLabel;
    private JTextField addressField;
    private JButton reconnectBtn;

    private JComboBox<String> cmdDropdown;
    private JPanel mathPanel;
    private JTextField val1Field;
    private JComboBox<String> opDropdown;
    private JTextField val2Field;
    private JPanel genericPanel;
    private JTextField payloadField;
    private JButton sendBtn;

    private JTextArea resultTextArea;
    private JTextArea errorTextArea;

    private final List<String> resultHistory = new ArrayList<>();
    private final List<String> errorHistory = new ArrayList<>();

    public void showUi() {
        if ("terminal".equalsIgnoreCase(mode)) {
            System.out.println("GUI Swing UI disabled (mode is terminal).");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            initComponents();
            setupLogAppender();
            
            // Set initial state
            updateConnectionStatus(stateMachine.getCurrentState(), null);
            
            frame.setVisible(true);
        });
    }

    private void initComponents() {
        frame = new JFrame("TCP Client Swing Panel");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(850, 600);
        frame.setLocationRelativeTo(null); // Center window
        
        frame.setLayout(new BorderLayout(10, 10));
        frame.getRootPane().setBorder(new EmptyBorder(10, 10, 10, 10));

        // 1. Top Panel: Connection Config
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(new TitledBorder("Server Connection Settings"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Status Indicator
        statusIndicator = new BooleanIndicator(false);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        topPanel.add(statusIndicator, gbc);

        // Status Label
        statusLabel = new JLabel("INIT_IDLE");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        statusLabel.setForeground(Color.GRAY); // Gray for idle state
        gbc.gridx = 1;
        gbc.gridy = 0;
        topPanel.add(statusLabel, gbc);

        // Spacer line / padding
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(5, 20));
        gbc.gridx = 2;
        gbc.gridy = 0;
        topPanel.add(sep, gbc);

        // Address Label
        JLabel addressLabel = new JLabel("Server Address:");
        gbc.gridx = 3;
        gbc.gridy = 0;
        topPanel.add(addressLabel, gbc);

        // Address Field
        String initialHost = clientService.getHost() != null ? clientService.getHost() : "localhost";
        int initialPort = clientService.getPort();
        addressField = new JTextField(initialHost + ":" + initialPort, 20);
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        topPanel.add(addressField, gbc);

        // Reconnect/Refresh Button
        reconnectBtn = new JButton("↻ Reconnect");
        gbc.gridx = 5;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        topPanel.add(reconnectBtn, gbc);

        frame.add(topPanel, BorderLayout.NORTH);

        // 2. Center Panel: Controls & Inputs
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        
        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBorder(new TitledBorder("Command Dispatcher"));
        
        GridBagConstraints cGbc = new GridBagConstraints();
        cGbc.fill = GridBagConstraints.HORIZONTAL;
        cGbc.insets = new Insets(5, 5, 5, 5);

        // Command selection dropdown
        cmdDropdown = new JComboBox<>(new String[]{"fast", "deep", "info", "time", "help", "math"});
        cGbc.gridx = 0;
        cGbc.gridy = 0;
        cGbc.weightx = 0.0;
        controlPanel.add(cmdDropdown, cGbc);

        // Card Panel containing either Maths inputs or generic payload
        JPanel dynamicContainer = new JPanel(new CardLayout());
        
        // Maths inputs
        mathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        val1Field = new JTextField(5);
        opDropdown = new JComboBox<>(new String[]{"+", "-", "*", "/", "%", "^"});
        val2Field = new JTextField(5);
        mathPanel.add(new JLabel("Value 1:"));
        mathPanel.add(val1Field);
        mathPanel.add(new JLabel("Op:"));
        mathPanel.add(opDropdown);
        mathPanel.add(new JLabel("Value 2:"));
        mathPanel.add(val2Field);

        // Generic payload
        genericPanel = new JPanel(new BorderLayout(5, 0));
        payloadField = new JTextField();
        genericPanel.add(new JLabel("Payload (Optional):"), BorderLayout.WEST);
        genericPanel.add(payloadField, BorderLayout.CENTER);

        dynamicContainer.add(genericPanel, "generic");
        dynamicContainer.add(mathPanel, "math");
        
        cGbc.gridx = 1;
        cGbc.gridy = 0;
        cGbc.weightx = 1.0;
        controlPanel.add(dynamicContainer, cGbc);

        // Send Button
        sendBtn = new JButton(">");
        sendBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        cGbc.gridx = 2;
        cGbc.gridy = 0;
        cGbc.weightx = 0.0;
        controlPanel.add(sendBtn, cGbc);

        centerPanel.add(controlPanel, BorderLayout.NORTH);

        // 3. Output Panels
        final JPanel outputGrid = new JPanel(new GridBagLayout());
        GridBagConstraints ogbc = new GridBagConstraints();
        ogbc.fill = GridBagConstraints.BOTH;
        ogbc.weighty = 1.0;
        
        // Result and History area
        resultTextArea = new JTextArea();
        resultTextArea.setEditable(false);
        resultTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        resultTextArea.setForeground(new Color(56, 189, 248)); // Sleek Blue
        resultTextArea.setBackground(new Color(15, 23, 42)); // Slate Dark
        JScrollPane resultScroll = new JScrollPane(resultTextArea);
        resultScroll.setBorder(new TitledBorder(null, "Results & History (Last 20)", TitledBorder.LEADING, TitledBorder.TOP, null, Color.GRAY));
        resultScroll.setPreferredSize(new Dimension(0, 0));
        
        ogbc.gridx = 0;
        ogbc.weightx = 1.0;
        ogbc.insets = new Insets(0, 0, 0, 5);
        outputGrid.add(resultScroll, ogbc);

        // Error Console area
        errorTextArea = new JTextArea();
        errorTextArea.setEditable(false);
        errorTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        errorTextArea.setForeground(new Color(253, 164, 175)); // Soft Red
        errorTextArea.setBackground(new Color(15, 23, 42)); // Slate Dark
        final JScrollPane errorScroll = new JScrollPane(errorTextArea);
        errorScroll.setBorder(new TitledBorder(null, "Error & System Logs (Last 50 lines)", TitledBorder.LEADING, TitledBorder.TOP, null, Color.GRAY));
        errorScroll.setPreferredSize(new Dimension(0, 0));
        
        ogbc.gridx = 1;
        ogbc.weightx = 1.0;
        ogbc.insets = new Insets(0, 5, 0, 0);
        outputGrid.add(errorScroll, ogbc);

        // Wrapper for outputGrid with Hide/Show Toggle Header
        JPanel logsWrapper = new JPanel(new BorderLayout(5, 5));
        
        JPanel logsHeader = new JPanel(new BorderLayout());
        JLabel logsTitleLabel = new JLabel("System Logs & Console History");
        logsTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        
        final JButton toggleLogsBtn = new JButton("▼ Hide Logs");
        toggleLogsBtn.setFocusPainted(false);
        toggleLogsBtn.setMargin(new Insets(2, 6, 2, 6));
        
        toggleLogsBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean nextVisible = !errorScroll.isVisible();
                errorScroll.setVisible(nextVisible);
                toggleLogsBtn.setText(nextVisible ? "▼ Hide Logs" : "▲ Show Logs");
                frame.revalidate();
                frame.repaint();
            }
        });
        
        logsHeader.add(logsTitleLabel, BorderLayout.WEST);
        logsHeader.add(toggleLogsBtn, BorderLayout.EAST);
        
        logsWrapper.add(logsHeader, BorderLayout.NORTH);
        logsWrapper.add(outputGrid, BorderLayout.CENTER);

        centerPanel.add(logsWrapper, BorderLayout.CENTER);
        frame.add(centerPanel, BorderLayout.CENTER);

        // Action Listeners
        cmdDropdown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CardLayout cl = (CardLayout) dynamicContainer.getLayout();
                if ("math".equals(cmdDropdown.getSelectedItem())) {
                    cl.show(dynamicContainer, "math");
                } else {
                    cl.show(dynamicContainer, "generic");
                }
            }
        });

        // Trigger connection action
        ActionListener reconnectAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = addressField.getText().trim();
                String[] parts = text.split(":");
                if (parts.length != 2) {
                    appendErrorLog("Invalid address format. Expected host:port");
                    return;
                }
                try {
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);
                    clientService.reconfigureTarget(host, port);
                } catch (NumberFormatException ex) {
                    appendErrorLog("Invalid port number");
                }
            }
        };

        reconnectBtn.addActionListener(reconnectAction);
        addressField.addActionListener(reconnectAction);

        // Send Command action
        ActionListener sendAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!stateMachine.isConnected()) {
                    appendResultLog("Client", "Connecting");
                    return;
                }
                String selectedCmd = (String) cmdDropdown.getSelectedItem();
                String fullCommand;
                if ("math".equals(selectedCmd)) {
                    String v1 = val1Field.getText().trim();
                    String op = (String) opDropdown.getSelectedItem();
                    String v2 = val2Field.getText().trim();
                    if (v1.isEmpty() || v2.isEmpty()) {
                        appendErrorLog("Math values cannot be empty");
                        return;
                    }
                    fullCommand = "m " + v1 + " " + op + " " + v2;
                } else {
                    fullCommand = selectedCmd;
                    String payload = payloadField.getText().trim();
                    if (!payload.isEmpty()) {
                        fullCommand += ":" + payload;
                    }
                }

                final String cmdToSend = fullCommand;
                new Thread(() -> {
                    appendResultLog("User", cmdToSend);
                    try {
                        String response = clientService.sendCommand(cmdToSend);
                        SwingUtilities.invokeLater(() -> appendResultLog("Server", response));
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> appendErrorLog("Command send failed: " + ex.getMessage()));
                    }
                }, "swing-send-thread").start();
            }
        };

        sendBtn.addActionListener(sendAction);
    }

    private void setupLogAppender() {
        LogEventAppender.setLogConsumer(logMessage -> {
            SwingUtilities.invokeLater(() -> appendErrorLog(logMessage));
        });

        try {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            LogEventAppender appender = new LogEventAppender();
            appender.setContext(root.getLoggerContext());
            appender.start();
            root.addAppender(appender);
        } catch (Exception e) {
            System.err.println("Failed to register Logback appender programmatically: " + e.getMessage());
        }
    }

    private void updateConnectionStatus(ConnectionState state, String error) {
        if (statusLabel == null || statusIndicator == null) {
            return;
        }
        statusLabel.setText(state.name());
        if (state == ConnectionState.CONNECTED) {
            statusIndicator.setStatus(true);
            statusLabel.setForeground(new Color(16, 185, 129));
        } else {
            statusIndicator.setStatus(false);
            if (state == ConnectionState.ERROR) {
                statusLabel.setForeground(Color.RED);
            } else if (state == ConnectionState.CONNECTING || state == ConnectionState.RECONNECTING) {
                statusLabel.setForeground(new Color(245, 158, 11));
            } else {
                statusLabel.setForeground(Color.GRAY);
            }
        }
    }

    @EventListener
    public void handleStateChange(ConnectionStateChangeEvent event) {
        SwingUtilities.invokeLater(() -> {
            updateConnectionStatus(event.getState(), event.getError());
        });
    }

    private void appendResultLog(String source, String message) {
        if (resultTextArea == null) {
            return;
        }
        String time = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        resultHistory.add("[" + time + "] " + source + " > " + message);
        if (resultHistory.size() > 20) {
            resultHistory.remove(0);
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = resultHistory.size() - 1; i >= 0; i--) {
            sb.append(resultHistory.get(i)).append("\n");
        }
        resultTextArea.setText(sb.toString());
        resultTextArea.setCaretPosition(0);
    }

    private void appendErrorLog(String message) {
        if (errorTextArea == null) {
            return;
        }
        errorHistory.add(message);
        if (errorHistory.size() > 50) {
            errorHistory.remove(0);
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = errorHistory.size() - 1; i >= 0; i--) {
            sb.append(errorHistory.get(i)).append("\n");
        }
        errorTextArea.setText(sb.toString());
        errorTextArea.setCaretPosition(0);
    }
}
