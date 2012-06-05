package togos.scrolly1;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import togos.scrolly1.gfx.AWTScrollyGraphicsOutput;
import togos.scrolly1.gfx.ScrollyGraphicsOutput;
import togos.scrolly1.noise.Add;
import togos.scrolly1.noise.Constant;
import togos.scrolly1.noise.D5_2Perlin;
import togos.scrolly1.noise.LFunctionDaDaDa_Da;
import togos.scrolly1.noise.Multiply;
import togos.scrolly1.noise.Scale;
import togos.scrolly1.noise.ZFilter;
import togos.scrolly1.tfunc.ColorFunction;
import togos.scrolly1.tfunc.ConstantColorFunction;
import togos.scrolly1.tfunc.ConstantPositionFunction;
import togos.scrolly1.tfunc.ConstantScalarFunction;
import togos.scrolly1.tfunc.PositionFunction;
import togos.scrolly1.tfunc.PulsatingColorFunction;
import togos.scrolly1.tfunc.ScalarFunction;
import togos.scrolly1.util.TMath;

public class ScrollyPaintable implements TimestampedPaintable
{
	static Color[] starColors = new Color[16];
	static {
		for( int i=0; i<starColors.length; ++i ) {
			int intensity = (i) * 160 / starColors.length;
			starColors[i] = new Color( 255, 255, 255, intensity );
		}
	}
	
	interface Drawable {
		public abstract void draw( long timestamp, ScrollyGraphicsOutput ren );
	}
	
	static class DrawableShape implements Drawable {
		static final float[] EMPTY_FLOAT_ARRAY = new float[0];
		
		final int nQuads, nTriangles;
		final float[] quadPointsX, quadPointsY, trianglePointsX, trianglePointsY;
		
		public DrawableShape( int nQuads, float[] quadPointsX, float[] quadPointsY, int nTriangles, float[] trianglePointsX, float[] trianglePointsY ) {
			this.nQuads = nQuads; this.quadPointsX = quadPointsX; this.quadPointsY = quadPointsY;
			this.nTriangles = nTriangles; this.trianglePointsX = trianglePointsX; this.trianglePointsY = trianglePointsY;
		}
		
		@Override public void draw(long timestamp, ScrollyGraphicsOutput ren) {
			if( nQuads > 0 ) ren.quads( quadPointsX, quadPointsY, nQuads );
			if( nTriangles > 0 ) ren.triangles( trianglePointsX, trianglePointsY, nTriangles );
		}
		
		public static DrawableShape rectangle( float x, float y, float w, float h ) {
			return new DrawableShape( 1, new float[]{x, x+w, x+w, x}, new float[]{y, y, y+h, y+h}, 0, EMPTY_FLOAT_ARRAY, EMPTY_FLOAT_ARRAY );
		}
	}
	
	static class ShapeCreator {
		float[] quadX = new float[128];
		float[] quadY = new float[128];
		float[] triX = new float[128];
		float[] triY = new float[128];
		int quadCount = 0;
		int triCount = 0;
		
		public ShapeCreator addQuad( float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3 ) {
			int i = quadCount*4;
			quadX[i] = x0; quadY[i] = y0; ++i;
			quadX[i] = x1; quadY[i] = y1; ++i;
			quadX[i] = x2; quadY[i] = y2; ++i;
			quadX[i] = x3; quadY[i] = y3;
			++quadCount;
			return this;
		}
		
		public ShapeCreator addTriangle( float x0, float y0, float x1, float y1, float x2, float y2 ) {
			int i = triCount*4;
			triX[i] = x0; triY[i] = y0; ++i;
			triX[i] = x1; triY[i] = y1; ++i;
			triX[i] = x2; triY[i] = y2; ++i;
			++triCount;
			return this;
		}
		
		public ShapeCreator clear() {
			quadCount = triCount = 0;
			return this;
		}
		
		public DrawableShape toShape() {
			float[] qx = quadCount == 0 ? DrawableShape.EMPTY_FLOAT_ARRAY : new float[quadCount*4];
			float[] qy = quadCount == 0 ? DrawableShape.EMPTY_FLOAT_ARRAY : new float[quadCount*4];
			float[] tx = quadCount == 0 ? DrawableShape.EMPTY_FLOAT_ARRAY : new float[triCount*3];
			float[] ty = quadCount == 0 ? DrawableShape.EMPTY_FLOAT_ARRAY : new float[triCount*3];
			for( int i=quadCount*4-1; i>=0; --i ) {
				qx[i] = quadX[i];
				qy[i] = quadY[i];
			}
			for( int i=triCount*3-1; i>=0; --i ) {
				qx[i] = triX[i];
				qy[i] = triY[i];
			}
			return new DrawableShape( quadCount, qx, qy, triCount, tx, ty );
		}
	}
	
	class BasicDrawable implements Drawable {
		final ColorFunction cf;
		final DrawableShape shape;
		final List<LayerObjectInstance> subObjects;
		
		public BasicDrawable( ColorFunction cf, DrawableShape s, List<LayerObjectInstance> subObjects ) {
			this.cf = cf;
			this.shape = s;
			this.subObjects = subObjects;
		}
		
		public BasicDrawable( ColorFunction cf, DrawableShape s ) {
			this( cf, s, Collections.EMPTY_LIST );
		}
		
		@Override public void draw( long timestamp, ScrollyGraphicsOutput ren ) { 
			ren.setColor( cf.getAwtColor(timestamp) );
			shape.draw(timestamp, ren);
			Object oldXf = ren.saveTransform();
			for( LayerObjectInstance soi : subObjects ) {
				_draw( soi, timestamp, ren );
				ren.restoreTransform(oldXf);
			}
		}
	}
	
	class Building implements Drawable {
		final DrawableShape bodyShape;
		final List<LayerObjectInstance> windows;
		
		public Building( DrawableShape s, List<LayerObjectInstance> windows ) {
			this.bodyShape = s;
			this.windows = windows;
		}
		
		@Override public void draw( long timestamp, ScrollyGraphicsOutput ren ) {
			ren.setColor( Color.BLACK );
			bodyShape.draw(timestamp, ren);
			switch( windowLightMode ) {
			case( WINDOW_LIGHTS_NORMAL ):
				Object oldXf = ren.saveTransform();
				for( LayerObjectInstance soi : windows ) {
					_draw( soi, timestamp, ren );
					ren.restoreTransform(oldXf);
				}
				break;
			}
		}
	}
	
	class LayerObjectInstance {
		final double x, y, scale;
		final ScalarFunction rot;
		final Drawable o;
		
		public LayerObjectInstance( double x, double y, ScalarFunction rot, double scale, Drawable o ) {
			this.x = x; this.y = y;
			this.rot = rot; this.scale = scale;
			this.o = o;
		}
		
		public LayerObjectInstance( double x, double y, Drawable o ) {
			this( x, y, ConstantScalarFunction.ZERO, 1, o );
		}
	}
	
	class Layer {
		final double distance;
		final List<LayerObjectInstance> objects;
		
		public Layer( double dist, List<LayerObjectInstance> objs ) {
			this.distance = dist;
			this.objects = new ArrayList(objs);
			Collections.sort( this.objects, new Comparator<LayerObjectInstance>() {
				@Override
				public int compare(LayerObjectInstance o1, LayerObjectInstance o2) {
					return o1.x < o2.x ? -1 : o1.x > o2.x ? 1 : 0;
				}
			});
		}
	}
	
	static final ConstantColorFunction BLACK = new ConstantColorFunction(Color.BLACK);
	static final ConstantColorFunction WINDOW_COLOR = new ConstantColorFunction(new Color(0.85f, 0.85f, 0.8f));
	
	LFunctionDaDaDa_Da groundHeight = new Add( new LFunctionDaDaDa_Da[] {
		new Scale( 0.050, 0.050, 0.050,  10, D5_2Perlin.instance ),
		new Scale( 0.005, 0.050, 0.050,  20, D5_2Perlin.instance ),
		new Scale( 0.005, 0.050, 0.050,  30, D5_2Perlin.instance ),
		new Scale( 0.002, 0.002, 0.001, 100, D5_2Perlin.instance ),
		new Scale( 0.001, 0.001, 0.001, 200, D5_2Perlin.instance ),
		new Multiply( new LFunctionDaDaDa_Da[] {
			new Add( new LFunctionDaDaDa_Da[] {
				new Scale( 0.0005, 0.0005, 0.0005, 500, D5_2Perlin.instance ),
				new Scale( 0.0002, 0.0002, 0.0002, 500, D5_2Perlin.instance ),
				new Constant( 500 ),
			}),
			new ZFilter( 2000, 8000 ),
		}),
	});
	
	List<Layer> layers = new ArrayList();
	long beginTimestamp;
	
	protected double value( LFunctionDaDaDa_Da f, double x, double y, double z ) {
		double[] dest = new double[1];
		f.apply( 1, new double[]{x}, new double[]{y}, new double[]{z}, dest );
		return dest[0];
	}
	
	public ScrollyPaintable() {
	}
	
	protected Drawable mast( double height, int nLights, int lightSide, long lightPhase ) {
		List<LayerObjectInstance> mastLights = new ArrayList();
		for( int i=1; i<=nLights; ++i ) {
			mastLights.add( new LayerObjectInstance( lightSide, height * i / nLights, new BasicDrawable( new PulsatingColorFunction(-1, 1, 0, 0, 1, 1, 0, 0, 2000, lightPhase + 2000 * -i / nLights ), DrawableShape.rectangle( -2, -2, 4, 4) ) ) );
		}
		if( height > 180 )
			mastLights.add( new LayerObjectInstance(-lightSide, height, new BasicDrawable( new PulsatingColorFunction(-1, 1, 1, 1, 1, 1, 1, 1, 1500, lightPhase), DrawableShape.rectangle( -3, -3, 6, 6) ) ) );
		return new BasicDrawable( BLACK, DrawableShape.rectangle( -2, -20, 4, (int)(height + 20) ), mastLights );
	}
	
	protected Drawable mast( Random r ) {
		double height = 180 + r.nextGaussian() * 60;
		return mast( height, (int)(height / 60 + r.nextInt(1)), r.nextInt(3) - 1, r.nextInt() % 4000 );
	}
	
	protected Drawable building( Random r ) {
		int windowWidth = 1+r.nextInt(3);
		int windowHeight = 1+r.nextInt(3);
		int windowSeparation = 1 + r.nextInt(3);
		int floorHeight = windowHeight+2+r.nextInt(3);
		int floorsHigh = 1 + r.nextInt(4)*r.nextInt(4)*r.nextInt(4)*r.nextInt(4); // in floors
		int roomsWide = floorsHigh / 10 + r.nextInt(6); // in window [pairs]
		
		boolean allLightsOn = floorsHigh < 20 && r.nextDouble() < 0.2;
		
		List<LayerObjectInstance> windows = new ArrayList();
		int width = (roomsWide * (windowWidth*2 + windowSeparation)) + ((roomsWide + 1) * windowSeparation);
		if( width%2 == 1 ) width++;
		int baseX = -width / 2;
		double lightChance = 0.5;
		for( int f=2; f<floorsHigh-1; ++f ) {
			for( int p=0; p<roomsWide; ++p ) {
				if( allLightsOn || r.nextDouble() < lightChance ) {
					int roomX = baseX + windowSeparation + p * (windowWidth+windowSeparation)*2;
					windows.add( new LayerObjectInstance( roomX, f*floorHeight,
						new BasicDrawable( WINDOW_COLOR, DrawableShape.rectangle(0, 0, windowWidth, windowHeight))));
					windows.add( new LayerObjectInstance( roomX + windowWidth+windowSeparation, f*floorHeight,
						new BasicDrawable( WINDOW_COLOR, DrawableShape.rectangle(0, 0, windowWidth, windowHeight))));
				}
			}
			lightChance += r.nextGaussian() * 0.2;
			if( lightChance < 0 ) lightChance = 0;
			if( lightChance > 1 ) lightChance = 1;
		}
		
		return new Building( DrawableShape.rectangle(baseX, -20, width, floorsHigh*floorHeight + 20), windows ); 
	}
	
	ShapeCreator sc = new ShapeCreator();
	
	public void init() {
		Drawable o2 = new BasicDrawable( BLACK, DrawableShape.rectangle( - 2, -10, 20,  40 ) );
		Drawable o3 = new BasicDrawable( BLACK, DrawableShape.rectangle( -20, -20, 40,  80 ) );
		Drawable o4 = new BasicDrawable( BLACK, DrawableShape.rectangle( -20, -20, 20,  40 ) );
		Drawable o5 = new BasicDrawable( BLACK, DrawableShape.rectangle( -20, -20, 40, 160 ) );
		
		/*
		List<LayerObjectInstance> mastLights = new ArrayList();
		mastLights.add( new LayerObjectInstance( 1,  60, new LayerObject( new PulsatingColorFunction(0, -1, 0, 0, 1, 1, 0, 0, 2000, 1500), new Rectangle( -2, -2, 4, 4) ) ) );
		mastLights.add( new LayerObjectInstance( 1, 120, new LayerObject( new PulsatingColorFunction(0, -1, 0, 0, 1, 1, 0, 0, 2000, 1000), new Rectangle( -2, -2, 4, 4) ) ) );
		mastLights.add( new LayerObjectInstance( 1, 180, new LayerObject( new PulsatingColorFunction(0, -1, 0, 0, 1, 1, 0, 0, 2000,  500), new Rectangle( -2, -2, 4, 4) ) ) );
		mastLights.add( new LayerObjectInstance( 1, 240, new LayerObject( new PulsatingColorFunction(0, -1, 0, 0, 1, 1, 0, 0, 2000,    0), new Rectangle( -2, -2, 4, 4) ) ) );
		mastLights.add( new LayerObjectInstance(-1, 240, new LayerObject( new PulsatingColorFunction(-1, 1, 1, 1, 1, 1, 1, 1, 1500,    0), new Rectangle( -3, -3, 6, 6) ) ) );
		LayerObject mast = new LayerObject( BLACK, new Rectangle( -2, -20, 4, 260 ), mastLights );
		*/
		
		Drawable pineTree = new BasicDrawable( BLACK,
			new ShapeCreator().addQuad(-1, -2, -1, 1, 1, 1, 1, -2).addTriangle(-4, 1, 0, 15, 4, 1).toShape()
		);
		
		Random r = new Random(123123);
		
		ScalarFunction[] pineTreeWagPhases = new ScalarFunction[8];
		for( int i=0; i<pineTreeWagPhases.length; ++i ) {
			final double baseRot = r.nextGaussian() * 0.15;
			final long offset = r.nextLong() % (3000 + i*300);
			pineTreeWagPhases[i] = new ScalarFunction() {
				@Override public double getValue(long at) {
					return baseRot + TMath.periodic( at+offset, 3000 ) * 0.05;
				}
			};
		}
		
		int segmentsPerGroundSection = 4;
		double[] px = new double[segmentsPerGroundSection+1];
		double[] py = new double[segmentsPerGroundSection+1];
		double[] pz = new double[segmentsPerGroundSection+1];
		
		int[] dists = {
			40, 70, 100, 150, 200, 300, 400, 800, 1200, 1600, 2000, 4000
			//40, 80, 160, 240, 320, 640, 1280, 2560, 5120, 20480, 40960
		};
		
		for( int i=0; i<dists.length; ++i ) {
			int dist = dists[i];
			List<LayerObjectInstance> layerObjects = new ArrayList();
			
			double groundSectionSegmentSize = dist / 12;
			double groundSectionSpan = groundSectionSegmentSize*segmentsPerGroundSection;
			
			// Generate ground sections
			for( int j=0; j<pz.length; ++j ) pz[j] = dist;
			double terrainLength = Math.max( 32000, dist * 16 ); 
			for( double j=-terrainLength; j<terrainLength; ) {
				double[] heights = new double[px.length];
				for( int k=0; k<heights.length; ++k ) px[k] = j + k*groundSectionSegmentSize;
				groundHeight.apply(px.length, px, py, pz, heights);
				
				double minHeight = 99999;
				for( int k=0; k<heights.length; ++k ) minHeight = Math.min( minHeight, heights[k] );
				
				/*
				Polygon p = new Polygon();
				p.addPoint( (int)(groundSectionSegmentSize*(heights.length-1)-(groundSectionSpan/2)), (int)(minHeight-2000) );
				p.addPoint( (int)(-(groundSectionSpan/2)), (int)(minHeight-2000) );
				for( int k=0; k<heights.length; ++k ) {
					p.addPoint( (int)(groundSectionSegmentSize*k - groundSectionSpan/2), (int)(heights[k]) );
				}
				*/
				sc.clear();
				for( int k=0; k<heights.length-1; ++k ) {
					float left = (float)(groundSectionSegmentSize*k - groundSectionSpan/2);
					float right = (float)(left + groundSectionSegmentSize*1.1);
					float top0 = (float)heights[k], top1 = (float)heights[k+1];
					float bottom0 = top0 - 2000, bottom1 = top1 - 2000;
					sc.addQuad( left, bottom0, left, top0, right, top1, right, bottom1 );
				}
				
				layerObjects.add( new LayerObjectInstance( j+groundSectionSpan/2, 0, new BasicDrawable( BLACK, sc.toShape(), Collections.EMPTY_LIST ) ) );
				
				j += groundSectionSpan;
			}
			// Trees
			if( dist < 1000 ) {
				for( int j=0; j<2000; ++j ) {
					double x = r.nextGaussian() * 3000 - 4000;
					layerObjects.add( new LayerObjectInstance( x, value( groundHeight, x, 0, dist ), pineTreeWagPhases[r.nextInt(pineTreeWagPhases.length)], 0.5 + Math.random() * 1.5, pineTree ) );
				}
				for( int j=0; j<2000; ++j ) {
					double x = r.nextGaussian() * 3000 + 4000;
					layerObjects.add( new LayerObjectInstance( x, value( groundHeight, x, 0, dist ), pineTreeWagPhases[r.nextInt(pineTreeWagPhases.length)], 0.5 + Math.random() * 1.5, pineTree ) );
				}
			}
			// Buildings
			if( dist < 1500 ) {
				for( int j=0; j<100; ++j ) {
					double x = r.nextGaussian() * 2000 + 2000;
					layerObjects.add( new LayerObjectInstance( x, value( groundHeight, x, 0, dist ), r.nextBoolean() ? o2 : r.nextBoolean() ? o3 : r.nextBoolean() ? o4 : o5 ) );
				}
			}
			// Skyscrapers
			if( dist < 1500 ) {
				for( int j=0; j<100; ++j ) {
					double x = r.nextGaussian() * 1000 + 2000;
					layerObjects.add( new LayerObjectInstance( x, value( groundHeight, x, 0, dist ), building(r) ) );
						// r.nextBoolean() ? o2 : r.nextBoolean() ? o3 : r.nextBoolean() ? o4 : o5 ) );
				}
			}
			// Radio towers
			if( dist > 100 && dist < 3000 ) {
				for( int j=0; j<20; ++j ) {
					double x = r.nextGaussian() * 8000 - 2000;
					layerObjects.add( new LayerObjectInstance( x, value( groundHeight, x, 0, dist ), mast(r) ) );
				}
			}
			// Bigger radio towers
			if( dist > 2000 ) {
				for( int j=0; j<10; ++j ) {
					double x = r.nextGaussian() * 32000;
					layerObjects.add( new LayerObjectInstance( x, value( groundHeight, x, 0, dist ), ConstantScalarFunction.ZERO, 10, mast(r) ) );
				}
			}
			layers.add(new Layer( dist, layerObjects ));
		}
		beginTimestamp = System.currentTimeMillis();
	}
	
	protected static float clamp( float c ) {
		return c < 0 ? 0 : c > 1 ? 1 : c;
	}
	
	protected static Color brighten( float r, float g, float b, float alph, float brightness ) {
		return new Color( clamp(r*brightness), clamp(g*brightness), clamp(b*brightness), alph );
	}
	
	protected Color fogColor( float opacity ) {
		return new Color( clamp(fogR*fogBrightness), clamp(fogG*fogBrightness), clamp(fogB*fogBrightness), clamp(opacity) );
	}
	
	public static final int WINDOW_LIGHTS_OFF    = 0;
	public static final int WINDOW_LIGHTS_SIMPLE = 1;
	public static final int WINDOW_LIGHTS_NORMAL = 2;
	public static final int WINDOW_LIGHTS_MODE_COUNT = 3;
	
	public PositionFunction positionFunction = new ConstantPositionFunction( 0, 0, 0 );
	public boolean antialiasing = false;
	public boolean drawForegroundFog = true;
	public double baseScale = 1.0;
	public float fogBrightness = 1.0f;
	public float fogOpacity    = 0.002f;
	public float fogR = 0.35f, fogG = 0.3f, fogB = 0.4f;
	public int windowLightMode = WINDOW_LIGHTS_NORMAL;
	
	public void paint(long timestamp, int width, int height, ScrollyGraphicsOutput ren) {
		Random r = new Random(123123);
		
		ren.setColor( Color.BLACK );
		ren.fill();
		
		// Draw stars:
		for( int i=width*height/256; i>=0; --i ) {
			ren.setColor( starColors[r.nextInt(starColors.length)] );
			ren.pixel( r.nextInt(width), r.nextInt(height) );
		}
		
		ArrayList<Layer> sortedLayers = new ArrayList<Layer>(layers);
		Collections.sort( sortedLayers, new Comparator<Layer>() {
			@Override public int compare(Layer o1, Layer o2) {
				if( o1.distance > o2.distance ) return -1;
				if( o1.distance < o2.distance ) return +1;
				return 0;
			}
		});
		
		//if( true ) return;
		
		ren.translate( width/2, height/2 );
		Object baseTransform = ren.saveTransform();
		
		double[] pos = new double[3];
		positionFunction.getPosition(timestamp, pos);
		
		if( drawForegroundFog ) {
			sortedLayers.add( new Layer(pos[2]+10, Collections.EMPTY_LIST) );
		}
		
		double prevLayerDist = 16000;
		for( Layer l : sortedLayers ) {
			if( l.distance - pos[2] < 10 ) continue;
			
			double scale = baseScale * height / 3 / (l.distance - pos[2]);
			
			ren.scale( scale, -scale );
			
			// World positions of screen coordinates:
			double worldScreenWidth = width/scale;
			double worldScreenRight = pos[0] - worldScreenWidth/2;
			double worldScreenLeft = worldScreenRight + worldScreenWidth;
			
			ren.translate( -pos[0], -pos[1] );
			
			double fogDepth = prevLayerDist - l.distance;
			double fogTrans = Math.pow( 1 - fogOpacity, fogDepth );
			ren.verticalGradient( (float)worldScreenRight-2, -1024, (float)worldScreenLeft+2, 4096, fogColor((float)(1-fogTrans)), fogColor(0.0f));
			
			/** Estimate of maximum distance (in world units)
			 * outside the screen something might be that we should still draw */
			double maxRad = l.distance;
			if( maxRad < 40 ) maxRad = 40;
			
			int beginIdx = l.objects.size()/2;
			int searchDist = beginIdx/2;
			while( searchDist > 8 ) {
				searchDist /= 2;
				double box = l.objects.get(beginIdx).x;
				if( box+maxRad > worldScreenRight ) {
					beginIdx -= searchDist;
				} else if( box+maxRad < worldScreenRight ) {
					beginIdx += searchDist;
				}
			}
			while( beginIdx > 0 && l.objects.get(beginIdx).x + maxRad > worldScreenRight ) {
				--beginIdx;
			}
			final int objCount = l.objects.size();
			Object xf = ren.saveTransform();
			for( int idx = beginIdx; idx < objCount ; ++idx ) {
				LayerObjectInstance loi = l.objects.get(idx);
				try {
					if( loi.x - maxRad > worldScreenRight + worldScreenWidth ) break;
					_draw( loi, timestamp, ren );
				} finally {
					ren.restoreTransform(xf);
				}
			}
			ren.restoreTransform( baseTransform );
			
			prevLayerDist = l.distance;
		}
	}
	
	@Override
	public void paint(long timestamp, int width, int height, Graphics2D g2d) {
		if( antialiasing ) {
			g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
			g2d.setRenderingHint( RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY );
		}
		
		paint( timestamp, width, height, new AWTScrollyGraphicsOutput(g2d));
	}

	protected void _draw( LayerObjectInstance loi, long timestamp, ScrollyGraphicsOutput ren ) {
		ren.translate( loi.x, loi.y );
		ren.rotate( loi.rot.getValue(timestamp) );
		ren.scale( loi.scale, loi.scale );
		loi.o.draw(timestamp, ren);
	}
}
