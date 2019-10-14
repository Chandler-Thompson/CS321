package UI;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class MapPanel extends JPanel implements MouseWheelListener, MouseListener, MouseMotionListener  {
	
	private final Image mapImage;
	private MainFrame parent;
	
    private double zoomFactor = 1;
    private double prevZoomFactor = 1;
    private boolean released;
    private double xOffset = 0;
    private double yOffset = 0;
    private int deltaX;
    private int deltaY;
    private Point dragStartingPoint;
	private boolean dragging = false;
	
	public MapPanel(MainFrame parent, String image) {
		this.parent = parent;
		this.mapImage = new ImageIcon(image).getImage();
		
		addMouseWheelListener(this);
	}
	
	@Override
    protected void processMouseWheelEvent(MouseWheelEvent eventArgs) {
        //Zoom in
        if (eventArgs.getWheelRotation() < 0) {
            zoomFactor *= 1.1;
            repaint();
        }
        //Zoom out
        if (eventArgs.getWheelRotation() > 0) {
            zoomFactor /= 1.1;
            repaint();
        }
        super.processMouseWheelEvent(eventArgs);
    }
	
	@Override
    public Dimension getPreferredSize() {
		if (mapImage == null) {
			return parent.getSize();
		}
        return new Dimension(mapImage.getWidth(null), mapImage.getHeight(null));
    }
	
	@Override
	public void paintComponent(Graphics g) {
	    super.paintComponent(g);
	    Graphics2D g2 = (Graphics2D) g;
        AffineTransform at = new AffineTransform();

        double xRel = MouseInfo.getPointerInfo().getLocation().getX() - getLocationOnScreen().getX();
        double yRel = MouseInfo.getPointerInfo().getLocation().getY() - getLocationOnScreen().getY();

        double zoomDiv = zoomFactor / prevZoomFactor;

        xOffset = (zoomDiv) * (xOffset) + (1 - zoomDiv) * xRel;
        yOffset = (zoomDiv) * (yOffset) + (1 - zoomDiv) * yRel;

        at.translate(xOffset, yOffset);
        at.scale(zoomFactor, zoomFactor);
        prevZoomFactor = zoomFactor;
        g2.transform(at);
        
        if (dragging) {
        	AffineTransform at2 = new AffineTransform();
            at2.translate(xOffset + deltaX, yOffset + deltaY);
            at2.scale(zoomFactor, zoomFactor);
            
            xOffset += deltaX;
            yOffset += deltaY;
            
            g2.transform(at2);        	
        }
	    
	    g2.drawImage(mapImage,  0,  0,  null);
	    // All drawings go here
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent eventArgs) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseDragged(MouseEvent eventArgs) {
		Point currentPoint = eventArgs.getLocationOnScreen();
		deltaX = currentPoint.x - dragStartingPoint.x;
		deltaY = currentPoint.y - dragStartingPoint.y;
		dragging = true;
		repaint();
	}

	@Override
	public void mouseMoved(MouseEvent eventArgs) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseClicked(MouseEvent eventArgs) {
		// TODO Auto-generated method stub
		
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
		dragStartingPoint = MouseInfo.getPointerInfo().getLocation();
		dragging = true;
	}

	@Override
	public void mouseReleased(MouseEvent eventArgs) {
		dragging = false;
		repaint();
	}
}
