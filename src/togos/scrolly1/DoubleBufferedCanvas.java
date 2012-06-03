package togos.scrolly1;


import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public abstract class DoubleBufferedCanvas extends Canvas
{
	private static final long serialVersionUID = 1L;
	
	BufferedImage buffer;
	int scaling = 1;
	int autoScaleArea = Integer.MAX_VALUE;
	
	protected BufferedImage getBuffer( int width, int height ) {
		if( buffer == null || buffer.getWidth() != width || buffer.getHeight() != height ) {
			buffer = getGraphicsConfiguration().createCompatibleImage(width, height);
		}
		return buffer;
	}
	
	protected void paintBackground( Graphics g ) {
		g.setColor( getBackground() );
		Rectangle clip = g.getClipBounds();
		g.fillRect( clip.x, clip.y, clip.width, clip.height );
	}
	
	protected abstract void _paint( Graphics g, int virtualWidth, int virtualHeight );
		
	@Override
	public void paint( Graphics g ) {
		int w = getWidth();
		int h = getHeight();
		
		int scale = scaling;
		while( w * h > autoScaleArea * scale*scale ) {
			scale += 1;
		}
		
		int virtualWidth  = w / scale;
		int virtualHeight = h / scale;
		int destWidth  = virtualWidth * scale;
		int destHeight = virtualHeight * scale;
		
		if( w > 512 || h > 512 ) {
			// If the component is huge, only repaint the clipped region
			// (it might be reasonable to always do this)
			Rectangle clip = g.getClipBounds();
			
			int vClipX = clip.x / scale;
			int vClipY = clip.y / scale;
			int vClipWidth  = (int)Math.ceil( (float)(clip.x + clip.width)  / scale ) - vClipX;
			int vClipHeight = (int)Math.ceil( (float)(clip.y + clip.height) / scale ) - vClipY;
			
			Graphics2D bufferGraphics = getBuffer( vClipWidth, vClipHeight ).createGraphics();
			bufferGraphics.setClip( 0, 0, vClipWidth, vClipHeight );
			bufferGraphics.translate( -vClipX, -vClipY );
			_paint( bufferGraphics, virtualWidth, virtualHeight );
			// g.drawImage( buffer, clip.x, clip.y, null );
			g.drawImage( buffer, vClipX * scale, vClipY * scale, vClipWidth * scale, vClipHeight * scale, 0, 0, virtualWidth, virtualHeight, null );
		} else {
			Graphics2D bufferGraphics = getBuffer( virtualWidth, virtualHeight ).createGraphics();
			bufferGraphics.setClip( g.getClip() );
			_paint( bufferGraphics, virtualWidth, virtualHeight );
			//g.drawImage( buffer, 0, 0, null );
			g.drawImage( buffer, 0, 0, destWidth, destHeight, 0, 0, virtualWidth, virtualHeight, null );
		}
	}
	
	@Override
	public void update( Graphics g ) {
		paint(g);
	}
}
