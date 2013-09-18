import ij.ImagePlus;
import ij.plugin.PlugIn;

import java.io.File;

import javax.swing.JFileChooser;
//import ij.io.*;
//import ij.IJ;
//import ij.io.File;
//import ij.*;
//import ij.gui.*;
//import ij.process.*;

/*///////////////////////////////////////
This plugin opens vm files found into a folder.
To change this to be able to open multiple files, ""of.setFileSelectionMode(JFileChooser.FILES_ONLY)""; has to be commented out
///////////////////////////////////////*/

public class VoxelMatrix_Batch_Converter implements PlugIn{ 
	
	public void run(String arg) 
	{
		//get the folder of the original images
		JFileChooser iF = new JFileChooser();
		iF.setDialogTitle("Select the folder of .vm files you want to open...");
		//of.setFileSelectionMode(JFileChooser.FILES_ONLY);   // default of class
   		iF.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
   		//iF.setMultiSelectionEnabled(true); %% this lets to select more than 1 file??? must change the setFileSelectionMode too
		int retval = iF.showOpenDialog(null);
		String iFolder = iF.getSelectedFile().getAbsolutePath();

		//select the folder to save the new VM images
		JFileChooser oF = new JFileChooser(iFolder);
		oF.changeToParentDirectory();
		oF.setDialogTitle("Select the folder of .vm files you want to open...");
		//of.setFileSelectionMode(JFileChooser.FILES_ONLY);   // default of class
   		oF.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int retval2 = oF.showOpenDialog(null);

		//process both directories
		if ( retval == JFileChooser.APPROVE_OPTION && retval2 == JFileChooser.APPROVE_OPTION ) {
			//File nFolder = nf.getCurrentDirectory();//it's not necessary
			String oFolder = oF.getSelectedFile().getAbsolutePath();		   

			listRecursive( iFolder, oFolder );
		}
		else return;
	}
	   
	public static void listRecursive( String inputDir, String outputDir ) 
	{
		File[] filesList = new File(inputDir).listFiles();
		@SuppressWarnings("unused")
		ImagePlus imp = new ImagePlus();
		int totalImages = filesList.length;
		for (int i=0; i < totalImages; ++i) 
		{
			String pathFile = filesList[i].toString();
	    		VoxelMatrix_Converter voxelWriter = new VoxelMatrix_Converter();
    			imp = voxelWriter.save_Image_To_VM( pathFile, outputDir, "null" );
		}
 	}
 	
}
