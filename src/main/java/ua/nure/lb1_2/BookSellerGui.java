package ua.nure.lb1_2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

class BookSellerGui extends JFrame {
    private BookSellerAgent agent;
    private JTextField titleField;
    private JTextField priceField;
    private JButton addButton;

    BookSellerGui(BookSellerAgent agent) {
        super(agent.getLocalName());
        this.agent = agent;

        initComponents();
        initEventListeners();
    }

    public void showFrame() {
        pack();
        centerWindow();
        super.setVisible(true);
    }

    private void initComponents() {
        titleField = new JTextField(15);
        priceField = new JTextField(15);
        addButton = new JButton("Add");

        getContentPane().add(getRootPanel(), BorderLayout.CENTER);
        getContentPane().add(getBottomPanel(), BorderLayout.SOUTH);
        setResizable(false);
    }

    private JPanel getRootPanel() {
        JPanel rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayout(2, 2));

        rootPanel.add(new JLabel("Book title:"));
        rootPanel.add(titleField);

        rootPanel.add(new JLabel("Price:"));
        rootPanel.add(priceField);

        return rootPanel;
    }

    private JPanel getBottomPanel() {
        JPanel panel = new JPanel();
        panel.add(addButton);

        return panel;
    }

    private void initEventListeners() {
        addButton.addActionListener(e -> {
            try {
                validateFields();
                String title = titleField.getText().trim();
                String price = priceField.getText().trim();

                BookSellerGui.this.agent.updateCatalogue(title, Integer.parseInt(price));

                clearFields();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                agent.doDelete();
            }
        });
    }

    private void validateFields() {
        String title = titleField.getText().trim();
        String priceStr = priceField.getText().trim();

        if (title.length() == 0) {
            throw new IllegalArgumentException("Title is required");
        }
        if (title.length() > 250) {
            throw new IllegalArgumentException("Title is too long");
        }

        if (priceStr.length() == 0) {
            throw new IllegalArgumentException("Price is required");
        }
        int price = Integer.parseInt(priceStr);
        if (price < 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
    }

    private void clearFields() {
        titleField.setText("");
        priceField.setText("");
    }

    private void centerWindow() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int) screenSize.getWidth() / 2;
        int centerY = (int) screenSize.getHeight() / 2;
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
    }
}