package inra.ijpb.voxelmatrix;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageRoi;
import ij.gui.Overlay;
import ij.gui.StackWindow;
import ij.plugin.PlugIn;
import ij.process.LUT;
import fiji.util.gui.GenericDialogPlus;

public class Segmentation_Checker implements PlugIn 
{		
	/** original directory **/
	public static String originalFolder="";
	/** segmented directory **/
	public static String segmentedFolder="";
	
	/** current image counter */
	int counter = -1;
	
	/** current image name */
	String currentImageName = null;
	
	/** current original image */
	ImagePlus originalImage = null;
	/** current segmented image */
	ImagePlus segmentedImage = null;
	
	/** current transparency */
	Float transparency = 0.33f;
	
	/** overlay LUT */
	LUT overlayLUT = Segmentation_Checker.createGoldenAngleLut();
	
	File[] originalFilesList = null;
	File[] segmentedFilesList = null;
	ArrayList<String> discardedFilesList = null;
	
	/** main GUI window */
	private CustomWindow win;
	private ImageCanvas canvas;

	/** executor service to launch threads for the plugin methods and events */
	final ExecutorService exec = Executors.newFixedThreadPool(1);

	// Button panel components
	JButton validateButton = null;
	JButton discardButton = null;
	JButton finishButton = null;
	JButton exitButton = null;	
	
	JSlider transparencySlider = null;
	
	JPanel buttonsPanel = new JPanel();
	
	/** main panel */
	Panel all = new Panel();
	
	/**
	 * Button listener
	 */
	private ActionListener listener = new ActionListener() {

		
	
		public void actionPerformed(final ActionEvent e) {

			
			// listen to the buttons on separate threads not to block
			// the event dispatch thread
			exec.submit(new Runnable() {
												
				public void run()
				{
					if(e.getSource() == validateButton)
						nextImage();
					else if( e.getSource() == discardButton )
						discardImage();
					else if( e.getSource() == finishButton )
						finishProcess();
					else if( e.getSource() == exitButton )
						IJ.run("Close All");
				}

				
			});
		}
	};
	
	
	/**
	 * Custom window to define the plugin GUI
	 */
	/**
	 * Custom window to define the plugin GUI
	 */
	private class CustomWindow extends StackWindow
	{
		/**
		 * Generated UID
		 */
		private static final long serialVersionUID = -7767481682958951196L;

		public CustomWindow( ImagePlus imp ) 
		{
		
			super(imp, new ImageCanvas(imp));
			
			// assign original image
			originalImage = imp;
			
			
			//segmentedImage.show();
			
			canvas = (ImageCanvas) getCanvas();

			// Zoom in if image is too small
			while(ic.getWidth() < 512 && ic.getHeight() < 512)
				IJ.run( imp, "In","" );

			setTitle( "Segmentation Checker" );

			// create buttons and add listener
			validateButton = new JButton( "Validate" );
			validateButton.addActionListener(listener);
			
			discardButton = new JButton( "Discard" );
			discardButton.addActionListener(listener);
			
			finishButton = new JButton( "Finish it" );
			finishButton.addActionListener(listener);
			
			exitButton = new JButton( "Exit" );
			exitButton.addActionListener(listener);
			exitButton.setToolTipText( "Exit plugin without changes" );
			
			// create transparency slider
			JLabel sliderLabel = new JLabel( "Transparency", JLabel.CENTER );
			transparencySlider = new JSlider( 0, 100 );
			transparencySlider.setValue( (int) (100 * transparency) );
			transparencySlider.setMajorTickSpacing( 20 );
			transparencySlider.setMinorTickSpacing( 5 );
			transparencySlider.setPaintLabels(true);
			transparencySlider.setName( "Transparency" );
			transparencySlider.setPaintTicks( true );
			transparencySlider.addChangeListener( new ChangeListener() {
				
				@Override
				public void stateChanged(ChangeEvent e) {
					exec.submit(new Runnable() {
						public void run() {							
							transparency = transparencySlider.getValue() / 100f;
							updateOverlay();
						}							
					});
					
				}
			});
			
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
			buttonsPanel.add( sliderLabel, buttonsConstraints );
			buttonsConstraints.gridy++;
			buttonsPanel.add( transparencySlider, buttonsConstraints );
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
			
			// add especial listener if the input image is a stack
			if(null != sliceSelector)
			{
				// add adjustment listener to the scroll bar
				sliceSelector.addAdjustmentListener(new AdjustmentListener() 
				{

					public void adjustmentValueChanged(final AdjustmentEvent e) {
						exec.submit(new Runnable() {
							public void run() {							
								if(e.getSource() == sliceSelector)
								{
									//IJ.log("moving scroll");
									originalImage.killRoi();									
									updateOverlay();
								}
							}							
						});
					}
				});

				// mouse wheel listener to update the rois while scrolling
				addMouseWheelListener(new MouseWheelListener() {

					@Override
					public void mouseWheelMoved(final MouseWheelEvent e) {

						exec.submit(new Runnable() {
							public void run() 
							{
								//IJ.log("moving scroll");
								originalImage.killRoi();									
								updateOverlay();
							}
						});

					}
				});

				// key listener to repaint the display image and the traces
				// when using the keys to scroll the stack
				KeyListener keyListener = new KeyListener() {

					@Override
					public void keyTyped(KeyEvent e) {}

					@Override
					public void keyReleased(final KeyEvent e) {
						exec.submit(new Runnable() {
							public void run() 
							{
								if(e.getKeyCode() == KeyEvent.VK_LEFT ||
										e.getKeyCode() == KeyEvent.VK_RIGHT ||
										e.getKeyCode() == KeyEvent.VK_LESS ||
										e.getKeyCode() == KeyEvent.VK_GREATER ||
										e.getKeyCode() == KeyEvent.VK_COMMA ||
										e.getKeyCode() == KeyEvent.VK_PERIOD)
								{
									//IJ.log("moving scroll");
									originalImage.killRoi();									
									updateOverlay();
								}
							}
						});

					}

					@Override
					public void keyPressed(KeyEvent e) {}
				};
				// add key listener to the window and the canvas
				addKeyListener(keyListener);
				canvas.addKeyListener(keyListener);

			}	
			
			// update display image
			updateOverlay();
						
		}
	
	}// end class CustomWindow
	
	
		
	/**
	 * Update the overlay in the display image based on 
	 * the current segmentation image and slice
	 */
	void updateOverlay() 
	{
		if( null != segmentedImage )
		{
			originalImage.deleteRoi();
			int slice = originalImage.getCurrentSlice();

			// create image ROI with segmented image and current transparency
			ImageRoi roi = new ImageRoi(0, 0, segmentedImage.getImageStack().getProcessor( slice ) );
			roi.setOpacity( 1.0 - transparency );
			
			originalImage.setOverlay( new Overlay( roi ) );
			originalImage.updateAndDraw();
		}
	}
	
	/**
	 * Create golden angle LUT (skipping first index, 
	 * which is black).
	 * @return golden angle LUT
	 */
	public final static LUT createGoldenAngleLut() 
	{
		// Create overlay LUT
		final byte[] red = new byte[ 256 ];
		final byte[] green = new byte[ 256 ];
		final byte[] blue = new byte[ 256 ];

		// hue for assigning new color ([0.0-1.0])
		float hue = 0f;
		// saturation for assigning new color ([0.5-1.0]) 
		float saturation = 1f; 
		Color[] colors = new Color[ 255 ]; 
		for(int i=0; i<255; i++)
		{
			colors[ i ] = Color.getHSBColor(hue, saturation, 1);

			hue += 0.38197f; // golden angle
			if (hue > 1) 
				hue -= 1;
			saturation += 0.38197f; // golden angle
			if (saturation > 1)
				saturation -= 1;
			saturation = 0.5f * saturation + 0.5f;							
		}
							
		for(int i = 1 ; i < 256; i++)
		{
			//IJ.log("i = " + i + " color index = " + colorIndex);
			red[i] = (byte) colors[ i-1 ].getRed();
			green[i] = (byte) colors[ i-1 ].getGreen();
			blue[i] = (byte) colors[ i-1 ].getBlue();
		}
		
		return new LUT(red, green, blue);
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
		
		if ( false == nextImage() )
			return;
		

		final ImagePlus firstImage = originalImage;
		// Build GUI
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						win = new CustomWindow( firstImage );
						win.pack();
					}
				});

	}

	boolean nextImage()
	{
		currentImageName = getNextImageName();

		if( null == currentImageName )
			return false;
		
		String realCurrentImageName = currentImageName; 
		
		boolean variousNucleus = currentImageName.contains("-");

		if ( variousNucleus == true )
		{
			int realLast = currentImageName.lastIndexOf("-");
			realCurrentImageName = currentImageName.substring( 0, realLast ) + currentImageName.substring( realLast+2, currentImageName.length() );
		}
		
		
		try{
				// 	open image (check if it is VoxelMatrix first)
				originalImage = currentImageName.endsWith( ".vm" ) ?
				VoxelMatrixIO.read( originalFilesList[counter].getParent().toString() + "/" + currentImageName ) :
				new ImagePlus( originalFilesList[counter].getParent().toString() + "/" + currentImageName );			
		}catch( Exception ex ){
			IJ.error("Could not load " + originalFilesList[counter].getParent().toString() + "/" + realCurrentImageName);
			ex.printStackTrace();
		}
		// read corresponding segmented image
		try{				
			segmentedImage = currentImageName.endsWith( ".vm" ) ?
					VoxelMatrixIO.read( segmentedFilesList[counter].getParent().toString() + "/" + currentImageName ) :
						new ImagePlus( segmentedFilesList[counter].getParent().toString() + "/" + currentImageName );

		}catch( Exception ex ){
			IJ.error("Could not load " + segmentedFilesList[counter].getParent().toString() + "/" + currentImageName);
			ex.printStackTrace();
			return false;
		}

		// assign LUT to segmented image
		segmentedImage.getProcessor().setColorModel( overlayLUT );
		segmentedImage.getImageStack().setColorModel( overlayLUT );
		segmentedImage.setDisplayRange( 0, 255 );
		segmentedImage.updateAndDraw();
				
		// update display image		
		repaintWindow();
		updateOverlay();
		
		
		return true;
	}
	
	void discardImage()
	{
		discardedFilesList.add( segmentedFilesList[counter].getPath().toString() );
		IJ.log("Images discarded: " + discardedFilesList.size() + "/" + (counter+1) );
		
		if( counter < segmentedFilesList.length )
			nextImage();
		else
		{
			IJ.run("Close All");
			finishProcess();
		}
	}

	void finishProcess()
	{
		
		String discardedFolder = segmentedFilesList[0].getParentFile().getParentFile().toString()+ "/discarded";
		IJ.log(discardedFolder);
		new File(discardedFolder).mkdirs();
		float success = ( discardedFilesList.size()*100 / (counter) ) ; 
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
		
	/**
	 * Repaint whole window
	 */
	private void repaintWindow()
	{
		// Repaint window
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						win.setImage(originalImage);

						canvas = win.getCanvas();

						// Zoom in if image is too small
						int largestDim = canvas.getWidth() > canvas.getHeight() ? canvas.getWidth() : canvas.getHeight() ;
						
						int iter = 512 / largestDim;
						
						for( int i=0; i<iter; i++)
							IJ.run( win.getImagePlus(), "In","" );

						win.invalidate();
						win.validate();
						win.repaint();
					}
				});
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
		
		String realCurrentSegmentedImageName = currentSegmentedImageName; 
		
		boolean variousNucleus = currentSegmentedImageName.contains("-");

		if ( variousNucleus == true )
		{
			int realLast = currentSegmentedImageName.lastIndexOf("-");
			realCurrentSegmentedImageName = currentSegmentedImageName.substring( 0, realLast ) + currentSegmentedImageName.substring( realLast+2, currentSegmentedImageName.length() );
		}
		
		// look for corresponding original image
		boolean found = false;
		while( found == false && counter >= segmentedFilesList.length)
		{
			for(int i=1; i<originalFilesList.length; i ++)
				if( realCurrentSegmentedImageName.equals( originalFilesList[ i ].getName() ) )
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
