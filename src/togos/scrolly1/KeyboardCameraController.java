package togos.scrolly1;

import java.awt.event.KeyEvent;

import togos.scrolly1.tfunc.AccellerativePositionFunction;
import togos.scrolly1.util.TMath;

public class KeyboardCameraController implements SimpleKeyboardListener
{
	ScrollyPaintable sp;
	GraphicSettingsAdjuster ga;
	double maxAccelleration = 30;
	
	public KeyboardCameraController( ScrollyPaintable sp, GraphicSettingsAdjuster ga ) {
		this.sp = sp;
		this.ga = ga;
	}
	
	protected void setPosition( AccellerativePositionFunction apf ) {
		sp.positionFunction = apf;
	}
	
	protected AccellerativePositionFunction getApf( long timestamp ) {
		if( sp.positionFunction instanceof AccellerativePositionFunction ) {
			return (AccellerativePositionFunction)sp.positionFunction;
		} else {
			double[] pos = new double[3];
			sp.positionFunction.getPosition(timestamp, pos );
			return new AccellerativePositionFunction( timestamp, pos[0], pos[1], pos[2], 0, 0, 0, 0, 0, 0 );
		}
	}
	
	protected AccellerativePositionFunction getApf() {
		return getApf(System.currentTimeMillis());
	}
	
	protected void setAccelleration( double ddx, double ddy, double ddz ) {
		setPosition( getApf().withAccelleration( System.currentTimeMillis(), ddx, ddy, ddz ) );
	}
	
	boolean shiftPressed;
	boolean upPressed, downPressed, leftPressed, rightPressed, inPressed, outPressed;
	
	protected float powerCycle( float value, int minPower, int maxPower ) {
		double base = 1.25;
		int currentPower = (int)Math.round(Math.log(value)/Math.log(base));
		currentPower += shiftPressed ? -1 : 1;
		currentPower = minPower + TMath.fdMod( currentPower-minPower, maxPower-minPower );
		return (float)Math.pow( base, currentPower );
	}
	
	protected void updateAccelleration() {
		double ddx = 0, ddy = 0, ddz = 0;
		if( leftPressed && !rightPressed ) ddx = -maxAccelleration; 
		if( rightPressed && !leftPressed ) ddx = +maxAccelleration;
		if( upPressed && !downPressed ) ddy = +maxAccelleration;
		if( downPressed && !upPressed ) ddy = -maxAccelleration;
		if( inPressed && !outPressed ) ddz = +maxAccelleration;
		if( outPressed && !inPressed ) ddz = -maxAccelleration;
		setPosition( getApf().withAccelleration( System.currentTimeMillis(), ddx, ddy, ddz ) );
	}
	
	@Override public void keyUp( int keyCode, char keyChar ) {
		switch( keyCode ) {
		case( KeyEvent.VK_SHIFT ): shiftPressed = false; break;
		case( KeyEvent.VK_UP    ): case( KeyEvent.VK_W ): upPressed = false; break;
		case( KeyEvent.VK_LEFT  ): case( KeyEvent.VK_A ): leftPressed = false; break;
		case( KeyEvent.VK_DOWN  ): case( KeyEvent.VK_S ): downPressed = false; break;
		case( KeyEvent.VK_RIGHT ): case( KeyEvent.VK_D ): rightPressed = false; break;
		case( KeyEvent.VK_OPEN_BRACKET ): outPressed = false; break;
		case( KeyEvent.VK_CLOSE_BRACKET ): inPressed = false; break;
		}
		updateAccelleration();
	}
	
	@Override public void keyDown( int keyCode, char keyChar ) {
		switch( keyCode ) {
		case( KeyEvent.VK_SPACE ):
			setPosition( getApf().withVelocityAndAccelleration( System.currentTimeMillis(), 0, 0, 0, 0, 0, 0 ) );
			break;
		case( KeyEvent.VK_H ):
			ga.toggleAa();
			break;
		case( KeyEvent.VK_B ):
			sp.fogBrightness = powerCycle(sp.fogBrightness, -10, +10);
			break;
		case( KeyEvent.VK_C ):
			sp.fogR = (float)(Math.random() * 0.5);
			sp.fogG = (float)(Math.random() * 0.5);
			sp.fogB = (float)(Math.random() * 0.5);
			break;
		case( KeyEvent.VK_F ):
			sp.fogOpacity = powerCycle(sp.fogOpacity, -50, +10);
			break;
		case( KeyEvent.VK_L ):
			sp.windowLightMode = (sp.windowLightMode + 1) % ScrollyPaintable.WINDOW_LIGHTS_MODE_COUNT;
			break;
		case( KeyEvent.VK_G ):
			if( shiftPressed ) ga.decreaseDetail(); else ga.increaseDetail();
			break;
		case( KeyEvent.VK_EQUALS ): case( KeyEvent.VK_PLUS ):
			sp.baseScale *= 1.5;
			break;
		case( KeyEvent.VK_MINUS ):
			sp.baseScale *= 0.75;
			break;
		case( KeyEvent.VK_SHIFT ): shiftPressed = true; break;
		case( KeyEvent.VK_UP    ): case( KeyEvent.VK_W ): upPressed = true; break;
		case( KeyEvent.VK_LEFT  ): case( KeyEvent.VK_A ): leftPressed = true; break;
		case( KeyEvent.VK_DOWN  ): case( KeyEvent.VK_S ): downPressed = true; break;
		case( KeyEvent.VK_RIGHT ): case( KeyEvent.VK_D ): rightPressed = true; break;
		case( KeyEvent.VK_OPEN_BRACKET ): outPressed = true; break;
		case( KeyEvent.VK_CLOSE_BRACKET ): inPressed = true; break;

		}
		updateAccelleration();
	}
}
