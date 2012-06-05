package togos.scrolly1;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import togos.scrolly1.tfunc.AccellerativePositionFunction;

public class LWJGLThing extends Canvas implements Runnable
{
	private static final long serialVersionUID = 1L;
	volatile boolean running = true;
	
	public void run() {
		ScrollyPaintable p = new ScrollyPaintable();
		// p.baseScale = 0.5;
		// p.positionFunction = new AccellerativePositionFunction( System.currentTimeMillis(), -4000, 0, 0, 100, 20, 0, 0, 0, 0 );
		JWJGLScrollyGraphicsOutput ren = new JWJGLScrollyGraphicsOutput();
		p.init();
		
		try {
			Display.setParent(this);
			Display.create();
			GL11.glClearColor(0,0.1f,0,1);
			setFocusable(false);
		} catch( LWJGLException e ) {
			throw new RuntimeException(e);
		}
		
		while( running ) {
			final int width = getWidth();
			final int height = getHeight();
			
			if( !running ) break;
			
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			
			GL11.glViewport(0, 0, width, height);
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glLoadIdentity();
			// Put 0,0 at top left of screen:
			GL11.glTranslatef(-1, 1, 1);
			// And make width,height correspond to bottom right:
			GL11.glScalef(2f/width, -2f/height, 1);
			
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			GL11.glLoadIdentity();
			
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
			GL11.glClear(GL11.GL_STENCIL_BITS);
			
			p.paint( System.currentTimeMillis(), width, height, ren );
			ren.resetMatrixStack();
			
			GL11.glFlush();
			Display.sync(120);
			Display.update();
		}
		//Display.destroy();
	}
	
	public void stop() {
		running = false;
	}
	
	public static void main( String[] args ) {
		final LWJGLThing t = new LWJGLThing();
		t.setPreferredSize( new Dimension( 512, 512 ) );
		final Frame f = new Frame("Scrolly1 (JWJGL)");
		f.addWindowListener( new WindowAdapter() {
			@Override public void windowClosing(WindowEvent evt) {
				t.stop();
				f.dispose();
			}
		});
		f.add(t);
		f.pack();
		f.setVisible( true );
		new Thread(t).start();
	}
}
