package togos.scrolly1;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

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
			starColors[i] = new Color( intensity, intensity, intensity );
		}
	}
	
	class LayerObject {
		final ColorFunction color;
		final Shape shape;
		final List<LayerObjectInstance> subObjects;
		
		public LayerObject( ColorFunction c, Shape s, List<LayerObjectInstance> subObjects ) {
			this.color = c;
			this.shape = s;
			this.subObjects = subObjects;
		}
		
		public LayerObject( ColorFunction c, Shape s ) {
			this( c, s, Collections.EMPTY_LIST );
		}
		
		public void draw( long timestamp, Graphics2D g2d ) {
			g2d.setColor( color.getAwtColor(timestamp) );
			g2d.fill( shape );
			AffineTransform oldXf = g2d.getTransform();
			for( LayerObjectInstance soi : subObjects ) {
				_draw( soi, timestamp, g2d );
				g2d.setTransform(oldXf);
			}
		}
	}
	
	class LayerObjectInstance {
		final double x, y, scale;
		final ScalarFunction rot;
		final LayerObject o;
		
		public LayerObjectInstance( double x, double y, ScalarFunction rot, double scale, LayerObject o ) {
			this.x = x; this.y = y;
			this.rot = rot; this.scale = scale;
			this.o = o;
		}
		
		public LayerObjectInstance( double x, double y, LayerObject o ) {
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
	
	protected LayerObject mast( double height, int nLights, int lightSide, long lightPhase ) {
		List<LayerObjectInstance> mastLights = new ArrayList();
		for( int i=1; i<=nLights; ++i ) {
			mastLights.add( new LayerObjectInstance( lightSide, height * i / nLights, new LayerObject( new PulsatingColorFunction(-1, 1, 0, 0, 1, 1, 0, 0, 2000, lightPhase + 2000 * -i / nLights ), new Rectangle( -2, -2, 4, 4) ) ) );
		}
		if( height > 180 )
			mastLights.add( new LayerObjectInstance(-lightSide, height, new LayerObject( new PulsatingColorFunction(-1, 1, 1, 1, 1, 1, 1, 1, 1500, lightPhase), new Rectangle( -3, -3, 6, 6) ) ) );
		return new LayerObject( BLACK, new Rectangle( -2, -20, 4, (int)(height + 20) ), mastLights );
	}
	
	protected LayerObject mast( Random r ) {
		double height = 180 + r.nextGaussian() * 60;
		return mast( height, (int)(height / 60 + r.nextInt(1)), r.nextInt(3) - 1, r.nextInt() % 4000 );
	}
	
	protected LayerObject building( Random r ) {
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
		for( int f=2; f<floorsHigh; ++f ) {
			for( int p=0; p<roomsWide; ++p ) {
				if( allLightsOn || r.nextDouble() < lightChance ) {
					int roomX = baseX + windowSeparation + p * (windowWidth+windowSeparation)*2;
					windows.add( new LayerObjectInstance( roomX, f*floorHeight,
						new LayerObject( WINDOW_COLOR, new Rectangle(windowWidth, windowHeight))));
					windows.add( new LayerObjectInstance( roomX + windowWidth+windowSeparation, f*floorHeight,
						new LayerObject( WINDOW_COLOR, new Rectangle(windowWidth, windowHeight))));
				}
			}
			lightChance += r.nextGaussian() * 0.2;
			if( lightChance < 0 ) lightChance = 0;
			if( lightChance > 1 ) lightChance = 1;
		}
		
		return new LayerObject( BLACK, new Rectangle(baseX, -20, width, floorsHigh*floorHeight + 20), windows ); 
	}
	
	public void init() {
		LayerObject o2 = new LayerObject( BLACK, new Rectangle( - 2, -10, 20,  40 ), Collections.EMPTY_LIST );
		LayerObject o3 = new LayerObject( BLACK, new Rectangle( -20, -20, 40,  80 ), Collections.EMPTY_LIST );
		LayerObject o4 = new LayerObject( BLACK, new Rectangle( -20, -20, 20,  40 ), Collections.EMPTY_LIST );
		LayerObject o5 = new LayerObject( BLACK, new Rectangle( -20, -20, 40, 160 ), Collections.EMPTY_LIST );
		
		/*
		List<LayerObjectInstance> mastLights = new ArrayList();
		mastLights.add( new LayerObjectInstance( 1,  60, new LayerObject( new PulsatingColorFunction(0, -1, 0, 0, 1, 1, 0, 0, 2000, 1500), new Rectangle( -2, -2, 4, 4) ) ) );
		mastLights.add( new LayerObjectInstance( 1, 120, new LayerObject( new PulsatingColorFunction(0, -1, 0, 0, 1, 1, 0, 0, 2000, 1000), new Rectangle( -2, -2, 4, 4) ) ) );
		mastLights.add( new LayerObjectInstance( 1, 180, new LayerObject( new PulsatingColorFunction(0, -1, 0, 0, 1, 1, 0, 0, 2000,  500), new Rectangle( -2, -2, 4, 4) ) ) );
		mastLights.add( new LayerObjectInstance( 1, 240, new LayerObject( new PulsatingColorFunction(0, -1, 0, 0, 1, 1, 0, 0, 2000,    0), new Rectangle( -2, -2, 4, 4) ) ) );
		mastLights.add( new LayerObjectInstance(-1, 240, new LayerObject( new PulsatingColorFunction(-1, 1, 1, 1, 1, 1, 1, 1, 1500,    0), new Rectangle( -3, -3, 6, 6) ) ) );
		LayerObject mast = new LayerObject( BLACK, new Rectangle( -2, -20, 4, 260 ), mastLights );
		*/
		
		LayerObject pineTree = new LayerObject( BLACK, new Polygon( new int[] { -1, -1, -4, 0, 4, 1, 1 }, new int[]{ -2, 1, 1, 16, 1, 1, -2 }, 7 ) );
		
		Random r = new Random(123123);
		
		ScalarFunction[] pineTreeWagPhases = new ScalarFunction[8];
		for( int i=0; i<pineTreeWagPhases.length; ++i ) {
			final double baseRot = (r.nextGaussian() - 0.5) * 0.15;
			final long offset = r.nextLong() % 3000;
			pineTreeWagPhases[i] = new ScalarFunction() {
				@Override public double getValue(long at) {
					return baseRot + (TMath.periodic( at+offset, 3000 )-0.5) * 0.05;
				}
			};
		}
		
		int segmentsPerGroundSection = 8;
		double[] px = new double[segmentsPerGroundSection+2];
		double[] py = new double[segmentsPerGroundSection+2];
		double[] pz = new double[segmentsPerGroundSection+2];
		
		int[] dists = {
			40, 70, 100, 150, 200, 300, 400, 800, 1200, 1600, 2000, 4000
			//40, 80, 160, 240, 320, 640, 1280, 2560, 5120, 20480, 40960
		};
		
		for( int i=0; i<dists.length; ++i ) {
			int dist = dists[i];
			List<LayerObjectInstance> layerObjects = new ArrayList();
			
			double groundSectionSegmentSize = dist / 16;
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
				
				Polygon p = new Polygon();
				p.addPoint( (int)(groundSectionSegmentSize*(heights.length-1)-(groundSectionSpan/2)), (int)(minHeight-2000) );
				p.addPoint( (int)(-(groundSectionSpan/2)), (int)(minHeight-2000) );
				for( int k=0; k<heights.length; ++k ) {
					p.addPoint( (int)(groundSectionSegmentSize*k - groundSectionSpan/2), (int)(heights[k]) );
				}
				layerObjects.add( new LayerObjectInstance( j+groundSectionSpan/2, 0, new LayerObject( BLACK, p, Collections.EMPTY_LIST ) ) );
				
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
	
	LinearGradientPaint fogPaint = new LinearGradientPaint(0, 0, 0, 2048,
		new float[] { 0.0f, 0.25f, 0.5f, 0.75f, 1.0f },
		new Color[] {
			new Color( 0.35f, 0.3f, 0.4f, 0.20f ),
			new Color( 0.35f, 0.3f, 0.4f, 0.10f ),
			new Color( 0.35f, 0.3f, 0.4f, 0.05f ),
			new Color( 0.35f, 0.3f, 0.4f, 0.01f ),
			new Color( 0.35f, 0.3f, 0.4f, 0.00f ),
		}
	);
	
	PositionFunction positionFunction = new ConstantPositionFunction( 0, 0, 0 );
	
	@Override
	public void paint(long timestamp, int width, int height, Graphics2D g2d) {
		g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		g2d.setRenderingHint( RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY );
		
		Random r = new Random(123123);
		
		g2d.setColor( Color.BLACK );
		g2d.fillRect(0,  0, width, height);
		
		// Draw stars:
		for( int i=width*height/256; i>=0; --i ) {
			g2d.setColor( starColors[r.nextInt(starColors.length)] );
			g2d.fillRect( r.nextInt(width), r.nextInt(height), 1, 1 );
		}
		
		ArrayList<Layer> sortedLayers = new ArrayList<Layer>(layers);
		Collections.sort( sortedLayers, new Comparator<Layer>() {
			@Override public int compare(Layer o1, Layer o2) {
				if( o1.distance > o2.distance ) return -1;
				if( o1.distance < o2.distance ) return +1;
				return 0;
			}
		});
		
		g2d.translate( width/2, height/2 );
		AffineTransform baseTransform = g2d.getTransform();
		
		double[] pos = new double[3];
		positionFunction.getPosition(timestamp, pos);
		
		//double worldCenterX = (timestamp - beginTimestamp) / 10f;
		// double worldCenterY = 10 + (timestamp - beginTimestamp) / 400f;
		// double worldCenterY = value( groundHeight, worldCenterX, 0, 0 ) + 10;
		//double worldCenterY = 300 + 300 * TMath.periodic(timestamp, 32000);
		
		for( Layer l : sortedLayers ) {
			double scale = height / 3 / l.distance - pos[2];
			
			g2d.scale( scale, -scale );
			
			double worldScreenWidth = width/scale;
			double worldScreenHeight = height/scale;
			double worldScreenRight = pos[0] - worldScreenWidth/2;
			double worldScreenBottom = pos[1] - worldScreenHeight/2;
					
			g2d.translate( -pos[0], -pos[1] );
			
			//Color topColor = new Color( 0.35f, 0.3f, 0.4f, 0.01f );
			//Color topColor = new Color( 0.0f, 0.0f, 0.0f, 0.00f );
			//Color bottomColor = new Color( 0.35f, 0.3f, 0.4f, 0.2f );
			g2d.setPaint( fogPaint ); //new GradientPaint(0, 0, bottomColor, 0, 512, topColor) );
			g2d.fillRect(
				(int)(worldScreenRight-2), (int)(worldScreenBottom-2),
				(int)(worldScreenWidth+4), (int)(worldScreenHeight+4) );
			
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
			AffineTransform xf = g2d.getTransform();
			for( int idx = beginIdx; idx < objCount ; ++idx ) {
				LayerObjectInstance loi = l.objects.get(idx);
				try {
					if( loi.x - maxRad > worldScreenRight + worldScreenWidth ) break;
					_draw( loi, timestamp, g2d );
				} finally {
					g2d.setTransform(xf);
				}
			}
			g2d.setTransform( baseTransform );
		}
	}

	protected void _draw(LayerObjectInstance loi, long timestamp, Graphics2D g2d) {
		g2d.translate( loi.x, loi.y );
		g2d.rotate( loi.rot.getValue(timestamp) );
		g2d.scale( loi.scale, loi.scale );
		loi.o.draw(timestamp, g2d);
	}
}
