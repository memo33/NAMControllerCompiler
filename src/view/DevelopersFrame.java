package view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;

/**
 * A custom Frame for the (developer's) perspective of the NAM Compiler.
 * @author memo
 */
@SuppressWarnings("serial")
public class DevelopersFrame extends JFrame {

	private JTextField[] fields = new JTextField[2];
	private JCheckBox[] checks = new JCheckBox[2];
	private JButton startButton;
	
//	public void setEnabledStartButton(boolean b) {
//		startButton.setEnabled(b);
//	}
	
	public String getInputPath() {
		return fields[0].getText();
	}
	
	public String getOutputPath() {
		return fields[1].getText();
	}
	
	public boolean isLHD() {
		return checks[0].isSelected();
	}
	
	public boolean isESeries() {
		return checks[1].isSelected();
	}
	
	public void addStartListener(ActionListener l) {
		this.startButton.addActionListener(l);
	}
	
	public DevelopersFrame(JTree tree) {
		this("", "", false, true, tree);
	}
	
	public DevelopersFrame(String inputPath, String outputPath, boolean isLHD, boolean isESeries, JTree tree) {
		this.setTitle("NAM Controller Compiler");
		GridBagConstraints c = new GridBagConstraints();
		JPanel panel = new JPanel(new GridBagLayout());

		//		JFileChooser chooser1 = new JFileChooser();
		//		chooser1.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		//		chooser1.setVisible(true);
		//		chooser1.showOpenDialog(this);

		for (int i = 0; i < 2; i++) {
			JLabel label = new JLabel(i==0 ? "Input: " : "Output: ");
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 0.0;
			c.weighty = 0.0;
			c.gridx = 0;
			c.gridy = i;
			panel.add(label, c);

			fields[i] = new JTextField(36);
			fields[i].setText(i==0 ? inputPath : outputPath);
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1.0;
			c.weighty = 0.0;
			c.gridwidth = 2;
			c.gridx = 1;
			panel.add(fields[i], c);

			JButton button = new JButton("Browse");
			button.setToolTipText(i==0
					? "Select the directory that contains the folders RUL0, RUL1 and RUL2 which contain the source files."
					: "Select the directory where you want to save the compiled controller.");
			final int ii = i;
			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					File file = new File(DevelopersFrame.this.fields[ii].getText());
					JFileChooser chooser = !file.exists() ? new JFileChooser() : new JFileChooser(file);
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					if (chooser.showOpenDialog(DevelopersFrame.this)
							== JFileChooser.APPROVE_OPTION) {
						try {
							fields[ii].setText(chooser.getSelectedFile().getCanonicalPath());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
				}
			});
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 0.0;
			c.weighty = 0.0;
			c.gridwidth = 1;
			c.gridx = 3;
			panel.add(button, c);
		}

		for (int i = 0; i < checks.length; i++) {
			checks[i] = new JCheckBox(i==0 ? "Left-Hand Drive?" : "E-Series?");
			checks[i].setSelected(i==0 ? isLHD : isESeries);
//			c.fill = GridBagConstraints.HORIZONTAL;
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 0.5;
			c.weighty = 0.0;
			c.gridx = 1 + i;
			c.gridy = 2;
			panel.add(checks[i], c);
		}
		
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.gridwidth = 4;
		c.gridx = 0;
		c.gridy = 3;
		panel.add(new JSeparator(SwingConstants.HORIZONTAL), c);
		c.gridwidth = 2;
		c.gridx = 1;
		c.gridy = 4;
		panel.add(new JLabel("Select the networks to exclude from RUL2-stability:"), c);

		tree.setVisibleRowCount(15);
		JScrollPane treeView = new JScrollPane(tree);
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.0;
		c.weighty = 1.0;
		c.gridwidth = 2;
		c.gridx = 1;
		c.gridy = 5;
		panel.add(treeView, c);

		startButton = new JButton("Start");
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.gridwidth = 2;
		c.gridx = 1;
		c.gridy = 6;
		panel.add(startButton, c);

		this.getRootPane().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		this.add(panel);
	}

}
