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
import java.util.LinkedList;

import javax.imageio.ImageIO;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

import Map.EdgelessNode;
import Map.Node;

public class MapPanel extends JPanel implements MouseWheelListener, MouseListener, MouseMotionListener  {

	// NOTE: The default values are what positions the image in the middle of the window when first starting the application.
	// I just printed out the values every repaint(), positioned the image that way, and then used the values that were 
	// printed once I had positioned the image the way I liked it.
	
	/**
	 * This is the image of the map. 
	 */
	private final Image mapImage;
	
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
	 * Amount of drag in X direction.
	 */
	private double translateX = -5;
	
	/**
	 * Amount of drag in Y direction.
	 */
	private double translateY = -9;
	
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
	
	private EdgelessNode[][] edgelessNodes = null;
	
	/**
	 * Number of nodes that are "walkable" 
	 */
	public int numberOfValidNodes = 0;
	
	private boolean serializationEnabled = false;
	
	/**
	 * Used to draw circles at last-clicked nodes.
	 */
	private LinkedList<Clicked> lastTenClicked = new LinkedList<Clicked>();
	
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
	
	private Node startingNode = null;
	private Node destNode = null;
	
	public MapPanel(MainFrame parent, String displayImagePath, String nodesImagePath) {
		this.parent = parent;
		this.displayImagePath = displayImagePath;
		this.nodesImagePath = nodesImagePath;
		this.mapImage = new ImageIcon(displayImagePath).getImage();
		
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
		        		Node n = new Node(nodeID, "Node " + nodeID++, this.edgelessNodes[i][j].getValid(),
		        							i, j, null, null, null, null);
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
				System.out.println("Serializing the nodes array...");
				
				if (serializationEnabled) {
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
	}
	
	/**
	 * Load nodes based on RGB of image. Eventually we'll just serialize and deserialize them directly.
	 */
	private void loadNodes() throws IOException {
		System.out.println("Generating grid of nodes using image...");
		BufferedImage bufferedMapImage = ImageIO.read(new File(this.nodesImagePath));
		//BufferedImage bufferedMapImage = ImageIO.read(MapPanel.class.getResource("C:\\Users\\Benjamin\\Documents\\School\\Fall 2019\\CS 321\\CS321\\CS321CourseProject\\src\\Res\\CampusMapForNodes.png"));
		byte[] pixels = ((DataBufferByte)bufferedMapImage.getRaster().getDataBuffer()).getData();
		
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
            boolean valid = (argbValue != -16777216);
            if (valid)
            	numberOfValidNodes = numberOfValidNodes + 1;
			Node nextNode = new Node(nodeID, "Node " + nodeID++, valid, row, col, 
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
	    Graphics2D g2 = (Graphics2D) g;
	    
	    // We use an AffineTransform to scale the image in order to provide zoom functionality.
        AffineTransform transform = new AffineTransform();

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

        // Apply the translation and then the scaling (zoom). Update prev. zoom amount value.
        if (shouldTranslate) {
        	// Need to add the offsets in order to get this to work properly w/zooming.
        	transform.translate(xOffset + translateX, yOffset + translateY);
        	
        	// Update these values when button isn't pressed. If they're always updated,
        	// then you'll be stuck moving in one direction and it'll speed up as you drag. Very weird.
        	if (updateOffsetsOnTranslate) {
        		xOffset += translateX;
        		yOffset += translateY;
            	shouldTranslate = false;
        	}
        }
        else {
        	transform.translate(xOffset, yOffset);
        }
        transform.scale(currentZoomAmount, currentZoomAmount);
        prevZoomAount = currentZoomAmount;
        g2.transform(transform);
        
        // Update the current image bounds based on the transform. Useful for determining if used click image and where. 
        imageBounds = transform.createTransformedShape(new Rectangle(mapImage.getWidth(null), mapImage.getHeight(null))).getBounds2D();
        
    	// printDebugInfo();
        
        if (displayValidNodes ) {
        	displayValidNodes = false;
        	
        	for (int i = 0; i < nodes.length; i++) {
            	for (int j = 0; j < nodes[i].length; j++) {
            		Node n = nodes[i][j];
            		if (n.getValid()) {
            			g2.setColor(Color.GREEN);
            			g2.fillOval(j, i, 1, 1);
            		}
            	}
            }        	
        }
        
        for (Clicked c : lastTenClicked) {
        	if (c.valid == true) {
        		g2.setColor(Color.GREEN);
        	}
        	else {
        		g2.setColor(Color.RED);
        	}
        	// System.out.println("Filling oval cenetered at (" + c.location.x + "," + c.location.y + ")");
        	g2.fillOval(c.location.x, c.location.y, 25, 25);
        }
        
        // Draw the image on the screen with transformation applied.
	    g2.drawImage(mapImage,  0,  0,  null);
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

	@Override
	public void mouseWheelMoved(MouseWheelEvent eventArgs) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseDragged(MouseEvent eventArgs) {
		// Get the current point and determine how much the user has moved since clicking.
		Point currentPoint = eventArgs.getLocationOnScreen();
		
		translateX = currentPoint.x - mouseDragStart.x;
		translateY = currentPoint.y - mouseDragStart.y;
		
		shouldTranslate = true;
		repaint();
	}

	@Override
	public void mouseMoved(MouseEvent eventArgs) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseClicked(MouseEvent eventArgs) {
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
			if (clickedNode.getValid()) {
				System.out.println("Clicked VALID node at image coordinates (" + adjustedForImage.getX() + "," + adjustedForImage.getY() + ")" + ". (Image clicked!)");
				c = new Clicked(clicked, true);
				
				if (this.nextClickSetsStart) 
				{
					System.out.println("Starting node set to node at (" + adjustedForImage.getX() + "," + adjustedForImage.getY() + ")");
					this.startingNode = clickedNode;
					this.nextClickSetsStart = false;
				}
				else if (this.nextClickSetsDest) {
					System.out.println("Destination node set to node at (" + adjustedForImage.getX() + "," + adjustedForImage.getY() + ")");
					this.destNode = clickedNode;
					this.nextClickSetsDest = false;					
				}
			}			
			else {
				System.out.println("Clicked INVALID node at image coordinates (" + adjustedForImage.getX() + "," + adjustedForImage.getY() + ")" + ". (Image clicked!)");
				c = new Clicked(clicked, false);
			}
			System.out.println("Screen coordinates = (" + clicked.getX() + "," + clicked.getY() + ")");
		} else {
			System.out.println("Click at " + clicked + ". (Image NOT clicked!)");
		}
		
		if (c != null) {
			lastTenClicked.add(c);
			
			if (lastTenClicked.size() > 10) 
				lastTenClicked.removeFirst();
		}
		// System.out.println("Current Image Bounds: " + imageBounds);
	}

	@Override
	public void mouseEntered(MouseEvent eventArgs) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent eventArgs) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent eventArgs) {
		// Pressed just means the button was pressed down, not necessarily released.
		// Clicked means the user pressed and released the button.
		
		updateOffsetsOnTranslate = false; // User clicked so this should be false.
		// getPoint() is relative to the source component.
		mouseDragStart = MouseInfo.getPointerInfo().getLocation();
	}

	@Override
	public void mouseReleased(MouseEvent eventArgs) {
		updateOffsetsOnTranslate = true;	// That also means this should now be set to true.
		
		repaint(); // Drag ended so repaint.
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