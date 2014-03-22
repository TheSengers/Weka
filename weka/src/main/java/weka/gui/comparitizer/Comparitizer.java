package weka.gui.comparitizer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import weka.clusterers.Clusterer;
import weka.core.Drawable;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.gui.Logger;
import weka.gui.ResultHistoryPanel;
import weka.gui.SysErrLog;

import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;

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
	 /** The output area for classification results */
	 protected JTextArea m_OutText = new JTextArea(20, 40);
	 /** A panel controlling results viewing */
	 protected ResultHistoryPanel m_History = new ResultHistoryPanel(m_OutText);
	 /** The file chooser for selecting model files */
	 protected JFileChooser m_FileChooser = new JFileChooser(new File(
	   System.getProperty("user.dir")));
	 /** The destination for log/status messages */
	 protected Logger m_Log = new SysErrLog();
	 private JPanel panel;
	/**
	 * Create the panel.
	 */
	public Comparitizer() {
	    m_History.setBorder(BorderFactory
	    	      .createTitledBorder("Result list (right-click for options)"));
		setLayout(new GridLayout(1, 2, 0, 0));
		
		comparitizFrame = new JPanel();
		add(comparitizFrame);
		comparitizFrame.setLayout(new BorderLayout(0, 0));
		
		comparType = new JTabbedPane(JTabbedPane.TOP);
		comparitizFrame.add(comparType, BorderLayout.CENTER);
		
		comparContent = new JPanel();
		comparType.addTab("Clustering", null, comparContent, null);
		comparContent.setLayout(new BorderLayout());
		
		comparContent.add(m_History, BorderLayout.WEST);	
	    
	    panel = new JPanel();
	    panel.setBorder(new TitledBorder(null, "Preview Pane", TitledBorder.LEADING, TitledBorder.TOP, null, null));
	    comparContent.add(panel, BorderLayout.CENTER);
	    // Connect / configure the components
	    m_OutText.setEditable(false);
	    m_OutText.setFont(new Font("Monospaced", Font.PLAIN, 12));
	    //m_OutText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	    m_OutText.addMouseListener(new MouseAdapter() {
	      @Override
	      public void mouseClicked(MouseEvent e) {
	        if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != InputEvent.BUTTON1_MASK) {
	          m_OutText.selectAll();
	        }
	      }
	    });
	    panel.setLayout(new BorderLayout(0, 0));
	    //comparContLeft = new JTextArea();
	    final JScrollPane js = new JScrollPane(m_OutText);
	    panel.add(js);
	    js.setBorder(null);
	    js.getViewport().addChangeListener(new ChangeListener() {
	      private int lastHeight;

	      @Override
	      public void stateChanged(ChangeEvent e) {
	        JViewport vp = (JViewport) e.getSource();
	        int h = vp.getViewSize().height;
	        if (h != lastHeight) { // i.e. an addition not just a user scrolling
	          lastHeight = h;
	          int x = h - vp.getExtentSize().height;
	          vp.setViewPosition(new Point(0, x));
	        }
	      }
	    });
		
		
		//comparContLeft.setLineWrap(true);
		//comparContLeft.setText("This is another test yolo");

		/*comparContRight = new JTextArea();
		comparContent.add(comparContRight);
		comparContRight.setLineWrap(true);
		comparContRight.setText("this is a test yolo");*/
		
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
		           /* JFileChooser chooser = new JFileChooser();
		            //change this based on what we are loading
		            FileNameExtensionFilter filter = new FileNameExtensionFilter(
		                "JPG & GIF Images", "jpg", "gif");
		            
		            chooser.setFileFilter(filter);
		            int returnVal = chooser.showOpenDialog(getParent());
		            if(returnVal == JFileChooser.APPROVE_OPTION) {
		               System.out.println("You chose to open this file: " +
		                    chooser.getSelectedFile().getName());
		            }*/
		        	switch(comparType.getSelectedIndex()){
		        	case 0:	//current tab is clustering
		        		loadClusterer();
		        		break;
		        	case 1: //current tab is other
		        		JOptionPane.showMessageDialog(null, "The \""+comparType.getTitleAt(comparType.getSelectedIndex())+"\" tab was selected.\nThis illustrates contextual functionality.");
		        		break;
		        		
		        	}
		        	
		          }
		        });
		}
	}
	
	 /**
	  * Loads a clusterer
	  */
	 protected void loadClusterer() {

	   int returnVal = m_FileChooser.showOpenDialog(this);
	   if (returnVal == JFileChooser.APPROVE_OPTION) {
	     File selected = m_FileChooser.getSelectedFile();
	     Clusterer clusterer = null;
	     Instances trainHeader = null;
	     int[] ignoredAtts = null;

	     m_Log.statusMessage("Loading model from file...");

	     try {
	       InputStream is = new FileInputStream(selected);
	       if (selected.getName().endsWith(".gz")) {
	         is = new GZIPInputStream(is);
	       }
	       ObjectInputStream objectInputStream = new ObjectInputStream(is);
	       clusterer = (Clusterer) objectInputStream.readObject();
	       try { // see if we can load the header & ignored attribute info
	         trainHeader = (Instances) objectInputStream.readObject();
	         ignoredAtts = (int[]) objectInputStream.readObject();
	       } catch (Exception e) {
	       } // don't fuss if we can't
	       objectInputStream.close();
	     } catch (Exception e) {

	       JOptionPane.showMessageDialog(null, e, "Load Failed",
	         JOptionPane.ERROR_MESSAGE);
	     }

	     m_Log.statusMessage("OK");

	     if (clusterer != null) {
	       m_Log.logMessage("Loaded model from file '" + selected.getName() + "'");
	       String name = (new SimpleDateFormat("HH:mm:ss - ")).format(new Date());
	       String cname = clusterer.getClass().getName();
	       if (cname.startsWith("weka.clusterers.")) {
	         cname = cname.substring("weka.clusterers.".length());
	       }
	       name += cname + " from file '" + selected.getName() + "'";
	       StringBuffer outBuff = new StringBuffer();

	       outBuff.append("=== Model information ===\n\n");
	       outBuff.append("Filename:     " + selected.getName() + "\n");
	       outBuff.append("Scheme:       " + clusterer.getClass().getName());
	       if (clusterer instanceof OptionHandler) {
	         String[] o = ((OptionHandler) clusterer).getOptions();
	         outBuff.append(" " + Utils.joinOptions(o));
	       }
	       outBuff.append("\n");

	       if (trainHeader != null) {

	         outBuff.append("Relation:     " + trainHeader.relationName() + '\n');
	         outBuff.append("Attributes:   " + trainHeader.numAttributes() + '\n');
	         if (trainHeader.numAttributes() < 100) {
	           boolean[] selectedAtts = new boolean[trainHeader.numAttributes()];
	           for (int i = 0; i < trainHeader.numAttributes(); i++) {
	             selectedAtts[i] = true;
	           }

	           if (ignoredAtts != null) {
	             for (int i = 0; i < ignoredAtts.length; i++) {
	               selectedAtts[ignoredAtts[i]] = false;
	             }
	           }

	           for (int i = 0; i < trainHeader.numAttributes(); i++) {
	             if (selectedAtts[i]) {
	               outBuff.append("              "
	                 + trainHeader.attribute(i).name() + '\n');
	             }
	           }
	           if (ignoredAtts != null) {
	             outBuff.append("Ignored:\n");
	             for (int ignoredAtt : ignoredAtts) {
	               outBuff.append("              "
	                 + trainHeader.attribute(ignoredAtt).name() + '\n');
	             }
	           }
	         } else {
	           outBuff.append("              [list of attributes omitted]\n");
	         }
	       } else {
	         outBuff.append("\nTraining data unknown\n");
	       }

	       outBuff.append("\n=== Clustering model ===\n\n");
	       outBuff.append(clusterer.toString() + "\n");

	       m_History.addResult(name, outBuff);
	       m_History.setSingle(name);
	       ArrayList<Object> vv = new ArrayList<Object>();
	       vv.add(clusterer);
	       if (trainHeader != null) {
	         vv.add(trainHeader);
	       }
	       if (ignoredAtts != null) {
	         vv.add(ignoredAtts);
	       }
	       // allow visualization of graphable classifiers
	       String grph = null;
	       if (clusterer instanceof Drawable) {
	         try {
	           grph = ((Drawable) clusterer).graph();
	         } catch (Exception ex) {
	         }
	       }
	       if (grph != null) {
	         vv.add(grph);
	       }

	       m_History.addObject(name, vv);

	     }
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
