package togos.scrolly1.noise;

public class Add implements LFunctionDaDaDa_Da
{
	public final LFunctionDaDaDa_Da[] funx;
	
	public Add( LFunctionDaDaDa_Da[] funx ) {
		this.funx = funx;
	}
	
	@Override
	public void apply( int vectorSize, double[] x, double[] y, double[] z, double[] dest ) {
		double[] subDest = new double[vectorSize];
		for( int i=vectorSize-1; i>=0; --i ) dest[i] = 0;
		for( int j=0; j<funx.length; ++j ) {
			funx[j].apply(vectorSize, x, y, z, subDest);
			for( int i=vectorSize-1; i>=0; --i ) dest[i] += subDest[i];
		}
	}
}
