
 

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

class AssemblerApp extends JFrame {

    private JTabbedPane tabbedPane;
    private JTextArea inputText;
    private JTextArea pass1Output;
    private JTextArea pass2Output;
    private Map<String, String> opcodeTable;
    private Map<String, String> symbolTable;
    private int startingAddress;
    private int programLength;

    // Constructor to set up the UI and event handlers
    public AssemblerApp() {
        setTitle("Simple Assembler");

        // Create a tabbed pane
        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);

        // Input Area
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputText = new JTextArea(20, 60);
        inputText.setBorder(new EmptyBorder(10, 10, 10, 10));
        inputPanel.add(new JScrollPane(inputText), BorderLayout.CENTER);
        tabbedPane.addTab("Input", inputPanel);

        // Pass 1 Output
        JPanel pass1Panel = new JPanel(new BorderLayout());
        pass1Output = new JTextArea(20, 60);
        pass1Output.setBorder(new EmptyBorder(10, 10, 10, 10));
        pass1Panel.add(new JScrollPane(pass1Output), BorderLayout.CENTER);
        tabbedPane.addTab("Pass 1 Output", pass1Panel);

        // Pass 2 Output
        JPanel pass2Panel = new JPanel(new BorderLayout());
        pass2Output = new JTextArea(20, 60);
        pass2Output.setBorder(new EmptyBorder(10, 10, 10, 10));
        pass2Panel.add(new JScrollPane(pass2Output), BorderLayout.CENTER);
        tabbedPane.addTab("Pass 2 Output", pass2Panel);

        // Button panel
        JPanel buttonPanel = new JPanel();
        JButton loadButton = new JButton("Load Input File");
        JButton runButton = new JButton("Run Assembler");

        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadInputFile();
            }
        });

        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runAssembler();
            }
        });

        buttonPanel.add(loadButton);
        buttonPanel.add(runButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Load opcode table once
        opcodeTable = loadOpcodeTable();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null); // Center the window
        setVisible(true);
    }

    // Load the opcode table from the "optab.txt" file
    private Map<String, String> loadOpcodeTable() {
        Map<String, String> opcodes = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("optab.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) {
                    opcodes.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Opcode table file not found. Please ensure 'optab.txt' exists.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        return opcodes;
    }

    // Load the assembly input file into the input text area
    private void loadInputFile() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(selectedFile))) {
                StringBuilder fileContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    fileContent.append(line).append("\n");
                }
                inputText.setText(fileContent.toString());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error reading file: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Run the assembler by executing Pass 1 and Pass 2
    private void runAssembler() {
        if (opcodeTable.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cannot run assembler. Opcode table is not loaded.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[] assemblyLines = inputText.getText().split("\\n");
        pass1Output.setText("");
        pass2Output.setText("");

        try {
            String intermediateOutput = passOne(assemblyLines);
            pass1Output.setText("Pass 1 Output:\n" + intermediateOutput + "\n");

            String passTwoOutputText = passTwo();
            pass2Output.setText("Pass 2 Output:\n" + passTwoOutputText + "\n");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Pass 1: Generate intermediate file and symbol table
    private String passOne(String[] assemblyLines) throws Exception {
        int locationCounter = 0;
        startingAddress = 0;
        symbolTable = new HashMap<>();
        StringBuilder outputLines = new StringBuilder();

        for (String line : assemblyLines) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length == 0) continue;

            // Handle START directive
            if (parts[0].equals("COPY") && parts.length > 2 && parts[1].equals("START")) {
                startingAddress = Integer.parseInt(parts[2], 16);
                locationCounter = startingAddress;
                outputLines.append("-\t").append(parts[0]).append("\t").append(parts[1]).append("\t").append(parts[2]).append("\n");
                continue;
            }

            // Handle END directive
            if (parts[0].equals("END")) {
                outputLines.append(String.format("%X", locationCounter)).append("\t-\tEND\t-\n");
                break;
            }

            String label = parts.length > 2 ? parts[0] : "-";
            String opcode = label.equals("-") ? parts[0] : parts[1];
            String operand = parts.length > 2 ? parts[2] : (parts.length > 1 ? parts[1] : "-");

            if (!label.equals("-") && !symbolTable.containsKey(label)) {
                symbolTable.put(label, String.format("%X", locationCounter));
            }

            // Handle directives (WORD, RESW, RESB, BYTE)
            if (opcode.equals("WORD")) {
                outputLines.append(String.format("%X", locationCounter)).append("\t").append(label)
                        .append("\t").append(opcode).append("\t").append(operand).append("\n");
                locationCounter += 3;
                continue;
            } else if (opcode.equals("RESW")) {
                outputLines.append(String.format("%X", locationCounter)).append("\t").append(label)
                        .append("\t").append(opcode).append("\t").append(operand).append("\n");
                locationCounter += 3 * Integer.parseInt(operand);
                continue;
            } else if (opcode.equals("RESB")) {
                outputLines.append(String.format("%X", locationCounter)).append("\t").append(label)
                        .append("\t").append(opcode).append("\t").append(operand).append("\n");
                locationCounter += Integer.parseInt(operand);
                continue;
            } else if (opcode.startsWith("BYTE")) {
                outputLines.append(String.format("%X", locationCounter)).append("\t").append(label)
                        .append("\t").append(opcode).append("\t").append(operand).append("\n");
                locationCounter += operand.startsWith("C'") ? operand.length() - 3 : (operand.length() - 2) / 2;
                continue;
            }

            if (!opcodeTable.containsKey(opcode)) {
                throw new Exception("Invalid opcode: " + opcode);
            }

            outputLines.append(String.format("%X", locationCounter)).append("\t").append(label)
                    .append("\t").append(opcode).append("\t").append(operand).append("\n");
            locationCounter += 3;
        }

        programLength = locationCounter - startingAddress;
        return outputLines.toString();
    }

    // Pass 2: Generate object code
    private String passTwo() {
        StringBuilder objectCode = new StringBuilder();
        StringBuilder textRecord = new StringBuilder();
        int textStartAddress = startingAddress;
        boolean firstTextRecord = true; // Flag to check if it's the first text record

        // Header Record
        objectCode.append("H^COPY^").append(String.format("%06X^", startingAddress))
                .append(String.format("%06X\n", programLength));

        // Create the object code from Pass 1 output
        for (String line : pass1Output.getText().split("\\n")) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 4) continue;

            String label = parts[1];
            String opcode = parts[2];
            String operand = parts[3];

            if (opcode.equals("END")) {
                // Output the text record if it exists
                if (textRecord.length() > 0) {
                    objectCode.append("T^").append(String.format("%06X^", textStartAddress))
                            .append(textRecord).append("\n");
                }
                objectCode.append("E^").append(String.format("%06X\n", startingAddress));
                break;
            }

            String objectCodeLine = "";

            // Generate object code for WORD and BYTE directives
            if (opcode.equals("WORD")) {
                objectCodeLine = String.format("%06X", Integer.parseInt(operand));
            } else if (opcode.equals("BYTE")) {
                if (operand.startsWith("C'")) {
                    StringBuilder chars = new StringBuilder();
                    for (char c : operand.substring(2, operand.length() - 1).toCharArray()) {
                        chars.append(String.format("%02X", (int) c));
                    }
                    objectCodeLine = chars.toString();
                } else if (operand.startsWith("X'")) {
                    objectCodeLine = operand.substring(2, operand.length() - 1);
                }
            } else if (opcodeTable.containsKey(opcode)) {
                String opcodeHex = opcodeTable.get(opcode);
                String operandAddress = symbolTable.getOrDefault(operand, "0000");
                objectCodeLine = opcodeHex + operandAddress;
            }

            // Add a '^' before each object code line
            if (textRecord.length() > 0) {
                textRecord.append("^");
            }
            textRecord.append(objectCodeLine);

            // Check for text record length limit
            if (textRecord.length() / 3 > 30) { // Limit is 30 bytes (not 69 characters)
                objectCode.append("T^").append(String.format("%06X^", textStartAddress))
                        .append(textRecord).append("\n");
                textRecord = new StringBuilder(); // Reset for the next record
                textStartAddress += 3; // Increment the text start address by 3
            }
        }

       

        return objectCode.toString();
    }

    // Main method to run the application
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new AssemblerApp();
            }
        });
    }
}
