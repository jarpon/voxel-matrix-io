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
	private int size1, size2, size3;
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
			imp = read_it(path);
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
	public ImagePlus read_it(String path) throws IOException  
	{		if (null == path) return null;	
		
		ImagePlus imp = new ImagePlus();
		
		try 
		{
			//create file input & data input stream
			FileInputStream fis = new FileInputStream( path );
			DataInputStream dis = new DataInputStream( fis );
			//BufferedInputStream bufferedInput = new BufferedInputStream(new FileInputStream(path));
			//DataInputStream dis = new DataInputStream(bufferedInput);
			int zero1, zero2, zero3;
			zero1 = reverse( dis.readInt() );//0
			zero2 = reverse( dis.readInt() );//0
			zero3 = reverse( dis.readInt() );//0
	
			// Previous versions of VM files used 3 values
			if ( zero1 > 0 && zero2 > 0 && zero3 > 0 ) 
			{
				dis.close();
				return null;
			}
			
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
					//int tmp = (int)(((buffer[j]&0xff)<<24) | ((buffer[j+1]&0xff)<<16) | ((buffer[j+2]&0xff)<<8) | (buffer[j+3]&0xff));
					//int tmp = (int)(((buffer[j+3])<<24) | ((buffer[j+2])<<16) | ((buffer[j+1])<<8) | (buffer[j]));
					
					int currentPos = j / 4;
					
					int y = currentPos / size2;
					int x = currentPos % size2;
					
					pixels[ y ][ x ] =  Float.intBitsToFloat(tmp);			
					//pixels[ y ][ x ] = (float)(tmp&0xffffffffL);
				}
				final FloatProcessor fp = new FloatProcessor( pixels );				
				stack.addSlice( fp );
			}
			fis.close();
			dis.close();
			
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
			
			dis.close();
			//bufferedInput.close();
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
