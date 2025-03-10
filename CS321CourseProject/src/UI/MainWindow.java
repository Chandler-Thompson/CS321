package UI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JViewport;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;
import java.awt.CardLayout;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import java.awt.FlowLayout;

import Map.Node;
import User.Profile;

public class MainWindow {

	private MainFrame frame;
	private MapPanel mapImagePanel;
	
	private BufferedImage mapImage;

	private JButton setStartingPointButton;
	private JButton btnSetDestination; 
	private JButton btnCalculatePath;
	private JButton btnClearSetNodes;
	private JButton btnTakeScreenshot;
	private JButton btnClearSelectedNodes;
	private JButton btnSavePath;
	
	private static final int width = 1280;
	private static final int height = 720;
	private static final float resizeWeight = 0.3333f;
	private static final float dividerLocation = 0.3333f;
	
	private static final String regularCampusMapPath = "src\\Res\\campus_map.png";
	private static final String campusMapForNodesPath = "src\\Res\\CampusMapForNodes.png";
	private static final String campusMapForNodesJpgPath = "src\\Res\\CampusMapForNodes.jpg";

	private String displayImagePath = null;
	private String nodesImagePath = null;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow(regularCampusMapPath, campusMapForNodesJpgPath);
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 * @throws IOException 
	 */
	public MainWindow(String displayImagePath, String nodesImagePath) throws IOException {
		this.displayImagePath = displayImagePath;
		this.nodesImagePath = nodesImagePath;
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		//TODO: Remove "TestUser" and replace with actual login
		frame = new MainFrame("TestUser");
		frame.setSize(width, height);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] {width};
		gridBagLayout.rowHeights = new int[] {0};
		gridBagLayout.columnWeights = new double[]{1.0};
		gridBagLayout.rowWeights = new double[]{1.0};
		frame.getContentPane().setLayout(gridBagLayout);
		
		// Create JPanels for the map and the control area.

		//JPanel controlPanel = new JPanel();
		//JPanel mapJPanel = new JPanel();
		
		// We use a split pane to divide the UI into two separate sections: controls & map.
		//JSplitPane splitPane = new JSplitPane(SwingConstants.VERTICAL, controlPanel, mapJPanel);

		//JPanel controlPanel = new JPanel();
		//JPanel locationPanel = new JPanel();
	    JPanel mapPanel = new JPanel();
		//UserLocationPanel locationPanel = new UserLocationPanel(mapPanel,true);
		// We use a split pane to divide the UI into two separate sections: controls & map.
		//JSplitPane splitPane = new JSplitPane(SwingConstants.VERTICAL, controlPanel, mapPanel);
		
		
		ImageIcon mapIcon = new ImageIcon(this.displayImagePath);
		final JLabel mapImageLabel = new JLabel(mapIcon);
		
		mapImageLabel.setPreferredSize(new Dimension(mapIcon.getIconWidth(), mapIcon.getIconHeight()));
		
		/*mapPanel = new MapPanel(frame, this.displayImagePath, this.nodesImagePath);
		mapPanel.setPreferredSize(new Dimension(mapIcon.getIconWidth(), mapIcon.getIconHeight()));
		mapJPanel.add(mapPanel);
		mapPanel.setPreferredSize(new Dimension(mapIcon.getIconWidth(), mapIcon.getIconHeight()));*/
		mapPanel.setLayout(new GridLayout(0, 1, 0, 0));
		
		JScrollPane scrollPane = new JScrollPane(mapImageLabel) {	
			/*@Override
		    protected void processMouseWheelEvent(MouseWheelEvent e) {
				if (frame.getControlPressed()) 
					System.out.println("Yeet");
		        super.processMouseWheelEvent(e);
		    }*/
		};
		
		scrollPane.getVerticalScrollBar().setUnitIncrement(12);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(12);
		
		scrollPane.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
		
		mapImagePanel = new MapPanel(frame, this.displayImagePath, this.nodesImagePath);
		mapImagePanel.setPreferredSize(new Dimension(mapIcon.getIconWidth(), mapIcon.getIconHeight()));
		mapPanel.add(mapImagePanel);
		mapImagePanel.setPreferredSize(new Dimension(mapIcon.getIconWidth(), mapIcon.getIconHeight()));
		mapImagePanel.setBackground(new Color(176, 224, 230));
		UserLocationPanel locationPanel = new UserLocationPanel(mapImagePanel);
		
		JSplitPane splitPane = new JSplitPane(SwingConstants.VERTICAL, locationPanel, mapPanel);	
		splitPane.setDividerLocation(dividerLocation);
		splitPane.setResizeWeight(resizeWeight);

		splitPane.setOrientation(SwingConstants.VERTICAL);
		splitPane.setBackground(Color.GREEN);
		GridBagConstraints gbc_splitPane = new GridBagConstraints();
		gbc_splitPane.weighty = 1.0;
		gbc_splitPane.weightx = 1.0;
		gbc_splitPane.insets = new Insets(0, 0, 5, 0);
		gbc_splitPane.fill = GridBagConstraints.BOTH;
		gbc_splitPane.gridx = 0;
		gbc_splitPane.gridy = 0;
		frame.getContentPane().add(splitPane, gbc_splitPane);
		
		//locationPanel.setMapPanel(mapImagePanel);
		
		//userLocationMenuPanel.setPrefferedSize()
		JButton setStartingPointButton = new JButton("Set Starting Point");
		setStartingPointButton.setBackground(new Color(176, 224, 230));
		setStartingPointButton.setFont(new Font("Yu Gothic UI Semibold", Font.PLAIN, 11));
		setStartingPointButton.setBounds(10, 576, 125, 23);
		setStartingPointButton.setPreferredSize(new  Dimension (200,23));
		setStartingPointButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mapImagePanel.setNextClickStart(true);
			}
		});
		locationPanel.add(setStartingPointButton);

		JButton btnSetDestination = new JButton("Set Destination");
		btnSetDestination.setBackground(new Color(176, 224, 230));
		btnSetDestination.setFont(new Font("Yu Gothic UI Semibold", Font.PLAIN, 11));
		btnSetDestination.setBounds(145, 576, 125, 23);
		btnSetDestination.setPreferredSize(new  Dimension (200,23));

		btnSetDestination.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mapImagePanel.setNextClickDest(true);
			}
		});		
		locationPanel.add(btnSetDestination);

		JButton btnCalculatePath = new JButton("Calculate Path");
		btnCalculatePath.setFont(new Font("Yu Gothic UI Semibold", Font.PLAIN, 11));
		btnCalculatePath.setBackground(new Color(176, 224, 230));
		btnCalculatePath.setBounds(280, 576, 125, 23);

		btnCalculatePath.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mapImagePanel.generatePaths();
			}
		});
		locationPanel.add(btnCalculatePath);
		
		JButton btnClearSetNodes = new JButton("Clear Path");
		btnClearSetNodes.setFont(new Font("Yu Gothic UI Semibold", Font.PLAIN, 11));
		btnClearSetNodes.setBackground(new Color(176, 224, 230));
		btnClearSetNodes.setBounds(10, 633, 125, 23);

		btnClearSetNodes.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mapImagePanel.clearPathNodes();
			}
		});
		locationPanel.add(btnClearSetNodes);
		

		JButton btnSavePath = new JButton("Save Path");
		btnSavePath.setFont(new Font("Yu Gothic UI Semibold", Font.PLAIN, 11));
		btnSavePath.setBackground(new Color(176, 224, 230));
		btnSavePath.setBounds(145, 633, 125, 23);
		btnSavePath.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mapImagePanel.savePath();
			}
		});
		locationPanel.add(btnSavePath);

		JButton btnSaveSelectedNodes = new JButton("Save Selection");
		btnSaveSelectedNodes.setFont(new Font("Yu Gothic UI Semibold", Font.PLAIN, 11));
		btnSaveSelectedNodes.setBackground(new Color(176, 224, 230));
		btnSaveSelectedNodes.setBounds(145, 690, 125, 23);
		btnSaveSelectedNodes.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mapImagePanel.saveSelection();
			}
		});
		locationPanel.add(btnSaveSelectedNodes);

		JButton btnClearSelectedNodes = new JButton("Clear Selection");
		btnClearSelectedNodes.setFont(new Font("Yu Gothic UI Semibold", Font.PLAIN, 11));
		btnClearSelectedNodes.setBackground(new Color(176, 224, 230));
		btnClearSelectedNodes.setBounds(280, 633, 125, 23);
		btnClearSelectedNodes.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mapImagePanel.clearSelection();
			}
		});
		locationPanel.add(btnClearSelectedNodes);

		JButton btnTakeScreenshot = new JButton("Take Screenshot");
		btnTakeScreenshot.setFont(new Font("Yu Gothic UI Semibold", Font.PLAIN, 11));
		btnTakeScreenshot.setBackground(new Color(176, 224, 230));
		btnTakeScreenshot.setBounds(145, 519, 125, 23);
		btnTakeScreenshot.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mapImagePanel.takeScreenshot();
			}
		});
		locationPanel.add(btnTakeScreenshot);
		

	}
	
	public JButton getStartingPointButton() {
		return setStartingPointButton;
	}
	
	public JButton getSetDestinationButton() {
		return btnSetDestination;
	}
	
	public JButton getCalculatePathButton() {
		return btnCalculatePath;
	}
	
	public JButton getClearSetNodesButton() {
		return btnClearSetNodes;
	}
	
	public JButton getTakeScreenshotButton() {
		return btnTakeScreenshot;
	}
	
	public JButton getClearSelectedNodesButton() {
		return btnClearSelectedNodes;
	}
	
	public JButton getSavePathButton() {
		return btnSavePath;
	}	
	
	public MainFrame getMainFrame() {
		return this.frame;
	}
	
	public MapPanel getMapPanel() {
		return this.mapImagePanel;
	}
}
