package inra.ijpb.voxelmatrix;
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

@SuppressWarnings("unused")
public class VoxelMatrix_Converter implements PlugIn{ 

	protected ImagePlus imp;
	protected ImageStack stack;

	// image property members
	private String unitString;
	private float pixelWidth, pixelHeight, pixelDepth;

	private String filename;
	
	public void run(String arg) 
	{
		// get the file
		String path = arg;
		String directory = null;
		String filename = null;
		if (null == path || 0 == path.length()) {
			OpenDialog od = new OpenDialog("Choose an image file to convert...", null);
			directory = od.getDirectory();
			if (null == directory) return;
			filename = od.getFileName();			
		} else {
			// the argument is the path
			File fileIn = new File(path);
			directory = fileIn.getParent(); // could be a URL
			filename = fileIn.getName();
		}

		path = directory + filename;
		ImagePlus image = IJ.openImage(path);
		
		// Open dialog to save image
		SaveDialog dlg = new SaveDialog("Choose the folder where you want to save the VoxelMatrix file...", filename, ".vm");

		// extract output file name
		String directoryOut = dlg.getDirectory();
		if (null == directoryOut) return;
		String fileNameOut = dlg.getFileName();
					
		if( VoxelMatrixIO.write( image, directoryOut + "/" + fileNameOut ) == false )
			IJ.error( "Could not save image as VoxelMatrix!" );
	}
	
}
