package togos.scrolly1;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import togos.scrolly1.tfunc.PositionFunction;

public class ScrollyApplet extends Apallit
{
	private static final long serialVersionUID = 6696808930542288751L;
	
	public int autoScaleArea = 1024*1024;
	
	protected void dealWithAppletParameters() {
		String k;
		
		k = getParameter("auto-scale-area");
		if( k != null ) autoScaleArea = Integer.parseInt(k);
	}
	
	class AccellerativePositionFunction
		implements PositionFunction
	{
		final long timestamp;
		final double x, y, dx, dy, ddx, ddy;
		
		public AccellerativePositionFunction( long ts, double x, double y, double dx, double dy, double ddx, double ddy ) {
			this.timestamp = ts;
			this.x = x; this.dx = dx; this.ddx = ddx;
			this.y = y; this.dy = dy; this.ddy = ddy;
		}
		
		@Override
		public void getPosition(long timestamp, double[] dest) {
			double dt = (timestamp - this.timestamp)/1000.0;
			dest[0] = x + dx * dt + ddx*dt*dt/2;
			dest[1] = y + dy * dt + ddy*dt*dt/2;
		}
		
		public AccellerativePositionFunction withAccelleration( long timestamp, double ddx, double ddy ) {
			double dt = (timestamp - this.timestamp)/1000.0;
			return new AccellerativePositionFunction(
				timestamp,
				x + dx * dt + this.ddx*dt*dt/2,
				y + dy * dt + this.ddy*dt*dt/2,
				dx + this.ddx*dt,
				dy + this.ddy*dt,
				ddx, ddy
			);
		}
	}
	
	ScrollyPaintable sp;
	
	AccellerativePositionFunction apf = new AccellerativePositionFunction( 0, 0, 0, 0, 0, 0, 0 );
	protected void setAccelleration( double ddx, double ddy ) {
		apf = apf.withAccelleration( System.currentTimeMillis(), ddx, ddy );
		sp.positionFunction = apf;
	}
	
	public void init() {
		super.init();
		dealWithAppletParameters();
		sp = new ScrollyPaintable();
		sp.positionFunction = apf;
		sp.init();
		fillWith( sp, 1024, 512, 30, autoScaleArea );
		KeyListener kl = new KeyListener() {
			boolean upPressed, downPressed, leftPressed, rightPressed;
			
			protected void updateAccelleration() {
				double ddx = 0, ddy = 0;
				if( leftPressed && !rightPressed ) ddx = -10; 
				if( rightPressed && !leftPressed ) ddx = +10;
				if( upPressed && !downPressed ) ddy = +10; 
				if( downPressed && !upPressed ) ddy = -10;
				setAccelleration( ddx, ddy );
			}
			
			@Override public void keyTyped(KeyEvent kevt) {}
			
			@Override public void keyReleased(KeyEvent kevt) {
				switch( kevt.getKeyCode() ) {
				case( KeyEvent.VK_UP    ): case( KeyEvent.VK_W ): upPressed = false; break;
				case( KeyEvent.VK_LEFT  ): case( KeyEvent.VK_A ): leftPressed = false; break;
				case( KeyEvent.VK_DOWN  ): case( KeyEvent.VK_S ): downPressed = false; break;
				case( KeyEvent.VK_RIGHT ): case( KeyEvent.VK_D ): rightPressed = false; break;
				}
				updateAccelleration();
			}
			
			@Override public void keyPressed(KeyEvent kevt) {
				switch( kevt.getKeyCode() ) {
				case( KeyEvent.VK_UP    ): case( KeyEvent.VK_W ): upPressed = true; break;
				case( KeyEvent.VK_LEFT  ): case( KeyEvent.VK_A ): leftPressed = true; break;
				case( KeyEvent.VK_DOWN  ): case( KeyEvent.VK_S ): downPressed = true; break;
				case( KeyEvent.VK_RIGHT ): case( KeyEvent.VK_D ): rightPressed = true; break;
				}
				updateAccelleration();
			}
		};
		for( Component c : this.getComponents() )  c.addKeyListener(kl);
	}
	
	public static void main() {
		ScrollyApplet a = new ScrollyApplet();
		a.runWindowed();
	}
}
