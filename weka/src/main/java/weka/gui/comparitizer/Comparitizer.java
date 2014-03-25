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
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;

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

import java.awt.Component;

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
	 private JPanel m_RightPanel;
	 private JTextArea m_FileInfo;
	 private JPanel m_LeftPanel;
	 private JPanel m_FileInfoPanel;
	 private String file_info;
	 private FileNameExtensionFilter filter = new FileNameExtensionFilter("Cluster Model", "model");
	/**
	 * Create the panel.
	 */
	public Comparitizer() {
		setLayout(new GridLayout(1, 2, 0, 0));
		
		comparitizFrame = new JPanel();
		add(comparitizFrame);
		comparitizFrame.setLayout(new BorderLayout(0, 0));
		
		comparType = new JTabbedPane(JTabbedPane.TOP);
		comparitizFrame.add(comparType, BorderLayout.CENTER);
		
		comparContent = new JPanel();
		comparType.addTab("Clustering", null, comparContent, null);
		comparContent.setLayout(new BorderLayout());
	    
	    m_RightPanel = new JPanel();
	    m_RightPanel.setBorder(new TitledBorder(null, "Preview Pane", TitledBorder.LEADING, TitledBorder.TOP, null, null));
	    comparContent.add(m_RightPanel, BorderLayout.CENTER);
	    // Connect / configure the components
	    m_OutText.setEditable(false);
	    m_OutText.setFont(new Font("Monospaced", Font.PLAIN, 12));
	    //m_OutText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	    
	    //if the preview windo is right clicked, all the text is selected
	    m_OutText.addMouseListener(new MouseAdapter() {
	      @Override
	      public void mouseClicked(MouseEvent e) {
	        if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != InputEvent.BUTTON1_MASK) {
	        	m_OutText.selectAll();
	        }
	      }
	    });
	    m_RightPanel.setLayout(new BorderLayout(0, 0));
	    //comparContLeft = new JTextArea();
	    final JScrollPane js = new JScrollPane(m_OutText);
	    m_RightPanel.add(js);
	    js.setBorder(null);
	    
	    //creates the left panel for file info and data select
	    m_LeftPanel = new JPanel();
	    comparContent.add(m_LeftPanel, BorderLayout.WEST);
	    m_LeftPanel.setLayout(new GridLayout(0, 1, 0, 0));
	    
	    //creates the view for the file info
	    m_FileInfoPanel = new JPanel();
	    m_LeftPanel.add(m_FileInfoPanel);
	    m_FileInfoPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "File Info", TitledBorder.LEADING, TitledBorder.TOP, null, null));
	    m_FileInfoPanel.setLayout(new GridLayout(0, 1, 0, 0));
	    m_FileInfo = new JTextArea();
	    m_FileInfo.setEditable(false);
	    m_FileInfo.setFont(new Font("Monospaced", Font.PLAIN, 12));
	    final JScrollPane fileinfo_scroll = new JScrollPane(m_FileInfo);
	    fileinfo_scroll.setPreferredSize(m_FileInfo.getPreferredSize());
	    m_FileInfoPanel.add(fileinfo_scroll);
	    fileinfo_scroll.setBorder(null);
	    
	    //sets the caret so that the scroll bar always starts at the top
	    DefaultCaret caret = (DefaultCaret)m_FileInfo.getCaret();
	    caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);


	    //adds the history 
	    m_LeftPanel.add(m_History);
	    m_History.setBorder(BorderFactory
	    	      .createTitledBorder("Result list (right-click for options)"));
	    m_History.setHandleRightClicks(false);
	    //mouse listener for updating file info
	    m_History.getList().addMouseListener(new MouseAdapter() {
		      @Override
		      public void mouseClicked(MouseEvent e) {
		        if ((e.getModifiers() & InputEvent.BUTTON2_MASK) != InputEvent.BUTTON2_MASK) {
		        	set_file_info(m_History.getSelectedBuffer().toString());
		        }
		        if (e.getClickCount() == 2) {
		            m_History.openFrame(m_History.getSelectedName());
		        }
		      }
		    });
	    
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
    
    /*
     * Sets up the menu bar at the top for features like load file
     * The load functionality is based on the current tab
     */
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
	  * Loads a clusterer. Reused from Weka's explorer load function.
	  */
	 protected void loadClusterer() {
         //change this based on what we are loading
	   m_FileChooser.setFileFilter(filter);
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

	       //set file info string to the buffer 
	       file_info = outBuff.toString();
		    //displays the file info in the test label as well
	       m_FileInfo.setText(file_info);
	       
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
	 
    // sets the text for the file info text area
	public void set_file_info(String msg){
		if(m_FileInfo != null){
			//get the stop index
			int index = msg.indexOf("=== Clustering model ===");
			String shortened = msg.substring(0, index);
			m_FileInfo.setText(shortened);
		}
	}
	
	public static void main(String[] args) {
		JFrame m_ComparFrame = new JFrame("Comparitizer test");
		  System.out.println("Comparitizor!!!!");
		    	m_ComparFrame = new JFrame("Weka Comparitizer");
		    	m_ComparFrame.getContentPane().setLayout(new BorderLayout());
		    	m_ComparFrame.getContentPane().add(new Comparitizer());
		    	m_ComparFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		    	m_ComparFrame.setLocationRelativeTo(null);
		    	m_ComparFrame.pack();
		    	m_ComparFrame.setSize(800,600);
		    	m_ComparFrame.setVisible(true);		    
	}

}
