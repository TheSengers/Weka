package weka.gui.comparitizer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.GridLayout;
import java.awt.FlowLayout;

public class Comparitizer extends JPanel {

	/*
	 * Gui Elements for Comparitizer 
	 */
	protected JPanel comparitizFrame;
	protected JTabbedPane comparType;
	protected JPanel comparContent;
	protected JTextArea comparContLeft;
	protected JTextArea comparContRight;
	protected JMenuBar menuBar;
	protected JMenu menuFile;
	protected JMenuItem menuLoad;
	/**
	 * Create the panel.
	 */
	public Comparitizer() {
		setLayout(new GridLayout(1, 2, 0, 0));
		
		comparitizFrame = new JPanel();
		add(comparitizFrame);
		comparitizFrame.setLayout(new BorderLayout(0, 0));
		
		comparType = new JTabbedPane(JTabbedPane.TOP);
		comparitizFrame.add(comparType);
		
		comparContent = new JPanel();
		comparType.addTab("Clustering", null, comparContent, null);
		comparContent.setLayout(new GridLayout(0, 2, 10, 0));
		
		comparContLeft = new JTextArea();
		comparContent.add(comparContLeft);
		comparContLeft.setLineWrap(true);
		comparContLeft.setText("This is another test yolo");
		
		comparContRight = new JTextArea();
		comparContent.add(comparContRight);
		comparContRight.setLineWrap(true);
		comparContRight.setText("this is a test yolo");
		
		JPanel panel_1 = new JPanel();
		comparType.addTab("Others", null, panel_1, null);
		
		set_up_menubar();

	}
	protected void set_up_menubar(){
		if(comparitizFrame != null){
			menuBar = new JMenuBar();
			comparitizFrame.add(menuBar, BorderLayout.NORTH);
			
			menuFile = new JMenu("File");
			menuBar.add(menuFile);
			
			menuLoad = new JMenuItem("Load");
			menuFile.add(menuLoad);
			menuLoad.addActionListener(new ActionListener() {
		        @Override
		        public void actionPerformed(ActionEvent e) {
		            JFileChooser chooser = new JFileChooser();
		            //change this based on what we are loading
		            FileNameExtensionFilter filter = new FileNameExtensionFilter(
		                "JPG & GIF Images", "jpg", "gif");
		            
		            chooser.setFileFilter(filter);
		            int returnVal = chooser.showOpenDialog(getParent());
		            if(returnVal == JFileChooser.APPROVE_OPTION) {
		               System.out.println("You chose to open this file: " +
		                    chooser.getSelectedFile().getName());
		            }
		          }
		        });
		}
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
