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
		final double x, y, z, dx, dy, dz, ddx, ddy, ddz;
		
		public AccellerativePositionFunction( long ts, double x, double y, double z, double dx, double dy, double dz, double ddx, double ddy, double ddz ) {
			this.timestamp = ts;
			this.x = x; this.dx = dx; this.ddx = ddx;
			this.y = y; this.dy = dy; this.ddy = ddy;
			this.z = z; this.dz = dz; this.ddz = ddz;
		}
		
		@Override
		public void getPosition(long timestamp, double[] dest) {
			double dt = (timestamp - this.timestamp)/1000.0;
			dest[0] = x + dx * dt + ddx*dt*dt/2;
			dest[1] = y + dy * dt + ddy*dt*dt/2;
			dest[2] = z + dz * dt + ddz*dt*dt/2;
		}
		
		public AccellerativePositionFunction withVelocityAndAccelleration( long timestamp, double dx, double dy, double dz, double ddx, double ddy, double ddz ) {
			double dt = (timestamp - this.timestamp)/1000.0;
			return new AccellerativePositionFunction(
				timestamp,
				x + this.dx * dt + this.ddx*dt*dt/2,
				y + this.dy * dt + this.ddy*dt*dt/2,
				z + this.dz * dt + this.ddz*dt*dt/2,
				 dx,  dy,  dz,
				ddx, ddy, ddz
			);
		}
		
		public AccellerativePositionFunction withAccelleration( long timestamp, double ddx, double ddy, double ddz ) {
			double dt = (timestamp - this.timestamp)/1000.0;
			return withVelocityAndAccelleration(timestamp, dx+this.ddx*dt, dy+this.ddy*dt, dz+this.ddz*dt, ddx, ddy, ddz);
		}
	}
	
	ScrollyPaintable sp;
	AccellerativePositionFunction apf;
	double maxAccelleration = 30;
	
	protected void setPosition( AccellerativePositionFunction apf ) {
		sp.positionFunction = apf;
		this.apf = apf;
	}
	protected void setAccelleration( double ddx, double ddy, double ddz ) {
		setPosition( apf.withAccelleration( System.currentTimeMillis(), ddx, ddy, ddz ) );
	}
	
	public void init() {
		super.init();
		dealWithAppletParameters();
		sp = new ScrollyPaintable();
		sp.init();
		apf = new AccellerativePositionFunction( System.currentTimeMillis(), -2000, 0, 0, 10, 2, 0, 0, 0, 0 );
		sp.positionFunction = apf;
		fillWith( sp, 1024, 512, 30, autoScaleArea );
		KeyListener kl = new KeyListener() {
			boolean upPressed, downPressed, leftPressed, rightPressed, inPressed, outPressed;
			
			protected void updateAccelleration() {
				double ddx = 0, ddy = 0, ddz = 0;
				if( leftPressed && !rightPressed ) ddx = -maxAccelleration; 
				if( rightPressed && !leftPressed ) ddx = +maxAccelleration;
				if( upPressed && !downPressed ) ddy = +maxAccelleration;
				if( downPressed && !upPressed ) ddy = -maxAccelleration;
				if( inPressed && !outPressed ) ddz = +maxAccelleration;
				if( outPressed && !inPressed ) ddz = -maxAccelleration;
				setPosition( apf.withAccelleration( System.currentTimeMillis(), ddx, ddy, ddz ) );
			}
			
			@Override public void keyTyped(KeyEvent kevt) {
				// Doesn't seem to work!
			}
			
			@Override public void keyReleased(KeyEvent kevt) {
				switch( kevt.getKeyCode() ) {
				case( KeyEvent.VK_UP    ): case( KeyEvent.VK_W ): upPressed = false; break;
				case( KeyEvent.VK_LEFT  ): case( KeyEvent.VK_A ): leftPressed = false; break;
				case( KeyEvent.VK_DOWN  ): case( KeyEvent.VK_S ): downPressed = false; break;
				case( KeyEvent.VK_RIGHT ): case( KeyEvent.VK_D ): rightPressed = false; break;
				case( KeyEvent.VK_OPEN_BRACKET ): outPressed = false; break;
				case( KeyEvent.VK_CLOSE_BRACKET ): inPressed = false; break;
				}
				updateAccelleration();
			}
			
			@Override public void keyPressed(KeyEvent kevt) {
				switch( kevt.getKeyCode() ) {
				case( KeyEvent.VK_SPACE ):
					setPosition( apf.withVelocityAndAccelleration( System.currentTimeMillis(), 0, 0, 0, 0, 0, 0 ) );
					break;
				case( KeyEvent.VK_H ):
					sp.highQuality = !sp.highQuality;
					break;
				case( KeyEvent.VK_G ):
					if( dbc.autoScaleArea > 1024 * 1024 ) {
						dbc.autoScaleArea = 256 * 256;
					} else {
						dbc.autoScaleArea *= 2;
					}
					break;
				case( KeyEvent.VK_EQUALS ): case( KeyEvent.VK_PLUS ):
					sp.baseScale *= 1.5;
					break;
				case( KeyEvent.VK_MINUS ):
					sp.baseScale *= 0.75;
					break;
				case( KeyEvent.VK_UP    ): case( KeyEvent.VK_W ): upPressed = true; break;
				case( KeyEvent.VK_LEFT  ): case( KeyEvent.VK_A ): leftPressed = true; break;
				case( KeyEvent.VK_DOWN  ): case( KeyEvent.VK_S ): downPressed = true; break;
				case( KeyEvent.VK_RIGHT ): case( KeyEvent.VK_D ): rightPressed = true; break;
				case( KeyEvent.VK_OPEN_BRACKET ): outPressed = true; break;
				case( KeyEvent.VK_CLOSE_BRACKET ): inPressed = true; break;

				}
				updateAccelleration();
			}
		};
		for( Component c : this.getComponents() )  c.addKeyListener(kl);
		addKeyListener(kl);
		requestFocus();
	}
	
	public static void main() {
		ScrollyApplet a = new ScrollyApplet();
		a.runWindowed();
	}
}
