package togos.scrolly1;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import togos.scrolly1.tfunc.AccellerativePositionFunction;

public class ScrollyApplet extends Apallit
{
	private static final long serialVersionUID = 6696808930542288751L;
	
	final ScrollyPaintable sp = new ScrollyPaintable();
	int autoScaleArea = 384*384;
	
	public ScrollyApplet() {
		super("Scrolly1");
	}
	
	protected void dealWithAppletParameters() {
		String k;
		
		if( this.getParameterInfo() == null ) return;
		
		k = getParameter("auto-scale-area");
		if( k != null ) autoScaleArea = Integer.parseInt(k);
		
		k = getParameter("antialiasing");
		if( k != null ) {
			k = k.toLowerCase();
			if( !k.equals("no") && !k.equals("false") && !k.equals("off") ) {
				sp.antialiasing = true;
			}
		}
		System.err.println(getParameter("antialiasing"));
	}
	
	AccellerativePositionFunction apf;
	
	protected void setPosition( AccellerativePositionFunction apf ) {
		sp.positionFunction = apf;
		this.apf = apf;
	}
	protected void setAccelleration( double ddx, double ddy, double ddz ) {
		setPosition( apf.withAccelleration( System.currentTimeMillis(), ddx, ddy, ddz ) );
	}
	
	class SimpleKeyboardKeyListener implements KeyListener {
		SimpleKeyboardListener skl;
		
		public SimpleKeyboardKeyListener( SimpleKeyboardListener l ) {
			this.skl = l;
		}
		
		@Override public void keyPressed(KeyEvent e) { skl.keyDown(e.getKeyCode(), e.getKeyChar()); }
		@Override public void keyReleased(KeyEvent e) { skl.keyUp(e.getKeyCode(), e.getKeyChar()); }
		@Override public void keyTyped(KeyEvent e) { /* no-op */ }
	}
	
	public void init() {
		super.init();
		dealWithAppletParameters();
		sp.init();
		sp.positionFunction = new AccellerativePositionFunction( System.currentTimeMillis(), -2000, 0, 0, 10, 2, 0, 0, 0, 0 );
		fillWith( sp, 1024, 512, 30, autoScaleArea );
		KeyListener kl = new SimpleKeyboardKeyListener(new KeyboardCameraController(sp,
			new GraphicSettingsAdjuster() {
				@Override public void toggleAa() {
					sp.antialiasing ^= true;
				}
				
				@Override public void increaseDetail() {
					dbc.autoScaleArea *= 2;
					
					if( dbc.autoScaleArea > 1024 * 1024 ) {
						dbc.autoScaleArea = 64 * 64;
					}
				}
				
				@Override public void decreaseDetail() {
					dbc.autoScaleArea /= 2;
					
					if( dbc.autoScaleArea < 64 * 64 ) {
						dbc.autoScaleArea = 1024 * 1024;
					}
				}
			}
		));
		for( Component c : this.getComponents() )  c.addKeyListener(kl);
		addKeyListener(kl);
		requestFocus();
	}
	
	public static void main( String[] args ) {
		ScrollyApplet a = new ScrollyApplet();
		a.runWindowed();
	}
}
