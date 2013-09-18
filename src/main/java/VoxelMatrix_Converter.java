import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.plugin.PlugIn;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
//import ij.measure.Calibration;
//import java.nio.*;

public class VoxelMatrix_Converter implements PlugIn{ 

	protected ImagePlus imp;
	protected ImageStack stack;

	// image property members
	private String unitString;
	private float pixelWidth, pixelHeight, pixelDepth;

	private String filename;
	
	public void run(String arg) 
	{
		//
	
		// get the file
		String path = arg;
		String directory = null;
		String filename = null;
		if (null == path || 0 == path.length()) {
			OpenDialog od = new OpenDialog("Choose an image file to convert...", null);
			directory = od.getDirectory();
			if (null == directory) return;
			filename = od.getFileName();
			path = directory + "/" + filename;
		} else {
			// the argument is the path
			File fileIn = new File(path);
			directory = fileIn.getParent(); // could be a URL
			filename = fileIn.getName();
			//if (directory.startsWith("http:/")) directory = "http://" + directory.substring(6); // the double '//' has been eliminated by the File object call to getParent()
		}

		// Open dialog to save image
		SaveDialog dlg = new SaveDialog("Choose the folder within to save the VoxelMatrix file...", filename, ".vm");

		// extract output file name
		String directoryOut = dlg.getDirectory();
		if (null == directoryOut) return;
		String fileNameOut = dlg.getFileName();
				
		imp = save_Image_To_VM( path, directoryOut, fileNameOut );
		if( null != imp )
		{
			imp.setTitle(filename);
			imp.show();
		}
	}
	
	public ImagePlus save_Image_To_VM( String inputFile, String directoryOut, String fileNameOut ) 
	{
		// Open the image	
		boolean needToShow = false;
		IJ.run("Open...", "open="+inputFile);
		ImagePlus imp = IJ.getImage();		
		//imp.hide();
		String basename = imp.getShortTitle();

		pixelWidth = (float)imp.getCalibration().pixelWidth;
		pixelHeight = (float)imp.getCalibration().pixelHeight;
		pixelDepth = (float)imp.getCalibration().pixelDepth;
		unitString = imp.getCalibration().getXUnit();
		stack = imp.getStack();

		int size1 = stack.getWidth();
		int size2 = stack.getHeight();
		int size3 = stack.getSize();
		
		File outputFile = null;
		
		if ( fileNameOut.equals("null") ) {
			outputFile = new File(directoryOut, (basename+".vm") );
		}
		else 
		{
			outputFile = new File(directoryOut, fileNameOut);
		}
		
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
		} 
				
		finally 
		{
			return imp;
		} 


	}

	// process to change values to little-endian	
	public final static int reverse(int i) 
	{
		return Integer.reverseBytes(i);
	}

	private final static int unitStringToInt(String s)
	{
		//IJ.log("/"+s+"/");
		if ( s.equals("cm") ) return -2;
		if ( s.equals("mm") ) return -3;
		if ( s.equals("µm") || s.equals("um") ) return -6;
		if ( s.equals("nm") ) return -9;
		return 0;
	}
}
