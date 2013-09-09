package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class GUIView extends ConsoleView {
    
    private static final int WIDTH = 400;

    private JFrame frame;
    private ProgressPanel progressPanel;
    private JDialog dialog;
    
    public void setFrame(JFrame frame) {
        this.frame = frame;
    }
    
    @Override
    public void initProgress(String message, int min, int max) {
        super.initProgress(message, min, max);
        progressPanel = new ProgressPanel(message, "", min, max);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
//                Object[] options = {UIManager.getString("OptionPane.cancelButtonText")};
//                int result = JOptionPane.showOptionDialog(frame, progressPanel, null, JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, null);
//                if (result == 0) {
//                }
                dialog = new JDialog(frame, true);
                dialog.add(new JOptionPane(progressPanel, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[0]));
                dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                dialog.pack();
                dialog.setLocationByPlatform(true);
                dialog.setVisible(true);
            }
        });
    }
    
    @Override
    public void dispose() {
        super.dispose();
        if (dialog != null) {
            dialog.setVisible(false);
            dialog.dispose();
        }
        if (frame != null) {
            frame.setVisible(false);
            frame.dispose();
        }
    }
    
    @Override
    public void publishProgressIncrement(int increment, String note) {
        super.publishProgressIncrement(increment, note);
        progressPanel.incrementProgress(increment);
        progressPanel.setNote(note);
    }

    @Override
    public void publishException(String message, Throwable e) {
        super.publishException(message, e);
        JPanel stackTracePanel = new JPanel(new BorderLayout());
        {
            JLabel messageLabel = new JLabel(getWrappedLabelText(WIDTH, message));
            messageLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
            stackTracePanel.add(messageLabel, BorderLayout.NORTH);
        } {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            stackTracePanel.add(new JScrollPane(new JTextArea(sw.toString())));
        }
        stackTracePanel.setPreferredSize(new Dimension(600, 300));
        JOptionPane.showMessageDialog(frame, stackTracePanel, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void publishIssue(String formatMessage, Object... args) {
        super.publishIssue(formatMessage, args);
        JOptionPane.showMessageDialog(frame, getWrappedLabelText(WIDTH, formatMessage, args), "Issue", JOptionPane.ERROR_MESSAGE);
    }
    
    @Override
    public void publishInfoMessage(String formatMessage, Object... args) {
        super.publishInfoMessage(formatMessage, args);
        JOptionPane.showMessageDialog(frame, getWrappedLabelText(WIDTH, formatMessage, args));
    }

    @Override
    public boolean publishConfirmOption(String formatMessage, Object... args) {
        return super.publishConfirmOption(formatMessage, args) // always returns true;
                && (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                        frame, getWrappedLabelText(WIDTH, formatMessage, args), null, JOptionPane.YES_NO_OPTION));
    }
    
    private String getWrappedLabelText(int width, String formatMessage, Object... args) {
        String s = args.length == 0 ? formatMessage : MessageFormat.format(formatMessage, args);
        return "<html><body><p style='width: " + width + "px;'>" + 
                s + "</body></html>";
    }
}
