package inra.ijpb.voxelmatrix;
import ij.plugin.PlugIn;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.JFileChooser;

import ij.ImagePlus;

/*///////////////////////////////////////
This plugin opens vm files found into a folder.
To change this to be able to open multiple files, ""of.setFileSelectionMode(JFileChooser.FILES_ONLY)""; has to be commented out
///////////////////////////////////////*/


public class VoxelMatrix_Batch_Reader implements PlugIn{ 
	
	public static ImagePlus imp;
	
	public void run(String arg) 
	{
		String inputFolder = null;

		//get the folder of the original images
		JFileChooser of = new JFileChooser();
		of.setDialogTitle("Select the folder of .vm files you want to open...");
		//of.setFileSelectionMode(JFileChooser.FILES_ONLY);   // default of class
   		of.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int retval = of.showOpenDialog(null);

		//process both directories
		if ( retval == JFileChooser.APPROVE_OPTION ) {
			//File nFolder = nf.getCurrentDirectory();//it's not necessary
			inputFolder = of.getSelectedFile().getAbsolutePath();		   
			//IJ.log(inputFolder);
			// DO YOUR PROCESSING HERE. OPEN FILE OR ...

			listRecursive( inputFolder );
		}
		else return;
	}
	   
	public static <ImagePlus> void listRecursive( String inputDir ) 
	{
		File[] filesList = new File(inputDir).listFiles();
		String[] originalList = new File(inputDir).list();
		//try this
		Arrays.sort(filesList);
		//File[] nucleusList = new File(nucleusDir).listFiles(); //only is necessary the original number

		int totalImages = (int)originalList.length;
		for (int i=0; i < totalImages; ++i) 
		{
			String path = filesList[i].toString();	    		    			
			imp = VoxelMatrixIO.read( path );
			if( null != imp )
				imp.show();				    			
		}
 	}
 	
}
