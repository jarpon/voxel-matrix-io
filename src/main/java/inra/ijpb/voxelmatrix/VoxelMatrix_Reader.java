package inra.ijpb.voxelmatrix;
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

		imp = VoxelMatrixIO.read( path );
		if( null != imp )
		{
			imp.setTitle(filename);
			imp.show();
		}

	}
			
}
