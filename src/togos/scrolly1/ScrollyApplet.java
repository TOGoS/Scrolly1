package togos.scrolly1;

public class ScrollyApplet extends Apallit
{
	private static final long serialVersionUID = 6696808930542288751L;
	
	public int autoScaleArea = 1024*1024;
	
	protected void dealWithAppletParameters() {
		String k;
		
		k = getParameter("auto-scale-area");
		if( k != null ) autoScaleArea = Integer.parseInt(k);
	}
	
	public void init() {
		super.init();
		dealWithAppletParameters();
		ScrollyPaintable sp = new ScrollyPaintable();
		sp.init();
		fillWith( sp, 1024, 512, 30, autoScaleArea );
	}
	
	public static void main() {
		ScrollyApplet a = new ScrollyApplet();
		a.runWindowed();
	}
}
