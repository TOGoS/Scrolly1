package togos.scrolly1.noise;

public class ZFilter implements LFunctionDaDaDa_Da
{
	public final double minZ, maxZ;
	
	public ZFilter( double minZ, double maxZ ) {
		this.minZ = minZ;
		this.maxZ = maxZ;
	}
	
	@Override
	public void apply(int vectorSize, double[] x, double[] y, double[] z, double[] dest) {
		for( int i=vectorSize-1; i>=0; --i ) {
			dest[i] = (z[i] >= minZ && z[i] <= maxZ ) ? 1 : 0;
		}
	}
}
