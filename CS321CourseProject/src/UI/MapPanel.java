package UI;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import Map.EdgelessNode;
import Map.Node;
import Map.QueueNode;
import Map.Terrain;
import Pathfinding.ShortestPathAlgorithm;

// https://crab.rutgers.edu/~guyk/BFS.pdf
public class MapPanel extends JPanel implements MouseWheelListener, MouseListener, MouseMotionListener  {

	// NOTE: The default values are what positions the image in the middle of the window when first starting the application.
	// I just printed out the values every repaint(), positioned the image that way, and then used the values that were 
	// printed once I had positioned the image the way I liked it.
	
	/**
	 * This is the image of the map. 
	 */
	private final Image mapImage;
	
	/**
	 * This value is used when displaying visual indicators for nodes.
	 * 
	 * It is used as with width and height.
	 */
	private final int nodeVisualIndicationWidth = 13;
	
	/**
	 * Scaling the nodes across the larger image (that is displayed to the user) results in the nodes being
	 * slightly too high when displayed. Therefore we apply a semi-hard-coded translation downwards to nodes. 
	 * It is semi-hard-coded in that it changes depending on the current zoom.
	 */
	private final int nodeDownTranslate = 10;
	
	// For selection
	private final int selectionVisualWidth = 7; 
	private final int maxSelectionVisualWidth = 9;
	private final int minSelectionVisualWidth = 7;
	
	// These are for start and destination nodes.
	private final int maxNodeVisualWidth = 20;
	private final int minNodeVisualWidth = 12;
	
	// These are for intermediate path nodes.
	private final int pathNodeVisualWidth = 7;
	private final int maxPathNodeVisualWidth = 8;
	private final int minPathNodeVisualWidth = 7;
	
	/**
	 * This is the UI component in which this MapPanel is contained. 
	 */
	private MainFrame parent;
	
	/**
	 * The previous amount that the user had zoomed (updated in repaint()).
	 */
	private double prevZoomAount = 0.17075336384126763;

	/** 
	 * Used to zoom relative to the mouse pointer's location.
	 */
	private double xOffset = 6.685615736630126;
	
	/**
	 * Flag used for debugging. 
	 * Causes dots to be painted on valid nodes just at the beginning.
	 */
	private boolean displayValidNodes = false;
	
	/** 
	 * Used to zoom relative to the mouse pointer's location.
	 */	
	private double yOffset = 135.97473533228612;
	
	/**
	 * The current amount of zoom.
	 */
	private double currentZoomAmount = 0.17075336384126763;
	
	/**
	 * How fast zooming "zooms" in and out.
	 */
	private final double zoomSpeed = 1.1;
	
	/**
	 * The maximum amount in which the user can zoom in. This exists to prevent 
	 * the user from being able to zoom in indefinitely.
	 * 
	 * The dual to {@link maxZoomOut}.
	 */
	private double maxZoomIn = 5.0;
	
	/**
	 * The maximum amount in which the user can zoom out. This exists to prevent 
	 * the user from being able to zoom out indefinitely.
	 * 
	 * The dual to {@link maxZoomIn}.
	 */
	private double maxZoomOut = 0.1;
	
	/**
	 * The point at which the user initiated the click-and-drag.
	 */
	private Point mouseDragStart;
	
	/**
	 * The point at which the user initiated the click-and-drag for selection of nodes.
	 */
	private Point mouseSelectionStart;
	
	/**
	 * Amount of drag in X direction.
	 */
	private double translateX = -5;
	
	/**
	 * Amount of drag in Y direction.
	 */
	private double translateY = -9;
	
	private double drawImageX = 0;
	
	private double drawImageY = 0;
	
	private String displayImagePath = null;
	private String nodesImagePath = null;
	
	/**
	 * Indicates whether or not the user has clicked-and-dragged since the last draw.
	 */
	private boolean shouldTranslate = false;
	
	/**
	 * We update {@link xOffset} and {@link yOffset} when the user releases the mouse after transforming.
	 * This flag indicates whether or not we need to do that. Used in repaint().
	 */
	private boolean updateOffsetsOnTranslate = false;
	
	/**
	 * The bounding box of the image, which will be adjusted based on zoom translation.
	 */
	private Rectangle2D imageBounds = null;
	
	/**
	 * Grid of nodes.
	 */
	private Node[][] nodes = null;
	
	/**
	 * The LinkedList will contain the starting location which is processed first till destination
	 * is reached
	 */
	private LinkedList<Node> path = new LinkedList<Node>();
	
	/**
	 * Used for serialization of nodes. Stores all of the nodes individually, without edges.
	 * After nodes are loaded from saved file their edges are calculated and the nodes are then
	 * thrown into the above mentioned 2d array of nodes. This is only used on startup.
	 */
	private EdgelessNode[][] edgelessNodes = null;
	
	/**
	 * Number of nodes that are "walkable" 
	 */
	public int numberOfValidNodes = 0;
	
	private boolean serializationEnabled = false;
	
	/**
	 * This variable indicates the maximum amount we can decrease the alpha (i.e., transparency) of
	 * starting and destination nodes. We vary the transparency based on zoom. If we're zoomed out more,
	 * then the node will be displayed as more transparent. If we zoom in more, the node gets smaller and thus
	 * it doesn't need to be transparent, as it will be covering up less of the underlying image.
	 */
	private final int maxAlphaDecreaseForStartAndDest = 38; // 38 means we could set the alpha to 216.75 at the least, which is only 15% transparent.
	
	/**
	 * This variable indicates the maximum amount we can decrease the alpha (i.e., transparency) of
	 * nodes displayed within a path. We vary the transparency based on zoom. If we're zoomed out more,
	 * then the node will be displayed as more transparent. If we zoom in more, the node gets smaller and thus
	 * it doesn't need to be transparent, as it will be covering up less of the underlying image.
	 */	
	private final int maxAlphaDecreaseForPathNodes = 64; // 64 means we can set alpha to 191 at the least, which is 25% transparent.
	
	/**
	 * The shortest path between the start and the destination node is stored here once calculated.
	 */
	private ArrayList<Node> shortestPath = new ArrayList<Node>();
	
	private final String nodesSavePath = "src\\Res\\nodes.dat";
	
	// The image displayed to the user is a much higher resolution than the one
	// used to generate nodes for a few reasons. Essentially, we want to use a
	// high-resolution image to display to the user so it looks good. We do not,
	// however, need as many nodes as we'd get if we use the higher resolution.
	//
	// Since the two images are different sizes, however, we need to effectively
	// scale the coordinates of the smaller image to account for the size difference.
	//
	// I computed these values as follows:
	// scaleX = high_resolution_image.WIDTH / low_resolution_image.WIDTH
	// scaleY = high_resolution_image.HEIGHT / low_resolution_image.HEIGHT
	private final double scaleX = 0.4000415627597672485453034081463;
	private final double scaleY = 0.3998330550918196994991652754591;
	
	private boolean nextClickSetsStart = false;
	private boolean nextClickSetsDest = false;
	
	//used for holding the selection from click-and-drag
	private Selection clickAndDragSelection = new Selection();
	
	private boolean isDragging = false;//activated when left mouse button pressed, deactivated when released
	private boolean isSelecting = false;//activated when right mouse button pressed, deactivated when released
	
	private Node startingNode = null;
	private Node destNode = null;
	
	public MapPanel(MainFrame parent, String displayImagePath, String nodesImagePath) {
		this.parent = parent;
		this.displayImagePath = displayImagePath;
		this.nodesImagePath = nodesImagePath;
		this.mapImage = new ImageIcon(displayImagePath).getImage();
		
		//TODO: Remove/Replace if UI supports multiple saved paths
		try {
			this.shortestPath = parent.getProfile().getSavedPaths().get(0);
			System.out.println("Using previous saved path.");
		}catch(IndexOutOfBoundsException e) {
			System.out.println("No previous saved paths found...");
		}
		
		addMouseWheelListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		
		try {
			File nodesFile = new File(nodesSavePath);
			if (serializationEnabled && nodesFile.exists()) {
				System.out.println("Loading nodes from file...");
				long start = System.nanoTime();
		        FileInputStream fileInputStream = new FileInputStream(nodesSavePath);
		        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
		        this.edgelessNodes = (EdgelessNode[][]) objectInputStream.readObject();
		        
		        this.nodes = new Node[edgelessNodes.length][edgelessNodes[0].length];
		        int nodeID = 0;
		        for (int i = 0; i < this.edgelessNodes.length; i++) {
		        	for (int j = 0; j < this.edgelessNodes[i].length; j++) {
		        		
		        		boolean isValid = this.edgelessNodes[i][j].getValid();
		        		Terrain terrain = (isValid) ? Terrain.WALKABLE : Terrain.BLOCKED;
		        		
		        		Node n = new Node(nodeID, "Node " + nodeID++, isValid,
		        							terrain, i, j, null, null, null, null);
		        		nodes[i][j] = n;
		        		if (i > 0) {
		        			Node left = nodes[i - 1][j];
		        			left.setRightNode(n);
		        			
		        			n.setLeftNode(left);
		        		}
		        		if (j > 0) {
		        			Node above = nodes[i][j-1];
		        			above.setBottomNode(n);
		        			
		        			n.setTopNode(above);
		        		}
		        	}
		        }
		        
		        long done = System.nanoTime();
		        long elapsed = done - start;
		        long elapsedSeconds = elapsed / 1000000000;
		        objectInputStream.close();
		        edgelessNodes = null;
		        System.out.println("Loading nodes from file done. Time elapsed: " + elapsedSeconds + " seconds");
			}
			else {
				// File not available. Instead, generate using an image.
				long startFromPicture = System.nanoTime();
				loadNodes();
				long doneFromPicture = System.nanoTime();
				double elapsedSecondsFromPicture = (doneFromPicture - startFromPicture) / 1000000000.0;
				System.out.println("Generating node grid from image done. Took " + elapsedSecondsFromPicture + " seconds.");
				
				if (serializationEnabled) {
					System.out.println("Serializing the nodes array...");
					
					// Serialize them so they're available next time.
					long startSerializing = System.nanoTime();
			        FileOutputStream fileOutputStream = new FileOutputStream(nodesSavePath);
			        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
			        objectOutputStream.writeObject(edgelessNodes);
			        long doneSerializing = System.nanoTime();
			        double elapsedSerializing = (doneSerializing - startSerializing) / 1000000000.0;
			        System.out.println("Done serializing. Took " + elapsedSerializing + " seconds.");
			        
			        objectOutputStream.close();					
				}
		        edgelessNodes = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//TODO: Edit nodes as they are created up above (instead of doing this)
		ArrayList<HashSet<Node>> premadeUserSelections = parent.getProfile().getSavedSelections();
		
		if(premadeUserSelections != null) {
		
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! Loading Persisted Selection");
			
			for(HashSet<Node> selectionSet : premadeUserSelections) {
				
				System.out.println("Selection found!");
				System.out.println("Selection contains " + selectionSet.size() + " Nodes.");
				
				for(Node node : selectionSet) {
				
					System.out.println("Adding node from selection...");
					
					clickAndDragSelection.addNode(node);
					nodes[node.getX()][node.getY()].setTerrain(node.getTerrain());//set actual map nodes to be of terrain from saved nodes
				
				}
			}
			
		}
		
		
		
	}
	
	/**
	 * Load nodes based on RGB of image. Eventually we'll just serialize and deserialize them directly.
	 */
	private void loadNodes() throws IOException {
		System.out.println("Generating grid of nodes using image...");
		BufferedImage bufferedMapImage = ImageIO.read(new File(this.nodesImagePath));
		//BufferedImage bufferedMapImage = ImageIO.read(MapPanel.class.getResource("C:\\Users\\Benjamin\\Documents\\School\\Fall 2019\\CS 321\\CS321\\CS321CourseProject\\src\\Res\\CampusMapForNodes.png"));
		byte[] pixels = ((DataBufferByte)bufferedMapImage.getRaster().getDataBuffer()).getData();
		bufferedMapImage.getType();
		
		final int width = bufferedMapImage.getWidth();
        final int height = bufferedMapImage.getHeight();
	    
        int nodeID = 0;	
	    // https://stackoverflow.com/questions/6524196/java-get-pixel-array-from-image
	    // boolean[][] rgbArray = new boolean[height][width];
	    Node[][] nodes = new Node[height][width];
	    edgelessNodes = new EdgelessNode[height][width];
	    final int pixelLength = 3;
	    for (int pixel = 0, row = 0, col = 0; pixel + 2 < pixels.length; pixel += pixelLength) {
    		int argbValue = 0;
    		// The values are stored in the format ARGB (alpha, red, green, blue), so
    		// that's why we compute the offsets the way we do. We also have to mask
    		// the values to extract the specific color value correctly.
    		argbValue += -16777216; // 255 alpha
    		argbValue += ((int) pixels[pixel] & 0xff); // blue
            argbValue += (((int) pixels[pixel + 1] & 0xff) << 8); // green
            argbValue += (((int) pixels[pixel + 2] & 0xff) << 16); // red
            boolean valid = (argbValue >= -8777216);
            if (valid)
            	numberOfValidNodes = numberOfValidNodes + 1;
            
			Node nextNode = new Node(nodeID, "Node " + nodeID++, valid, ((valid)?Terrain.WALKABLE:Terrain.BLOCKED), row, col, 
					null, null, null, null);
			
			nodes[row][col] = nextNode;
			
			edgelessNodes[row][col] = new EdgelessNode(row, col, valid);
			
			if (col > 0) {
				Node left = nodes[row][col - 1];
				left.setRightNode(nextNode);
				
				nextNode.setLeftNode(left);
			}
			if (row > 0) {
				Node above = nodes[row - 1][col];
				above.setBottomNode(nextNode);
				
				nextNode.setTopNode(above);
			}
			col++;
			// If we've reached the end of a column, reset the column index to zero and increment row.
			if (col == width)
			{
				if (row % 250 == 0) 
					System.out.println("Finished row " + row + "...");
				col = 0;
				row++;
			}
    	}
    	
    	this.nodes = nodes;
	}
	
	/**
	 * Clamp the zoom between {@link maxZoomIn} and {@link maxZoomOut} to ensure 
	 * the user cannot zoom out or zoom in indefinitely. 
	 */
	private void clampZoom() {
		if (currentZoomAmount > maxZoomIn)
			currentZoomAmount = maxZoomIn;
		else if (currentZoomAmount < maxZoomOut)
			currentZoomAmount = maxZoomOut;
	}
	
	public int getNumberNodes(boolean validOnly) {
		if (!validOnly) {
			return nodes.length * nodes[0].length;
		}
		else {
			return numberOfValidNodes;
		}
	}
	
	/**
	 * Handles mouse scroll events triggered by the user.
	 */
	@Override
    protected void processMouseWheelEvent(MouseWheelEvent eventArgs) {
		// Determine if the user was attempting to zoom in or zoom out based on whether
		// or not the mouse wheel was rotated up or down.
        if (eventArgs.getWheelRotation() < 0) {
        	// Zoom in, then make sure user is within the configured "zoom bounds" by calling clampZoom
        	currentZoomAmount *= zoomSpeed;
            clampZoom();
            repaint();
        }
        if (eventArgs.getWheelRotation() > 0) {
        	// Zoom out, then make sure user is within the configured "zoom bounds" by calling clampZoom
        	currentZoomAmount /= zoomSpeed;
            clampZoom();
        	repaint();
        }
        super.processMouseWheelEvent(eventArgs);
    }
	
	/**
	 * This is related to the UI. We must specify a "preferred size" for this element in order for a parental "scroll pane" to work properly.
	 */
	@Override
    public Dimension getPreferredSize() {
		if (mapImage == null) {
			return parent.getSize();
		}
        return new Dimension(mapImage.getWidth(null), mapImage.getHeight(null));
    }
	
	/**
	 * Draw the map (and possibly other things).
	 */
	@Override
	public void paintComponent(Graphics g) {
	    super.paintComponent(g);

        // Get the location of the mouse pointer relative to this UI element. We get the mouse pointer's location
        // as well as the location of "this" UI element (the MapPanel component) to calculate the relative x. 
        double relativeX = MouseInfo.getPointerInfo().getLocation().getX() - getLocationOnScreen().getX();
        double relativeY = MouseInfo.getPointerInfo().getLocation().getY() - getLocationOnScreen().getY();

        // The amount the zoom has changed relative to the prev. zoom. 
        double zoomRatio = currentZoomAmount / prevZoomAount;

        // We translate the image to allow us to "zoom in/out" relative to the mouse pointer's location.
        // We must first calculate how much to translate the image based on the zoom ratio.
        xOffset = (zoomRatio * xOffset) + (1 - zoomRatio) * relativeX;
        yOffset = (zoomRatio * yOffset) + (1 - zoomRatio) * relativeY;

        drawImageX = xOffset;
        drawImageY = yOffset;
        
        // Apply the translation and then the scaling (zoom). Update prev. zoom amount value.
        if (shouldTranslate) {
        	drawImageX += translateX;
        	drawImageY += translateY;
        	
        	// Update these values when button isn't pressed. If they're always updated,
        	// then you'll be stuck moving in one direction and it'll speed up as you drag. Very weird.
        	if (updateOffsetsOnTranslate) {
        		xOffset += translateX;
        		yOffset += translateY;
            	shouldTranslate = false;
        	}
        }
        prevZoomAount = currentZoomAmount;
        
        //System.out.println("\ndrawImageX: " + (int)drawImageX);
        //System.out.println("drawImageY: " + (int)drawImageY);
        
        // Update the current image bounds based on the transform. Useful for determining if used click image and where. 
        imageBounds = new Rectangle((int)drawImageX, (int)drawImageY, (int)(mapImage.getWidth(null) * currentZoomAmount), (int)(mapImage.getHeight(null) * currentZoomAmount));
        
        // Draw the image on the screen with transformation applied.
	    g.drawImage(mapImage,  (int)drawImageX,  (int)drawImageY, (int)(mapImage.getWidth(null) * currentZoomAmount), (int)(mapImage.getHeight(null) * currentZoomAmount), null);
        
	    int ovalWidth = (int)(this.nodeVisualIndicationWidth * (1 / currentZoomAmount));
	    
	    double amountZoomedAsPercent = this.currentZoomAmount / this.maxZoomIn;
	    System.out.println("amountZoomedAsPercent = " + amountZoomedAsPercent);
	    int pathIncrement = 1;
	    
	    if (amountZoomedAsPercent <= 0.25)
	    	pathIncrement = 5;
	    if (amountZoomedAsPercent > 0.25 && amountZoomedAsPercent <= 0.5) 
	    	pathIncrement = 4;
	    if (amountZoomedAsPercent > 0.5 && amountZoomedAsPercent <= 0.9) 
	    	pathIncrement = 3;
	    if (amountZoomedAsPercent > 0.9)
	    	pathIncrement = 1;
	    
	    int startDestTransparency = 255 - (int)(this.maxAlphaDecreaseForStartAndDest * amountZoomedAsPercent);
	    int pathTransparency = 255 - (int)(this.maxAlphaDecreaseForPathNodes * amountZoomedAsPercent);
	    
	    // Clamp the value of ovalWidth between the pre-defined constraints.
	    if (ovalWidth > maxNodeVisualWidth)
	    	ovalWidth = maxNodeVisualWidth;
	    else if (ovalWidth < minNodeVisualWidth)
	    	ovalWidth = minNodeVisualWidth;
	    
	    int ovalRadius = ovalWidth / 2;
        if (this.startingNode != null) {
        	Color transparentGreen = new Color(10, 199, 41, startDestTransparency); 
        	g.setColor(transparentGreen);
        	//System.out.println("\n-=-=-=-=-=-= STARTING NODE =-=-=-=-=-=-");
        	Point topLeft = nodeToImageCoordinates(this.startingNode.getPointFlipped());
        	
        	// Adjust so the oval is centered where the user clicks (instead of the top-left of the oval
        	// being where the user clicked).
    		Point center = new Point(topLeft.x - ovalRadius, topLeft.y - ovalRadius); 
    		
        	//System.out.println("Drawing oval for starting node at " + center.toString() + " with width " + ovalWidth);
        	g.fillOval((int)(center.x), (int)(center.y), ovalWidth, ovalWidth);
        }
        
        if (this.destNode != null) {
        	Color transparentRed = new Color(199, 10, 10, startDestTransparency); 
        	g.setColor(transparentRed);
        	//System.out.println("\n-=-=-=-=-=-= DESTINATION NODE =-=-=-=-=-=-");
        	Point topLeft = nodeToImageCoordinates(this.destNode.getPointFlipped());
        	
        	int downShift = (int)(nodeDownTranslate * (1 / this.currentZoomAmount));
        	
        	// Adjust so the oval is centered where the user clicks (instead of the top-left of the oval
        	// being where the user clicked).
    		Point center = new Point(topLeft.x - ovalRadius, topLeft.y - ovalRadius);         	
        	//System.out.println("Drawing oval for destination node at " + center.toString() + " with width " + ovalWidth);

        	g.fillOval((int)(center.x), (int)(center.y), ovalWidth, ovalWidth);
        }
        
        if (shortestPath.size() > 0) {
    	    int pathOvalWidth = (int)(this.pathNodeVisualWidth * (1 / currentZoomAmount));
    	    // Clamp the value of ovalWidth between the pre-defined constraints.
    	    if (pathOvalWidth > maxPathNodeVisualWidth)
    	    	pathOvalWidth = maxPathNodeVisualWidth;
    	    else if (pathOvalWidth < minPathNodeVisualWidth)
    	    	pathOvalWidth = minPathNodeVisualWidth;
    	    
    	    int pathOvalRadius = pathOvalWidth / 2;        
            
        	Color transparentBlue = new Color(66, 87, 245, pathTransparency);
        	g.setColor(transparentBlue);        
        	
        	// We don't downshift the first 5% of the path nodes or the last 5%.
        	// We do not downshift start/ending nodes, so by not down shifting we sort of connect the two.
        	int lower1 = (int)(shortestPath.size() * 0.02);
        	int upper1 = (int)(shortestPath.size() * 0.98);
        	
        	int lower2 = (int)(shortestPath.size() * 0.4);
        	int upper2 = (int)(shortestPath.size() * 0.96);
        	
        	// Iterate over all of the nodes in the shortest path and display them.
        	// We increment i by a value dependent on how zoomed-in we are. If we're
        	// very zoomed-in, then we'd like to display more of the nodes in the path.
        	// If we're zoomed out, it isn't so important to display each and every node.
        	for (int i = 0; i < shortestPath.size(); i += pathIncrement) {
        		Node cur = shortestPath.get(i);
        		
        		Point topLeft = null;
        		
        		// We vary the downshift amount depending on where in the past the nodes are.
        		// Nodes near the very start or very end are downshifted LESS than nodes in the middle of the path.
        		if (i <= lower1) {
        			topLeft = nodeToImageCoordinates(cur.getPointFlipped(), false);
        		}	
        		else if (i > lower1 && i <= lower2) {
        			topLeft = nodeToImageCoordinates(cur.getPointFlipped(), true, 5);
        		}
        		else if (i > lower2 && i <= upper2) {
        			topLeft = nodeToImageCoordinates(cur.getPointFlipped(), true, 10);
        		}
        		else if (i > upper2 && i <= upper1) {
        			topLeft = nodeToImageCoordinates(cur.getPointFlipped(), true, 5);
        		}
        		else {
        			topLeft = nodeToImageCoordinates(cur.getPointFlipped(), false);
        		}
        		
    			Point center = new Point(topLeft.x - pathOvalRadius, topLeft.y - pathOvalRadius); 
        		
        		g.fillOval((int)(center.x), (int)(center.y), pathOvalWidth, pathOvalWidth);
        	}        	
        }
        
        //draw the selection if there is one
        if(!clickAndDragSelection.isEmpty()) {
        	
        	System.out.println("Selection not Empty");
        	
        	HashSet<Node> selectedNodes = clickAndDragSelection.getNodes();
        
        	int selectionWidth = (int)(this.selectionVisualWidth * (1 / currentZoomAmount));
    	    // Clamp the value of rectWidth between the pre-defined constraints.
    	    if (selectionWidth > this.maxSelectionVisualWidth)
    	    	selectionWidth = maxSelectionVisualWidth;
    	    else if (selectionWidth < this.minSelectionVisualWidth)
    	    	selectionWidth = minSelectionVisualWidth;
        	
        	//set color of selected nodes
    		Color transparentGreen = new Color(66, 245, 87, pathTransparency);
    		g.setColor(transparentGreen);
        	
    		// draw every other
    		int draw = 0;
    		//draw over only valid selected nodes
        	for(Node node : selectedNodes) {
        		/*if (draw++ != 0) {
        			if (draw >= 5)
        			{
        				draw = 0;
        			}
        			continue;
        		}
        		draw++;*/
        		// We pass '5' as the downshift instead of using the default '10' as '5' just looks better for this, based on trial and error.
        		Point center = nodeToImageCoordinates(node.getPointFlipped(), true, 5); 
        		//int downShift = (int)(nodeDownTranslate * (1 / this.currentZoomAmount));
        		g.fillOval(center.x, center.y, selectionWidth, selectionWidth);
        	}
    	}
        
	}
	
	
	
	private Rectangle getImageBounds() {
		int w = mapImage.getWidth(null);
		int h = mapImage.getHeight(null);
		Rectangle rect = new Rectangle(0, 0, w, h);
		
		rect.translate((int)(xOffset + translateX), (int)(yOffset + translateY));
		
		int _w = (int)(currentZoomAmount * w);
		int _h = (int)(currentZoomAmount * h);
		
		rect.grow(_w, _h);
		
		return rect;
	}
	
	/**
	 * Print the values of the various variables.
	 */
	private void printDebugInfo() {
        System.out.println("CurrentZoomAmount: " + currentZoomAmount);
        System.out.println("PrevZoomAmount: " + prevZoomAount);
        System.out.println("Offset X: " + xOffset);
        System.out.println("Offset Y: " + yOffset);
        System.out.println("Translate X: " + translateX);
        System.out.println("Translate Y: " + translateY);		
	}
	
	public void clearSelectedNodes() {
		this.startingNode = null;
		this.destNode = null;
		shortestPath.clear();
		
		repaint();
	}

	public void generatePaths() {
		
		if (this.startingNode == null && this.destNode == null) 
			JOptionPane.showMessageDialog(null, "Please specify both a starting node and a destination node first.", "Error", JOptionPane.WARNING_MESSAGE);
		else if (this.startingNode == null) 
			JOptionPane.showMessageDialog(null, "Please specify a starting node.", "Error", JOptionPane.WARNING_MESSAGE);
		else if (this.destNode == null)
			JOptionPane.showMessageDialog(null, "Please specify a destination node.", "Error", JOptionPane.WARNING_MESSAGE);
		
		
		LinkedList<Node> path = new LinkedList<Node>();
		path.add(startingNode);
		path.add(destNode);
		ShortestPathAlgorithm spa = new ShortestPathAlgorithm(path, nodes);
		HashMap<Node, Node> discovered = new HashMap<Node, Node>();
		int result = spa.calculateShortestPath(discovered);
		
		if (result == -1) {
			JOptionPane.showMessageDialog(null, "ERROR - No path could be generated. The two points are not connected...", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		System.out.println("The distance between the two nodes is " + result + " nodes.");
		
		LinkedList<Node> actualPath = new LinkedList<Node>();
		Node n = destNode;
		actualPath.add(n);
		while (!startingNode.equals(n)) {
			n = discovered.get(n);
			actualPath.add(n);
		}
		// Remove the first and last elements, which will be the destination and the start, respectively.
		// actualPath.removeFirst();
		// actualPath.removeLast();
		this.shortestPath.clear();
		this.shortestPath.addAll(actualPath);
		repaint();
	}
	
	public void drawStartingLoc(Graphics g)
	{
		double amountZoomedAsPercent = this.currentZoomAmount / this.maxZoomIn;
	    System.out.println("amountZoomedAsPercent = " + amountZoomedAsPercent);
	    int pathIncrement = 1;
	    
	    if (amountZoomedAsPercent <= 0.25)
	    	pathIncrement = 5;
	    if (amountZoomedAsPercent > 0.25 && amountZoomedAsPercent <= 0.5) 
	    	pathIncrement = 4;
	    if (amountZoomedAsPercent > 0.5 && amountZoomedAsPercent <= 0.9) 
	    	pathIncrement = 3;
	    if (amountZoomedAsPercent > 0.9)
	    	pathIncrement = 1;
	    
	    int startDestTransparency = 255 - (int)(this.maxAlphaDecreaseForStartAndDest * amountZoomedAsPercent);
		int ovalWidth = (int)(this.nodeVisualIndicationWidth * (1 / currentZoomAmount));
		 // Clamp the value of ovalWidth between the pre-defined constraints.
	    if (ovalWidth > maxNodeVisualWidth)
	    	ovalWidth = maxNodeVisualWidth;
	    else if (ovalWidth < minNodeVisualWidth)
	    	ovalWidth = minNodeVisualWidth;
	    
	    int ovalRadius = ovalWidth / 2;
        if (this.startingNode != null) {
        	Color transparentGreen = new Color(10, 199, 41, startDestTransparency); 
        	g.setColor(transparentGreen);
        	//System.out.println("\n-=-=-=-=-=-= STARTING NODE =-=-=-=-=-=-");
        	Point topLeft = nodeToImageCoordinates(this.startingNode.getPointFlipped());
        	
        	// Adjust so the oval is centered where the user clicks (instead of the top-left of the oval
        	// being where the user clicked).
    		Point center = new Point(topLeft.x - ovalRadius, topLeft.y - ovalRadius); 
        	//System.out.println("Drawing oval for starting node at " + center.toString() + " with width " + ovalWidth);
        	g.fillOval((int)(center.x), (int)(center.y), ovalWidth, ovalWidth);
        }
        System.out.println("paint start");
	}
	
	public void setStartingNode(int x, int y)
	{
		/*int rowLength = nodes[x].length;
		int colLength = nodes.length;
		System.out.println("row: "  + rowLength);
		System.out.println("col: "  + colLength);*/
		
		startingNode = nodes[x][y];
		System.out.println("starting node set");
		// If we click buildings multiple times in a row without doing anything that triggers a repaint(), then we can 
		// end up painting multiple red/green dots using this method. Repaint() ensures the old circles don't get redrawn after 
		// we update the value of the starting/destination node.		
		repaint();
		//drawStartingLoc(getGraphics());
	}
	
	public void drawDestLoc(Graphics g)
	{
		double amountZoomedAsPercent = this.currentZoomAmount / this.maxZoomIn;
	    System.out.println("amountZoomedAsPercent = " + amountZoomedAsPercent);
	    int pathIncrement = 1;
	    
	    if (amountZoomedAsPercent <= 0.25)
	    	pathIncrement = 5;
	    if (amountZoomedAsPercent > 0.25 && amountZoomedAsPercent <= 0.5) 
	    	pathIncrement = 4;
	    if (amountZoomedAsPercent > 0.5 && amountZoomedAsPercent <= 0.9) 
	    	pathIncrement = 3;
	    if (amountZoomedAsPercent > 0.9)
	    	pathIncrement = 1;
	    
	    int startDestTransparency = 255 - (int)(this.maxAlphaDecreaseForStartAndDest * amountZoomedAsPercent);
		int ovalWidth = (int)(this.nodeVisualIndicationWidth * (1 / currentZoomAmount));
		 // Clamp the value of ovalWidth between the pre-defined constraints.
	    if (ovalWidth > maxNodeVisualWidth)
	    	ovalWidth = maxNodeVisualWidth;
	    else if (ovalWidth < minNodeVisualWidth)
	    	ovalWidth = minNodeVisualWidth;
	    
	    int ovalRadius = ovalWidth / 2;
		if (this.destNode != null) {
        	Color transparentRed = new Color(199, 10, 10, startDestTransparency); 
        	g.setColor(transparentRed);
        	//System.out.println("\n-=-=-=-=-=-= DESTINATION NODE =-=-=-=-=-=-");
        	Point topLeft = nodeToImageCoordinates(this.destNode.getPointFlipped());
        	
        	// Adjust so the oval is centered where the user clicks (instead of the top-left of the oval
        	// being where the user clicked).
    		Point center = new Point(topLeft.x - ovalRadius, topLeft.y - ovalRadius);         	
        	//System.out.println("Drawing oval for destination node at " + center.toString() + " with width " + ovalWidth);
        	g.fillOval((int)(center.x), (int)(center.y), ovalWidth, ovalWidth);
        }
	}	
	
	public void setDestinationNode(int x, int y) 
	{
		destNode = nodes[x][y];
		repaint();
		// If we click buildings multiple times in a row without doing anything that triggers a repaint(), then we can 
		// end up painting multiple red/green dots using this method. Repaint() ensures the old circles don't get redrawn after 
		// we update the value of the starting/destination node.
		//drawDestLoc(getGraphics());
	}
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent eventArgs) {
		// TODO Auto-generated method stub
		
	}

	public void mouseDragged(MouseEvent eventArgs) {
		
		if(isDragging) {//LEFT MOUSE BUTTON IS HELD (MOVE CAMERA)
			
			// Get the current point and determine how much the user has moved since clicking.
			Point currentPoint = eventArgs.getLocationOnScreen();
			
			translateX = currentPoint.x - mouseDragStart.x;
			translateY = currentPoint.y - mouseDragStart.y;
			
			shouldTranslate = true;
		}
		else if (isSelecting) {//RIGHT MOUSE BUTTON IS HELD (CREATE SELECTION)	
			System.out.println("Selecting...");
			
			Point startNodeCoords = panelToNodeCoordinates(mouseSelectionStart);//beginning node point of selection
			
			Point endNodeCoords = eventArgs.getPoint();//end map point of selection
			endNodeCoords = panelToNodeCoordinates(endNodeCoords);//end node point of selection
			
			//(potentially) swap around x and y points so that we can always assume the user started their
			//selection in the upper left corner of a square and ended it in the bottom left corner
			//as opposed to any of the other 3 possible combinations of click-and-drag selection
			int startX = (startNodeCoords.x <= endNodeCoords.x) ? startNodeCoords.x : endNodeCoords.x;
			int endX = (endNodeCoords.x > startNodeCoords.x) ? endNodeCoords.x : startNodeCoords.x;
			
			int startY = (startNodeCoords.y <= endNodeCoords.y) ? startNodeCoords.y : endNodeCoords.y;
			int endY = (endNodeCoords.y > startNodeCoords.y) ? endNodeCoords.y : startNodeCoords.y; 
			
			//loop through all nodes in the selected square and add to Selection
			
			System.out.println("("+startX+","+startY+")");
			System.out.println("("+endX+","+endY+")");
			
			for(int x = startX; x < endX; x++) {
				for(int y = startY; y < endY; y++) {
					
					Node currNode = nodes[y][x];//the current node at (x, y)
					
					if(currNode.isValid()) {//if node is valid, add to Selection
						
						clickAndDragSelection.addNode(nodes[y][x]);
						
					}
					
				}
			}
		
		}
		
		repaint();
		
	}

	public void mouseMoved(MouseEvent eventArgs) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Adjust a given point's coordinates so that they can be used as indices to the 2D node array.
	 * @param p Point whose coordinates are relative to the underlying image.
	 * @return point whose position on the screen is the same as the given point but whose coordinates are 
	 * 		   relative to the Nodes[][] nodes object.
	 */
	public Point panelToNodeCoordinates(Point p) {
		double ratioX = mapImage.getWidth(null) / imageBounds.getWidth();
		ratioX *= scaleX;
		
		double ratioY = mapImage.getHeight(null) / imageBounds.getHeight();
		ratioY *= scaleY;
		
		Point adjusted = new Point((int)(ratioX * (p.getX() - imageBounds.getX())), 
										   (int)(ratioY * (p.getY() - imageBounds.getY())));	
		return adjusted;	
	}
	
	public Point nodeToImageCoordinates(Point p) {
		return this.nodeToImageCoordinates(p, false);
	}
	
	public Point nodeToImageCoordinates(Point p, boolean applyDownshift) {
		return this.nodeToImageCoordinates(p, applyDownshift, 10);
	}
	
	/**
	 * Given the indices of a node in the 2D Node[][] nodes array, return the corresponding image coordinates.
	 * 
	 * If 'applyDownshift' is true, then we shift the point down slightly. This is to account for the fact that, when we scale the grid of nodes across the higher-resolution
	 * display image, it doesn't scale perfectly. The nodes are essentially a little higher than they're supposed to be. So when we draw them, we want to paint them lower.
	 * We only really do this for path nodes though, not for starting/destination nodes. We want starting and destination nodes to appear exactly where the user clicks, and
	 * down-shifting them would mess up this behavior. 
	 * @return
	 */
	public Point nodeToImageCoordinates(Point p, boolean applyDownshift, int downShift) {
		double ratioX = imageBounds.getWidth() / mapImage.getWidth(null);
		//System.out.println("\nimageBounds.getWidth() / mapImage.getWidth(null) = " + imageBounds.getWidth() + "/" + mapImage.getWidth(null) + " = " + ratioX);
		double ratioY = imageBounds.getHeight() / mapImage.getHeight(null);
		//System.out.println("imageBounds.getHeight() / mapImage.getHeight(null) = " + imageBounds.getHeight() + "/" + mapImage.getHeight(null) + " = " + ratioY);
		
		//System.out.println("Node Point: " + p.toString());
		//System.out.println("ImageBounds: " + imageBounds.toString() + "\n");
		int x = (int)(p.x / scaleX);
		//System.out.println("p.x / scaleX = " + x);
		x = (int)(x * ratioX);
		//System.out.println("x += this.drawImageX --> " + x + " += " + this.drawImageX + " --> " + ((int)(x + drawImageX)));
		x += this.drawImageX;
		int y = (int)(p.y / scaleY);
		//System.out.println("p.y / scaleY = " + y);
		y = (int)(y * ratioY);
		//System.out.println("y += this.drawImageY --> " + y + " += " + this.drawImageY + " --> " + ((int)(y + drawImageY)));
		y += this.drawImageY;
		
		if (applyDownshift) {
			downShift = (int)(downShift / scaleX);
			downShift = (int)(downShift * ratioX);
			y += downShift;
		}
		
		return new Point(x,y);
	}

	public void mouseClicked(MouseEvent eventArgs) {
		
		//Get the point that was clicked and output the validity of the node at that point
		Point clicked = eventArgs.getPoint();
		Clicked c = null;
		if (imageBounds.contains(clicked)) {
			double ratioX = mapImage.getWidth(null) / imageBounds.getWidth();
			ratioX *= scaleX;
			double ratioY = mapImage.getHeight(null) / imageBounds.getHeight();
			ratioY *= scaleY;
			Point adjustedForImage = new Point((int)(ratioX * (clicked.getX() - imageBounds.getX())), 
											   (int)(ratioY * (clicked.getY() - imageBounds.getY())));
			Node clickedNode = nodes[adjustedForImage.y][adjustedForImage.x];
			
			if (clickedNode.isValid()) {
				path.add(clickedNode);
				System.out.println("Clicked VALID node at image coordinates (" + adjustedForImage.getX() + "," + adjustedForImage.getY() + ")" + ". (Image clicked!)");
				c = new Clicked(clicked, true);
				
				if (this.nextClickSetsStart) 
				{
					System.out.println("Starting node set to node at (" + adjustedForImage.getX() + "," + adjustedForImage.getY() + ")");
					this.startingNode = clickedNode;
					this.shortestPath.clear();
					this.nextClickSetsStart = false;
				}
				else if (this.nextClickSetsDest) {
					System.out.println("Destination node set to node at (" + adjustedForImage.getX() + "," + adjustedForImage.getY() + ")");
					this.destNode = clickedNode;
					this.shortestPath.clear();
					this.nextClickSetsDest = false;					
				}
			}			
			else {
				System.out.println("Clicked INVALID node at node coordinates (" + adjustedForImage.getX() + "," + adjustedForImage.getY() + ")" + ". (Image clicked!)");
				c = new Clicked(clicked, false);
			}
			System.out.println("Panel coordinates = (" + clicked.getX() + "," + clicked.getY() + ")");
		} else {
			System.out.println("Click at " + clicked + ". (Image NOT clicked!)");
		}
		
		System.out.println("eventArgs.getLocationOnScreen() = " + eventArgs.getLocationOnScreen());
		
		repaint(); // Drag ended so repaint.
		// System.out.println("Current Image Bounds: " + imageBounds);
	}

	public void mouseEntered(MouseEvent eventArgs) {
		// TODO Auto-generated method stub
		
	}

	public void mouseExited(MouseEvent eventArgs) {
		// TODO Auto-generated method stub
		
	}

	public void mousePressed(MouseEvent eventArgs) {
		// Pressed just means the button was pressed down, not necessarily released.
		// Clicked means the user pressed and released the button.
		
		updateOffsetsOnTranslate = false; // User clicked so this should be false.
		
		if(eventArgs.getButton() == MouseEvent.BUTTON1) {//left mouse button, allow moving of camera
			// getPoint() is relative to the source component.
			mouseDragStart = MouseInfo.getPointerInfo().getLocation();
			isDragging = true;
		}
		if(eventArgs.getButton() == MouseEvent.BUTTON3) {//right mouse button, allow creation of Selection
			mouseSelectionStart = eventArgs.getPoint();
			isSelecting = true;
		}
	}

	public void mouseReleased(MouseEvent eventArgs) {
		
		if(eventArgs.getButton() == MouseEvent.BUTTON1) {//LEFT MOUSE BUTTON RELEASED (DRAG CAMERA)
			
			updateOffsetsOnTranslate = true;	// That also means this should now be set to true.
			isDragging = false;
			
		}
		
		if(eventArgs.getButton() == MouseEvent.BUTTON3) {//RIGHT MOUSE BUTTON RELEASED (MAKE SELECTION)
			
			isSelecting = false;
			
		}
		
		repaint();
		
	}
	
	public void setNextClickStart(boolean newValue) {
		this.nextClickSetsStart = newValue;
		
		if (newValue == true) {
			nextClickSetsDest = false;
		}
	}
	
	public void setNextClickDest(boolean newValue) {
		this.nextClickSetsDest = newValue;
		
		if (newValue == true) {
			nextClickSetsStart = false;
		}
	}
	
	public void takeScreenshot() {//https://coderanch.com/t/470601/java/screenshot-JPanel
		BufferedImage bufImage = new BufferedImage(this.getSize().width, this.getSize().height,BufferedImage.TYPE_INT_RGB);
		this.paint(bufImage.createGraphics());
		File imageFile = new File("."+File.separator+"src\\Res\\User Screenshots\\snapshot.jpeg");
	    try{
	        imageFile.createNewFile();
	        ImageIO.write(bufImage, "jpeg", imageFile);
	        System.out.println("Took a Screenshot!");
	    }catch(Exception ex){
	    	ex.printStackTrace();
	    }
	}
	
	public void clearPathNodes() {
		this.startingNode = null;
		this.destNode = null;
		shortestPath.clear();
		
		//TODO: remove this when no longer needed
		ArrayList<ArrayList<Node>> savedPaths = parent.getProfile().getSavedPaths(); 
		if (savedPaths.size() > 0)
			savedPaths.remove(0);
		parent.getProfile().saveProfile();
		
		repaint();
	}
	
	public void savePath() {
		
		parent.getProfile().storePath(shortestPath);
		parent.getProfile().saveProfile();
		
	}
	
	public void saveSelection() {
		
		//set selection to Blocked
		clickAndDragSelection.setNodesTerrain(Terrain.BLOCKED);
		
		
		HashSet<Node> selectedNodes = clickAndDragSelection.getNodes();
		for(Node node : selectedNodes)
			nodes[node.getX()][node.getY()].setTerrain(node.getTerrain());//update the live map (so the BFS takes selection into account)
		
		//save the terrain change on the selection to persist
		parent.getProfile().storeSelection(clickAndDragSelection.getNodes());
		parent.getProfile().saveProfile();
		
	}
	
	public Selection getSelection() {
		return clickAndDragSelection;
	}
	
	public void clearSelection() {
		
		//set selection to Walkable
		clickAndDragSelection.setNodesTerrain(Terrain.WALKABLE);
		
		HashSet<Node> selectedNodes = clickAndDragSelection.getNodes();
		for(Node node : selectedNodes)
			nodes[node.getX()][node.getY()].setTerrain(node.getTerrain());//update the live map to revert selection changes
		
		//get rid of the selection
		clickAndDragSelection.clear();
		
		//remove all of the terrain edits from the selection
		parent.getProfile().storeSelection(clickAndDragSelection.getNodes());
		parent.getProfile().saveProfile();
		
		repaint();
	}
	
	public void setStartingNode(Node n) {
		this.startingNode = n;
	}
	
	public void setDestinationNode(Node n) {
		this.destNode = n;
	}
	
	public Node getStartingNode() {
		return this.startingNode;
	}
	
	public Node getDestinationNode() {
		return this.destNode;
	}
	
	public ArrayList<Node> getShortestPath() {
		return this.shortestPath;
	}
	
	public Node[][] getGraph()
	{
		return this.nodes;
		
	}
	
	public LinkedList<Node> getPath()
	{
		return this.path;
	}
	
	class Clicked {
		Point location;
		boolean valid;

		public Clicked(Point p, boolean b) {
			this.location = p;
			this.valid = b;
		}
	}
}
