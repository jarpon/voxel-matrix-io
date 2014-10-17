package inra.ijpb.voxelmatrix;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class implements the library methods needed to read/write VoxelMatrix
 * files.
 * 
 * @author Javier Arpon and Ignacio Arganda-Carreras
 *
 */
public class VoxelMatrixIO {

	/**
	 * Read VoxelMatrix file
	 * @param path input file name with path
	 * @return read image or null if error
	 */
	public static ImagePlus read( String path )
	{
		if (null == path) return null;	

		ImagePlus imp = new ImagePlus();

		try 
		{
			//create file input & data input stream
			FileInputStream fis = new FileInputStream( path );
			DataInputStream dis = new DataInputStream( fis );

			int size1, size2, size3;
			size1 = reverse( dis.readInt() );//0
			size2 = reverse( dis.readInt() );//0
			size3 = reverse( dis.readInt() );//0
			
			// Distinguish between versions: previous versions of VM files used 3 values
			if ( size1 > 0 && size2 > 0 && size3 > 0 ) 
			{
				//old version type = int;
				imp = readOldFormat( path, dis, size1, size2, size3 );
				fis.close();
				dis.close();
				if (size3>1) {
					imp.setSlice( size3/2);
					ImageProcessor ip = imp.getProcessor();
					ip.resetMinAndMax();
					imp.setDisplayRange(ip.getMin(),ip.getMax());
				}
			}
			else 
			{
				imp = openNewFormat(path, dis);
				fis.close();
				dis.close();
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			return null;
		} 
		return imp;
	}

	/**
	 * Read VoxelMatrix file stored in the old format
	 * 
	 * @param path image file name with complete path
	 * @param dis input data stream to read from
	 * @param size1 dimension 1
	 * @param size2 dimension 2
	 * @param size3 dimension 3
	 * @return read image or null if error
	 * @throws IOException
	 */
	static ImagePlus readOldFormat(
			String path, 
			DataInputStream dis, 
			int size1, 
			int size2,
			int size3 ) throws IOException
	{
		//prepare variables
		ImageStack stack = new ImageStack( size1, size2 );
		final int numPixels = size1 * size2;
		final int bufferSize = 4 * numPixels;
		final byte[] buffer = new byte[bufferSize];

		// write pixels
		for (int z=0; z < size3; ++z) 
		{
			final float[][] pixels = new float[ size1 ][ size2 ];
			int n = dis.read( buffer, 0, bufferSize );
			if ( n < 0 )
				return null;

			for(int j = 0; j < bufferSize; j+=4)
			{
				int tmp = (int)((buffer[j])<<24 | (buffer[j+1])<<16 | (buffer[j+2])<<8 | (buffer[j+3]));
				int currentPos = j / 4;
				int y = currentPos / size2;
				int x = currentPos % size2;
				pixels[ y ][ x ] = (float)(tmp);
			}
			final FloatProcessor fp = new FloatProcessor( pixels );				
			stack.addSlice( fp );
		}

		return new ImagePlus( path, stack );		
	}

	static ImagePlus openNewFormat(
			String path, 
			DataInputStream dis ) throws IOException
	{
		//ImagePlus imp;
		// New version of VM using 12 parameter fields
		int version = reverse( dis.readInt() );
		int type = reverse( dis.readInt() );
		int size1 = reverse(dis.readInt());
		int size2 = reverse(dis.readInt());
		int size3 = reverse(dis.readInt());
		ImageStack stack = new ImageStack( size1, size2 );

		int voxelUnit = reverse(dis.readInt());
		float voxelWidth = Float.intBitsToFloat( reverse(dis.readInt()) );
		float voxelHeight = Float.intBitsToFloat( reverse(dis.readInt()) );
		float voxelDepth = Float.intBitsToFloat( reverse(dis.readInt()) );

		final int numPixels = size1 * size2;
		final int bufferSize = 4 * numPixels;

		final byte[] buffer = new byte[bufferSize];

		// write pixels
		for (int z=0; z < size3; ++z) 
		{

			final float[][] pixels = new float[ size1 ][ size2 ];

			int n = dis.read( buffer, 0, bufferSize );

			for(int j = 0; j < bufferSize; j+=4)
			{
				int tmp = (int)(((buffer[j+3]&0xff)<<24) | ((buffer[j+2]&0xff)<<16) | ((buffer[j+1]&0xff)<<8) | (buffer[j]&0xff));

				int currentPos = j / 4;
				int y = currentPos / size2;
				int x = currentPos % size2;

				if ( type == 5 ) pixels[ y ][ x ] =  Float.intBitsToFloat(tmp); //float type
				else if ( type == 2 ) pixels[ y ][ x ] = (float) tmp; //int type

			}
			final FloatProcessor fp = new FloatProcessor( pixels );
			stack.addSlice( fp );
		}


		ImagePlus imp = new ImagePlus( path, stack );

		if (size3>1) {
			imp.setSlice( size3/2);
			ImageProcessor ip = imp.getProcessor();
			ip.resetMinAndMax();
			imp.setDisplayRange(ip.getMin(),ip.getMax());
		}

		Calibration calibration = new Calibration();
		String unit = intToUnitString( voxelUnit );
		calibration.setXUnit( unit );
		calibration.setYUnit( unit );
		calibration.setZUnit( unit );
		calibration.pixelWidth = voxelWidth;
		calibration.pixelHeight = voxelHeight;
		calibration.pixelDepth = voxelDepth;
		imp.setCalibration( calibration );

		return imp;
	}

	private final static String intToUnitString(int i)
	{
		switch( i )
		{
		case 0: return "m";
		case -2: return "cm";
		case -3: return "mm";
		case -6: return "µm";
		case -9: return "nm";
		}
		return "(unknown)";
	}

	
	public static boolean write( 
			ImagePlus imp, 
			String path ) 
	{		
		if( null == imp )
			return false;
		
		ImageStack stack = imp.getStack();		
		
		float pixelWidth = (float)imp.getCalibration().pixelWidth;
		float pixelHeight = (float)imp.getCalibration().pixelHeight;
		float pixelDepth = (float)imp.getCalibration().pixelDepth;
		String unitString = imp.getCalibration().getXUnit();
			
		int size1 = stack.getWidth();
		int size2 = stack.getHeight();
		int size3 = stack.getSize();
		
		IJ.log("/"+size1+"/");
		
		File outputFile = new File( path );

		try 
		{
			// Construct the BufferedOutputStream object
			BufferedOutputStream bufferedOutput = new BufferedOutputStream(new FileOutputStream(outputFile));
			DataOutputStream dataOut = new DataOutputStream(bufferedOutput);

			// Start writing to the output stream

			int zero = 0;
			dataOut.writeInt(zero);
			dataOut.writeInt(zero);
			dataOut.writeInt(zero);

			int version = 1;
			dataOut.writeInt(reverse(version));

			// type of data
			// type= 2 - int
			// type= 5 - float
			int type = 5;
			dataOut.writeInt( reverse(type) );
			
	            	dataOut.writeInt( reverse(size1) );	
			dataOut.writeInt( reverse(size2) );
			dataOut.writeInt( reverse(size3) );

			// Spatial calibration
			dataOut.writeInt( reverse(unitStringToInt(unitString)) );
			dataOut.writeInt( reverse(Float.floatToIntBits(pixelWidth)) );
			dataOut.writeInt( reverse(Float.floatToIntBits(pixelHeight)) );
			dataOut.writeInt( reverse(Float.floatToIntBits(pixelDepth)) );

			// write pixels
			for (int z=0; z < size3; ++z) {	
				for (int x=0; x < size1; ++x) {		
					for (int y=0; y < size2; ++y) {
						dataOut.writeInt(reverse(Float.floatToIntBits((float)stack.getVoxel(x,y,z))));
					}
				}
			}

			// cleanup output stream
			dataOut.flush();
			dataOut.close();

		}

		catch (Exception e) 
		{
		    e.printStackTrace();
		    return false;
		} 
		return true;
	}
		
	// process to change values to little-endian	
	public final static int reverse(int i) 
	{
		return Integer.reverseBytes(i);
	}

	private final static int unitStringToInt(String s)
	{
		IJ.log("/"+s+"/");
		if ( s.equals("cm") ) return -2;
		if ( s.equals("mm") ) return -3;
		if ( s.equals("µm") || s.equals("um") ) return -6;
		if ( s.equals("nm") ) return -9;
		return 0;
	}

	/**
	 * Convert file to VoxelMatrix
	 * @param inputPath input file name with complete path
	 * @param outputPath output file name with complete path
	 * @return false if error
	 */
	public static boolean convert( String inputPath, String outputPath )
	{
		final ImagePlus input = new ImagePlus( inputPath );
		return VoxelMatrixIO.write( input, outputPath );
	}
}
