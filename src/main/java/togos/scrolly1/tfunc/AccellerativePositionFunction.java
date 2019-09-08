package togos.scrolly1.tfunc;

public class AccellerativePositionFunction
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