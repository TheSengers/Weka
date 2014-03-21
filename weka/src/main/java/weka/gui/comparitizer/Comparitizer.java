package weka.gui.comparitizer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.*;
import java.awt.GridLayout;
import java.awt.FlowLayout;

public class Comparitizer extends JPanel {

	/**
	 * Create the panel.
	 */
	public Comparitizer() {
		setLayout(new GridLayout(1, 2, 0, 0));
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		add(tabbedPane);
		
		JPanel panel = new JPanel();
		tabbedPane.addTab("Clustering", null, panel, null);
		panel.setLayout(new GridLayout(0, 2, 10, 0));
		
		JTextArea txtrThisIsAnother = new JTextArea();
		panel.add(txtrThisIsAnother);
		txtrThisIsAnother.setLineWrap(true);
		txtrThisIsAnother.setText("This is another test yolo");
		
		JTextArea txtrThisIsA = new JTextArea();
		panel.add(txtrThisIsA);
		txtrThisIsA.setLineWrap(true);
		txtrThisIsA.setText("this is a test yolo");
		
		JPanel panel_1 = new JPanel();
		tabbedPane.addTab("Others", null, panel_1, null);

	}
	
	public static void main(String[] args) {
		JFrame m_ComparFrame = new JFrame("Comparitizer test");
		  System.out.println("Comparitizor!!!!");
		    	m_ComparFrame = new JFrame("Weka Comparitizer");
		    	m_ComparFrame.getContentPane().setLayout(new BorderLayout());
		    	m_ComparFrame.getContentPane().add(new Comparitizer());
		    	m_ComparFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		    	//JLabel textLabel = new JLabel("I'm a label in the window",SwingConstants.CENTER);
		    	//textLabel.setPreferredSize(new Dimension(300, 100));
		    	//m_ComparFrame.getContentPane().add(textLabel, BorderLayout.CENTER);
		    	m_ComparFrame.setLocationRelativeTo(null);
		    	m_ComparFrame.pack();
		    	m_ComparFrame.setSize(800,600);
		    	m_ComparFrame.setVisible(true);		    
	}

}
