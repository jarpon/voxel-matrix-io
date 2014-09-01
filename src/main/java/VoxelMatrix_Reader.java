import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.OpenDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.imageio.IIOException;
@SuppressWarnings("unused")
public class VoxelMatrix_Reader implements PlugIn
{ 
	public ImagePlus imp;
	protected ImageStack stack;

	// image property members

	private int version;
	private int type;
	private int size1, size2, size3, zero1, zero2, zero3;
	private float voxelWidth, voxelHeight, voxelDepth;
	private int voxelUnit;
	private String filename;
	
	public void run(String arg) 
	{
		//boolean needToShow = false;
 	
		// get the file
		String path = arg;
		String directory = null;
		if (null == path || 0 == path.length()) 
		{
			OpenDialog od = new OpenDialog("Choose .vm file", null, "*.vm");
			directory = od.getDirectory();
			if (null == directory) return;
			filename = od.getFileName();
			path = directory + "/" + filename;
			//File fileIn = new File(path);
		} 
		else 
		{
			// the argument is the path
			File fileIn = new File(path);
			directory = fileIn.getParent(); // could be a URL
			filename = fileIn.getName();
			//if (directory.startsWith("http:/")) directory = "http://" + directory.substring(6); // the double '//' has been eliminated by the File object call to getParent()
		}

		try 
		{
			imp = readIt(path);
			if( null != imp )
			{
				imp.setTitle(filename);
				imp.show();
			}
		} 
		
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

		
	@SuppressWarnings("finally")
	public ImagePlus readIt(String path) throws IOException  
	{		if (null == path) return null;	
		
		ImagePlus imp = new ImagePlus();
		
		try 
		{
			//create file input & data input stream
			FileInputStream fis = new FileInputStream( path );
			DataInputStream dis = new DataInputStream( fis );
			//BufferedInputStream bufferedInput = new BufferedInputStream(new FileInputStream(path));
			//DataInputStream dis = new DataInputStream(bufferedInput);
			int size1, size2, size3;
			size1 = reverse( dis.readInt() );//0
			size2 = reverse( dis.readInt() );//0
			size3 = reverse( dis.readInt() );//0
			// Distinguish between versions: previous versions of VM files used 3 values
			if ( size1 > 0 && size2 > 0 && size3 > 0 ) 
			{
				//old version type = int;
				imp = readOldFormat( path, dis, size1, size2 );
				fis.close();
				dis.close();
				if (size3>1) {
		            imp.setSlice( size3/2);
		            ImageProcessor ip = imp.getProcessor();
		            ip.resetMinAndMax();
		            imp.setDisplayRange(ip.getMin(),ip.getMax());
		        }

				//return imp;

			}

			else 
			{
				imp = openNewFormat(path, dis);
				fis.close();
				dis.close();
				//bufferedInput.close();
			}
		}
		
		catch (Exception e) 
		{
		    e.printStackTrace();
		} 
				
		finally 
		{
			return imp;
		} 

	}

	public static int reverse(int i) 
	{
		return Integer.reverseBytes(i);
	}

	private ImagePlus openNewFormat(String path, DataInputStream dis) throws IOException
	{
		//ImagePlus imp;
		// New version of VM using 12 parameter fields
		version = reverse( dis.readInt() );
		type = reverse( dis.readInt() );
		size1 = reverse(dis.readInt());
		size2 = reverse(dis.readInt());
		size3 = reverse(dis.readInt());
		ImageStack stack = new ImageStack( size1, size2 );

		voxelUnit = reverse(dis.readInt());
		voxelWidth = Float.intBitsToFloat( reverse(dis.readInt()) );
		voxelHeight = Float.intBitsToFloat( reverse(dis.readInt()) );
		voxelDepth = Float.intBitsToFloat( reverse(dis.readInt()) );

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


		imp = new ImagePlus( path, stack );

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
	public ImagePlus readOldFormat(String path, DataInputStream dis, int size1, int size2) throws IOException
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

		imp = new ImagePlus( path, stack );

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
	
}
