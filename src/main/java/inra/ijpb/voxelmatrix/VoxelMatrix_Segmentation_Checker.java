package inra.ijpb.voxelmatrix;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.plugin.frame.PlugInFrame;

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFileChooser;

public class VoxelMatrix_Segmentation_Checker implements PlugIn{ 

	public void run(String arg) 
	{

		//get the folder of the original images
		JFileChooser of = new JFileChooser();
		//JFileChooser of = new JFileChooser("/home/javier/data/projects/");
		of.setDialogTitle("Select the folder of your original images...");
		//oc.setFileSelectionMode(JFileChooser.FILES_ONLY);   // default of class
   		of.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int retval = of.showOpenDialog(null);

		//get the folder of the nucleus images
		JFileChooser nf = new JFileChooser();
		//JFileChooser nf = new JFileChooser("/home/javier/data/projects/");
		nf.setDialogTitle("Select the folder of your nuclei images...");
		nf.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int retval2 = nf.showOpenDialog(null);

		//process both directories
		if (retval == JFileChooser.APPROVE_OPTION && retval2 == JFileChooser.APPROVE_OPTION) {
			String originalFolder = of.getSelectedFile().getAbsolutePath();		   
			String nucleusFolder = nf.getSelectedFile().getAbsolutePath();

			try {
				new VMSegmentationChecker(originalFolder, nucleusFolder);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else return;
	}
}

class VMSegmentationChecker extends PlugInFrame implements ActionListener{

	String originalFolder = null;
	String nucleusFolder = null;

	File[] originalFilesList = null;
	File[] nucleusFilesList = null;
	ArrayList<String> discardedFilesList = new ArrayList();

	int counter = 0;
	//int counterDiscarded = 0;
	ImagePlus currentImage = null;

	Panel panel = null;

	public VMSegmentationChecker(
		String originalFolder, 
		String nucleusFolder) throws IOException
	{
		super("Segmentation checker");
		this.originalFolder = originalFolder;
		this.nucleusFolder = nucleusFolder;

		originalFilesList = new File( originalFolder ).listFiles();
		nucleusFilesList = new File( nucleusFolder ).listFiles();

		showNextImage(); 

		setLayout(new FlowLayout(FlowLayout.CENTER,1,3));        
		panel = new Panel();        
		panel.setLayout(new GridLayout(4,1,5,5));        
		
		Button bGood = new Button( "Validate & Next" );  
		bGood.addActionListener( this );
		panel.add( bGood );

		Button bBad = new Button( "Discard & Next" );  
		bBad.addActionListener( this );
		panel.add( bBad );	

		Button bClose = new Button( "Finish" );  
		bClose.addActionListener( this );
		panel.add( bClose );
		
		Button bExit = new Button( "Exit" );  
		bClose.addActionListener( this );
		panel.add( bExit );
		add( panel );

		pack();
		show();
	}

	public void actionPerformed( ActionEvent e )
	{
		String label = e.getActionCommand();
		if ( null == label )
			return;
		if ( label.equals( "Validate & Next" ) )
			try {
				showNextImage();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} 
		if ( label.equals( "Discard & Next" ) )
			try {
				discardImage();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} 			
		if ( label.equals( "Finish" ) )
			finishProcess(); //final step to close
		if ( label.equals( "Exit" ) )
			return; //exit
		
	}

	void showNextImage() throws IOException
	{ 

		if( null != currentImage )
			IJ.run("Close All");
				
		if( counter >= originalFilesList.length ) 
			finishProcess();
					
		String hasSameName = originalFilesList[counter].getName().toString();
		if ( hasSameName.equals(nucleusFilesList[counter].getName().toString()) ) {

			IJ.log( (counter+1)+"/"+originalFilesList.length + "--" + originalFilesList[counter].getName().toString() );

			String originalPath = originalFilesList[counter].toString();

			ImagePlus originalIMP = VoxelMatrixIO.read( originalPath );

			originalIMP.setTitle("Original_Image");
			IJ.run(originalIMP, "Enhance Contrast...", "saturated=0.4 normalize process_all use");

			String nucleusPath = nucleusFilesList[counter].toString();

			ImagePlus nucleusIMP = VoxelMatrixIO.read( nucleusPath );

			nucleusIMP.setTitle("Segmented_Image");	
			IJ.run("Merge Channels...", " c1=Segmented_Image c2=Original_Image keep");

			nucleusIMP.close();
			originalIMP.close();

			IJ.selectWindow("RGB");
			currentImage = IJ.getImage();
			//currentImage.setTitle(originalIMP.getName());
			IJ.run("Orthogonal Views");

			counter ++;

		}
		
	}
	
	void discardImage() throws IOException
	{
		discardedFilesList.add( nucleusFilesList[counter-1].getPath().toString() );
		IJ.log("nuclei "+discardedFilesList.size()+"/"+counter);
				
		if( counter < originalFilesList.length )
		{	
			//IJ.log(nucleusFilesList[counter].getPath().toString());
			//IJ.log("disccounter"+ Integer.toString(discardedFilesList.size()) );
			showNextImage();
		}
		else
		{
			//discardedFilesList.add( nucleusFilesList[counter-1].getPath().toString() );	
			IJ.run("Close All");
			finishProcess();
		}
		
	}

	void finishProcess()
	{
		
		String discardedFolder = nucleusFilesList[0].getParentFile().getParentFile().toString()+ "/discardedNucleus";
		IJ.log(discardedFolder);
		new File(discardedFolder).mkdirs();
		float success = (discardedFilesList.size()*100/(counter+1)) ; 
		IJ.log( success + "% of files discarded:" );
		
		for ( int i=0; i<discardedFilesList.size(); ++i )
		{
			File discardedFile = new File( discardedFilesList.get(i).toString() );
			discardedFile.renameTo( new File(discardedFolder +"/"+ discardedFile.getName()) );
			IJ.log( discardedFile.getName().toString() );
			//IJ.log( "numdiscarded"+ Integer.toString(discardedFilesList.size()) );
		}

		return;
		
	}
	
	
}
