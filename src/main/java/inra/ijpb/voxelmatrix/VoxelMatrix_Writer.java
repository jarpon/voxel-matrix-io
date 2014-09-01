package inra.ijpb.voxelmatrix;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
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

public class VoxelMatrix_Writer implements PlugIn { 
	
//	VoxelMatrix_Writer(){}
	protected ImagePlus imp;
	protected ImageStack stack;

	// image property members

	private String unitString;
	private float pixelWidth;
	private float pixelHeight;
	private float pixelDepth;

//	private String filename;
	@Override
	public void run(String arg) {

		// Extract current image with meta-data
		imp = IJ.getImage();
		pixelWidth = (float)imp.getCalibration().pixelWidth;
		pixelHeight = (float)imp.getCalibration().pixelHeight;
		pixelDepth = (float)imp.getCalibration().pixelDepth;
		unitString = imp.getCalibration().getXUnit();

		// Open dialog to save image
		//String baseName = imp.getShortTitle();
		String baseName = imp.getTitle();
		SaveDialog dlg = new SaveDialog("Choose VoxelMatrix file", baseName, ".vm");
		IJ.log("/"+baseName+"/");

		// extract output file name
		String directory = dlg.getDirectory();
		if (null == directory) return;
		String fileName = dlg.getFileName();
		File outputFile = new File(directory, fileName);

		// Save stack
		saveStack(outputFile);

	}
	
	public void saveStack(File outputFile) 
	{

		
		stack = imp.getStack();

		int size1 = stack.getWidth();
		int size2 = stack.getHeight();
		int size3 = stack.getSize();
		
		IJ.log("/"+size1+"/");

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
}
