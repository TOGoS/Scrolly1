package togos.scrolly1;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import togos.scrolly1.gfx.ScrollyGraphicsOutput;

public class JWJGLScrollyGraphicsOutput implements ScrollyGraphicsOutput
{
	protected int transformStackLevel = 0;
	
	@Override public Object saveTransform() {
		GL11.glPushMatrix();
		Integer oldStackLevel = Integer.valueOf(transformStackLevel);
		++transformStackLevel;
		return oldStackLevel;
	}
	
	@Override public void restoreTransform(Object xf) {
		int newStackLevel = ((Integer)xf).intValue();
		while( transformStackLevel > newStackLevel ) {
			GL11.glPopMatrix();
			--transformStackLevel;
		}
		saveTransform();
	}
	
	public void resetMatrixStack() {
		while( transformStackLevel > 0 ) {
			GL11.glPopMatrix();
			--transformStackLevel;
		}
	}
	
	@Override public void translate(double x, double y) {
		GL11.glTranslated( x, y, 0 );
	}
	
	@Override public void scale(double xScale, double yScale) {
		GL11.glScaled( xScale, yScale, 1 );
	}
	
	@Override public void rotate(double amt) {
		GL11.glRotated( amt*180/Math.PI, 0, 0, 1 );
	}

	@Override public void setColor(Color c) {
		// For whatever reason, glColor4b seems to just ~not work~ sometimes
		//GL11.glColor4b( (byte)c.getRed(), (byte)c.getGreen(), (byte)c.getBlue(), (byte)c.getAlpha() );
		GL11.glColor4f( c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, c.getAlpha()/255f );
	}

	@Override public void fill() {
		GL11.glClear(GL11.GL_COLOR);
	}
	
	@Override public void pixel(float x, float y) {
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2f(x  , y  );
		GL11.glVertex2f(x+1, y  );
		GL11.glVertex2f(x+1, y+1 );
		GL11.glVertex2f(x  , y+1 );
		GL11.glEnd();
	}

	@Override public void quads(float[] xPoints, float[] yPoints, final int count) {
		GL11.glBegin(GL11.GL_QUADS);
		for( int i=0; i<count*4; ++i ) {
			GL11.glVertex2f( xPoints[i], yPoints[i] );
		}
		GL11.glEnd();
	}

	@Override public void triangles(float[] xPoints, float[] yPoints, int count) {
		GL11.glBegin(GL11.GL_TRIANGLES);
		for( int i=0; i<count*3; ++i ) {
			GL11.glVertex2f( xPoints[i], yPoints[i] );
		}
		GL11.glEnd();
	}

	@Override public void verticalGradient(float x1, float y1, float x2, float y2, Color c1, Color c2) {
		GL11.glBegin(GL11.GL_QUADS);
		setColor( c1 );
		GL11.glVertex2f(x1, y1);
		GL11.glVertex2f(x2, y1);
		setColor( c2 );
		GL11.glVertex2f(x2, y2);
		GL11.glVertex2f(x1, y2 );
		GL11.glEnd();
	}
}
