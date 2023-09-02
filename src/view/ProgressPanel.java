package view;

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

@SuppressWarnings("serial")
public class ProgressPanel extends JPanel {

    private JLabel noteLabel;
    private JProgressBar progressBar;

    public ProgressPanel(String message, String initialNote, int min, int max) {
        super(new GridLayout(3, 1));
        this.add(new JLabel(message));
        this.noteLabel = new JLabel(initialNote);
        this.add(this.noteLabel);
        this.progressBar = new JProgressBar(min, max);
        this.progressBar.setValue(0);
        this.add(this.progressBar);
        this.setPreferredSize(new Dimension(400, 0));
    }

    public void setNote(String note) {
        this.noteLabel.setText(note);
    }

    public void incrementProgress(int delta) {
        this.progressBar.setValue(this.progressBar.getValue() + delta);
    }
}
