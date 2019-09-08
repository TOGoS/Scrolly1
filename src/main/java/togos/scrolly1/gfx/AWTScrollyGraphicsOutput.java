package togos.scrolly1.gfx;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

public class AWTScrollyGraphicsOutput implements ScrollyGraphicsOutput
{
	protected final Graphics2D g2d;
	
	public AWTScrollyGraphicsOutput( Graphics2D g2d ) {
		this.g2d = g2d;
	}
	
	@Override public Object saveTransform() {
		return g2d.getTransform();
	}

	@Override public void restoreTransform(Object xf) {
		g2d.setTransform( (AffineTransform)xf );
	}

	@Override public void translate( double x, double y ) {
		g2d.translate( x, y );
	}

	@Override public void scale( double xScale, double yScale ) {
		g2d.scale( xScale, yScale );
	}

	@Override public void rotate(double amt) {
		g2d.rotate( amt );
	}

	@Override public void setColor(Color c) {
		g2d.setColor(c);
	}
	
	@Override public void fill() {
		Rectangle clip = g2d.getClipBounds();
		g2d.fillRect( clip.x, clip.y, clip.width, clip.height );
	}
	
	@Override public void pixel(float x, float y) {
		g2d.fillRect( (int)x, (int)y, 1, 1 );
	}
	
	final int[] polyx = new int[4], polyy = new int[4];
	
	@Override public void quads(float[] xp, float[] yp, int count) {
		for( int i=(count-1)*4; i>=0; i-=4 ) {
			polyx[0] = (int)xp[i+0]; polyy[0] = (int)yp[i+0];
			polyx[1] = (int)xp[i+1]; polyy[1] = (int)yp[i+1];
			polyx[2] = (int)xp[i+2]; polyy[2] = (int)yp[i+2];
			polyx[3] = (int)xp[i+3]; polyy[3] = (int)yp[i+3];
			g2d.fillPolygon( polyx, polyy, 4 );
		}
	}

	@Override
	public void triangles(float[] xp, float[] yp, int count) {
		for( int i=(count-1)*3; i>=0; i-=4 ) {
			polyx[0] = (int)xp[i+0]; polyy[0] = (int)yp[i+0];
			polyx[1] = (int)xp[i+1]; polyy[1] = (int)yp[i+1];
			polyx[2] = (int)xp[i+2]; polyy[2] = (int)yp[i+2];
			g2d.fillPolygon( polyx, polyy, 3 );
		}
	}

	@Override
	public void verticalGradient( float x1, float y1, float x2, float y2, Color c1, Color c2 ) {
		g2d.setPaint( new GradientPaint(0, y1, c1, 0, y2, c2 ) );
		g2d.fillRect( (int)x1, (int)y1, (int)Math.ceil(x2-x1), (int)Math.ceil(y2-y1) );
		g2d.setPaint( null );
	}
}
