package inra.ijpb.voxelmatrix;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.StackWindow;
import ij.plugin.PlugIn;
import fiji.util.gui.GenericDialogPlus;

public class Segmentation_Checker implements PlugIn 
{		
	/** original directory **/
	public static String originalFolder="";
	/** segmented directory **/
	public static String segmentedFolder="";
	
	VoxelMatrix_Reader reader = new VoxelMatrix_Reader();
	
	/** current image counter */
	int counter = -1;
	
	/** current image name */
	String currentImageName = null;
	
	File[] originalFilesList = null;
	File[] segmentedFilesList = null;
	ArrayList<String> discardedFilesList = null;
	
	/** main GUI window */
	private CustomWindow win;

	// Button panel components
	JButton validateButton = null;
	JButton discardButton = null;
	JButton finishButton = null;
	JButton exitButton = null;	
	
	JPanel buttonsPanel = new JPanel();
	
	/** main panel */
	JPanel all = new JPanel();
	
	/**
	 * Custom window to define the plugin GUI
	 */
	private class CustomWindow extends StackWindow
	{

		public CustomWindow( ImagePlus imp ) 
		{
		
			super(imp, new ImageCanvas(imp));

			final ImageCanvas canvas = (ImageCanvas) getCanvas();

			// Zoom in if image is too small
			while(ic.getWidth() < 512 && ic.getHeight() < 512)
				IJ.run( imp, "In","" );

			setTitle( "Segmentation Checker" );

			validateButton = new JButton( "Validate" );
			discardButton = new JButton( "Discard" );
			finishButton = new JButton( "Finish it" );
			exitButton = new JButton( "Exit" );
			
			// Options panel (left side of the GUI)
			buttonsPanel.setBorder( BorderFactory.createTitledBorder( "Options" ) );
			GridBagLayout buttonsLayout = new GridBagLayout();
			GridBagConstraints buttonsConstraints = new GridBagConstraints();
			buttonsConstraints.anchor = GridBagConstraints.NORTHWEST;
			buttonsConstraints.fill = GridBagConstraints.HORIZONTAL;
			buttonsConstraints.gridwidth = 1;
			buttonsConstraints.gridheight = 1;
			buttonsConstraints.gridx = 0;
			buttonsConstraints.gridy = 0;
			buttonsConstraints.insets = new Insets(5, 5, 6, 6);
			buttonsPanel.setLayout( buttonsLayout );

			buttonsPanel.add( validateButton, buttonsConstraints );
			buttonsConstraints.gridy++;
			buttonsPanel.add( discardButton, buttonsConstraints );
			buttonsConstraints.gridy++;
			buttonsPanel.add( finishButton, buttonsConstraints);
			buttonsConstraints.gridy++;
			buttonsPanel.add( exitButton, buttonsConstraints );
			buttonsConstraints.gridy++;
			
			GridBagLayout layout = new GridBagLayout();
			GridBagConstraints allConstraints = new GridBagConstraints();
			all.setLayout(layout);

			allConstraints.anchor = GridBagConstraints.NORTHWEST;
			allConstraints.fill = GridBagConstraints.BOTH;
			allConstraints.gridwidth = 1;
			allConstraints.gridheight = 2;
			allConstraints.gridx = 0;
			allConstraints.gridy = 0;
			allConstraints.weightx = 0;
			allConstraints.weighty = 0;

			all.add(buttonsPanel, allConstraints);
			
			allConstraints.gridx++;
			allConstraints.weightx = 1;
			allConstraints.weighty = 1;
			allConstraints.gridheight = 1;
			all.add(canvas, allConstraints);
			
			allConstraints.gridy++;
			allConstraints.weightx = 0;
			allConstraints.weighty = 0;
			// if the input image is 3d, put the
			// slice selectors in place
			if( null != super.sliceSelector )
			{
				all.add( super.sliceSelector, allConstraints );

				if( null != super.zSelector )
					all.add( super.zSelector, allConstraints );
				if( null != super.tSelector )
					all.add( super.tSelector, allConstraints );
				if( null != super.cSelector )
					all.add( super.cSelector, allConstraints );
			}
			allConstraints.gridy--;

			GridBagLayout wingb = new GridBagLayout();
			GridBagConstraints winc = new GridBagConstraints();
			winc.anchor = GridBagConstraints.NORTHWEST;
			winc.fill = GridBagConstraints.BOTH;
			winc.weightx = 1;
			winc.weighty = 1;
			setLayout(wingb);
			add(all, winc);
			
			// Fix minimum size to the preferred size at this point
			pack();
			setMinimumSize( getPreferredSize() );
			
		}

		/**
		 * Generated UID
		 */
		private static final long serialVersionUID = -7767481682958951196L;
		
		
		
	}

	//---------------------------------------------------------------------------------
	/**
	 * Plug-in run method
	 * 
	 * @param arg plug-in arguments
	 */
	public void run(String arg) 
	{
		GenericDialogPlus gd = new GenericDialogPlus("Segmentation Checker");

		gd.addDirectoryField("Original folder", originalFolder, 50);
		gd.addDirectoryField("Segmented folder", segmentedFolder, 50);
		gd.showDialog();
		
		// Exit when canceled
		if (gd.wasCanceled()) 
			return;
		
		originalFolder = gd.getNextString();
		segmentedFolder = gd.getNextString();


		String originalDir = originalFolder;
		if (null == originalDir) 
			return;
		originalDir = originalDir.replace('\\', '/');
		if (!originalDir.endsWith("/")) originalDir += "/";
		

		String segmentedDir = segmentedFolder;
		if (null == segmentedDir) 
			return;
		segmentedDir = segmentedDir.replace('\\', '/');
		if (!segmentedDir.endsWith("/")) segmentedDir += "/";		

		// Find first original image with corresponding segmentation
		
		// initialize list of discarded files
		discardedFilesList = new ArrayList<String>(); 
		
		originalFilesList = new File( originalFolder ).listFiles();
		segmentedFilesList = new File( segmentedFolder ).listFiles();
		// sort file names
		Arrays.sort( segmentedFilesList );
		
		String currentImageName = getNextImageName();
		
		if( null == currentImageName )
			return;
						
		try{
			// open first image (check if it is VoxelMatrix first)
			final ImagePlus firstImage = currentImageName.endsWith( ".vm" ) ?
				reader.readIt( originalFilesList[counter].getParent().toString() + "/" + currentImageName ) :
				new ImagePlus( originalFilesList[counter].getParent().toString() + "/" + currentImageName );
			// Build GUI
			SwingUtilities.invokeLater(
					new Runnable() {
						public void run() {
							win = new CustomWindow( firstImage );
							win.pack();
						}
					});
		}catch( Exception ex ){
			IJ.error("Could not load " + originalFilesList[counter].getParent().toString() + "/" + currentImageName);
			ex.printStackTrace();
		}
		


	}
	
	/**
	 * Get next image name to treat (existing original and segmented images)
	 * NOTE: it increases the counter until it finds a suitable image name.
	 * 
	 * @return name of next image with both original and segmented image
	 */
	String getNextImageName()
	{
		counter++;
		
		if( counter >= segmentedFilesList.length ) 
				return null;
		
		String currentSegmentedImageName = segmentedFilesList[counter].getName();
		
		// look for corresponding original image
		boolean found = false;
		while( found == false && counter >= segmentedFilesList.length)
		{
			for(int i=1; i<originalFilesList.length; i ++)
				if( currentSegmentedImageName.equals( originalFilesList[ i ].getName() ) )
				{
					found = true;
					break;
				}
			counter ++;	
			currentSegmentedImageName = segmentedFilesList[counter].getName();
		}
		
		if( counter >= segmentedFilesList.length ) 
			return null;
		
		IJ.log( (counter+1)+"/"+segmentedFilesList.length + "--" + currentSegmentedImageName );
		
		return currentSegmentedImageName;
	}
	

}
