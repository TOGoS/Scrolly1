package togos.scrolly1.noise;

public class Scale implements LFunctionDaDaDa_Da
{
	public final double scaleX, scaleY, scaleZ, scaleV;
	public final LFunctionDaDaDa_Da next;
	
	public Scale( double scaleX, double scaleY, double scaleZ, double scaleV, LFunctionDaDaDa_Da next ) {
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		this.scaleZ = scaleZ;
		this.scaleV = scaleV;
		this.next = next;
	}
	
	@Override
	public void apply(int vectorSize, double[] x, double[] y, double[] z, double[] dest) {
		double[] sx = new double[vectorSize];
		double[] sy = new double[vectorSize];
		double[] sz = new double[vectorSize];
		for( int i=vectorSize-1; i>=0; --i ) {
			sx[i] = x[i]*scaleX;
			sy[i] = y[i]*scaleY;
			sz[i] = z[i]*scaleZ;
		}
		next.apply(vectorSize, sx, sy, sz, dest);
		for( int i=vectorSize-1; i>=0; --i ) {
			dest[i] *= scaleV;
		}
	}
}
