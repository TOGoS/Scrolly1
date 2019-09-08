package togos.scrolly1.noise;

public class Constant implements LFunctionDaDaDa_Da
{
	public final double c;
	
	public Constant( double c ) {
		this.c = c;
	}
	
	@Override
	public void apply(int vectorSize, double[] x, double[] y, double[] z, double[] dest) {
		for( int i=vectorSize-1; i>=0; --i ) dest[i] = c;
	}
}
