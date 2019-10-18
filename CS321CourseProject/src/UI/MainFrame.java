package UI;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;

public class MainFrame extends JFrame implements KeyListener {
	private boolean controlPressed = false;
	
	public MainFrame() {
		addKeyListener(this);
		setFocusable(true);
		setFocusTraversalKeysEnabled(false);
	}
	
	@Override
	public void keyPressed(KeyEvent eventArgs) {
		if (eventArgs.getKeyCode() == KeyEvent.VK_CONTROL)
			controlPressed = true;
		
	}

	@Override
	public void keyReleased(KeyEvent eventArgs) {
		if (eventArgs.getKeyCode() == KeyEvent.VK_CONTROL)
			controlPressed = false;
		
	}

	@Override
	public void keyTyped(KeyEvent eventArgs) {
		// TODO Auto-generated method stub
	}	
	
	public boolean getControlPressed() {
		return controlPressed;
	}
}
