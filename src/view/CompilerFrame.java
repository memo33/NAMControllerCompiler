package view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.SwingConstants;

import controller.NAMControllerCompilerMain;

/**
 * A custom Frame for the (developer's) perspective of the NAM Compiler.
 * @author memo
 */
@SuppressWarnings("serial")
public class CompilerFrame extends JFrame {
    
    private static final boolean isMacOS = System.getProperty("os.name").toLowerCase().startsWith("mac os x"); 

    private final String descriptionTextResource = "description text.txt";
    
	private JTextField[] fields = new JTextField[2];
	private JRadioButton[] radioButtons = new JRadioButton[2];
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
		return radioButtons[1].isSelected();
	}
	
//	public boolean isESeries() {
//		return checks[1].isSelected();
//	}
	
	public void addStartButtonListener(ActionListener l) {
		this.startButton.addActionListener(l);
	}
	
//	public CompilerFrame(JTree tree) {
//		this("", "", false, tree);
//	}
	
	public CompilerFrame(boolean detailed, String inputPath, String outputPath, boolean isLHD, JTree tree) {
		this.setTitle("NAM Controller Compiler");
		GridBagConstraints c = new GridBagConstraints();
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setPreferredSize(new Dimension(detailed ? 800 : 700, detailed ? 660 : 600));

		c.insets = new Insets(3,3,3,3);
		
		int y = 0;

		c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = 2;
        c.gridx = 1;
        c.gridy = y++;
        JLabel descLabel = new JLabel(this.loadDescriptionText());
//        descLabel.setBorder(BorderFactory.createEtchedBorder());
        panel.add(descLabel, c);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = 4;
        c.gridx = 0;
        c.gridy = y++;
        panel.add(new JSeparator(SwingConstants.HORIZONTAL), c);
		
		if (detailed) {
    		for (int i = 0; i < 2; i++, y++) {
    			JLabel label = new JLabel(i==0 ? "Input: " : "Output: ");
    			c.fill = GridBagConstraints.BOTH;
    			c.weightx = 0.0;
    			c.weighty = 0.0;
    			c.gridx = 0;
    			c.gridy = y;
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
    					File file = new File(CompilerFrame.this.fields[ii].getText());
    					if (isMacOS) {
    					    System.setProperty("apple.awt.fileDialogForDirectories", "true");
    					    FileDialog dialog = new FileDialog(CompilerFrame.this);
    					    if (file.exists()) {
    					        dialog.setDirectory(file.getAbsolutePath());
    					    }
    					    dialog.setVisible(true);
    					    System.setProperty("apple.awt.fileDialogForDirectories", "false");
    					    String dir = dialog.getDirectory();
    					    String folder = dialog.getFile();
    					    if (dir != null && folder != null) {
    					        File selectedFile = new File(dir, folder);
                                fields[ii].setText(selectedFile.getAbsolutePath());
    					    }
    					    dialog.dispose();
    					} else {
        					JFileChooser chooser = !file.exists() ? new JFileChooser() : new JFileChooser(file);
        					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        					if (chooser.showOpenDialog(CompilerFrame.this)
        							== JFileChooser.APPROVE_OPTION) {
    							fields[ii].setText(chooser.getSelectedFile().getAbsolutePath());
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
		}

		ButtonGroup buttonGroup = new ButtonGroup();
		for (int i = 0; i < radioButtons.length; i++) {
		    radioButtons[i] = new JRadioButton((i==0 ? "Right" : "Left") + "-hand drive");
		    buttonGroup.add(radioButtons[i]);
		    c.fill = GridBagConstraints.BOTH;
		    c.gridwidth = 1;
		    c.weightx = 1./radioButtons.length;
		    c.weighty = 0.0;
		    c.gridx = 1 + i;
		    c.gridy = y;
		    panel.add(radioButtons[i], c);
		}
		radioButtons[isLHD ? 1 : 0].setSelected(true);
		y++;
		
		tree.setVisibleRowCount(15);
		JScrollPane treeView = new JScrollPane(tree);
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.0;
		c.weighty = 1.0;
		c.gridwidth = 2;
		c.gridx = 1;
		c.gridy = y++;
		panel.add(treeView, c);
		
		startButton = new JButton("Start Compilation");
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.gridwidth = 2;
		c.gridx = 1;
		c.gridy = y++;
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.add(startButton, BorderLayout.EAST);
		panel.add(wrapper, c);

		this.getRootPane().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		this.add(panel);
		this.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy() {
            @Override
            public Component getDefaultComponent(Container aContainer) {
                return CompilerFrame.this.startButton;
            }
        });
	}

	private String loadDescriptionText() {
	    InputStream is = null;
	    Scanner scanner = null;
	    try {
    	    is = this.getClass().getResourceAsStream(descriptionTextResource);
    	    scanner = new Scanner(is).useDelimiter("\\A"); // first match matches the whole text
    	    return scanner.hasNext() ? scanner.next() : "FAILED TO LOAD DESCRIPTION";
	    } finally {
	        if (scanner != null) {
	            scanner.close();
	        }
	        if (is != null) {
	            try {
                    is.close();
                } catch (IOException e) {
                    NAMControllerCompilerMain.LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
                }
	        }
	    }
	}
}

