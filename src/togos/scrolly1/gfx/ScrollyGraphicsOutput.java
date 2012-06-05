package togos.scrolly1.gfx;

import java.awt.Color;

public interface ScrollyGraphicsOutput
{
	public Object saveTransform();
	public void restoreTransform( Object xf );
	public void translate( double x, double y );
	public void scale( double xScale, double yScale );
	public void rotate( double amt );
	
	public void setColor( Color c );
	
	public void fill();
	public void pixel( float x, float y );
	public void quads( float[] xPoints, float[] yPoints, int count );
	public void triangles( float[] xPoints, float[] yPoints, int count );
	
	public void verticalGradient( float x1, float y1, float x2, float y2, Color c1, Color c2 );
}
