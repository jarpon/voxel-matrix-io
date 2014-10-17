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

	@Override
	public void run(String arg) {

		// Extract current image with meta-data
		imp = IJ.getImage();

		// Open dialog to save image
		//String baseName = imp.getShortTitle();
		String baseName = imp.getTitle();
		SaveDialog dlg = new SaveDialog("Choose VoxelMatrix file", baseName, ".vm");
		IJ.log("/"+baseName+"/");

		// extract output file name
		String directory = dlg.getDirectory();
		if (null == directory) return;
		String fileName = dlg.getFileName();

		// Save stack
		VoxelMatrixIO.write( imp, directory + fileName );

	}
	
	
}
