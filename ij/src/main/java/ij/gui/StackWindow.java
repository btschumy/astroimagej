package ij.gui;

import ij.*;
import ij.astro.AstroImageJ;
import ij.astro.accessors.TransferablePlot;
import ij.astro.util.PdfPlotOutput;
import ij.io.SaveDialog;
import ij.plugin.GifWriter;
import ij.plugin.frame.SyncWindows;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.event.*;

/** This class is an extended ImageWindow that displays stacks and hyperstacks. */
public class StackWindow extends ImageWindow implements Runnable, AdjustmentListener, ActionListener, MouseWheelListener {

	protected Scrollbar sliceSelector; // for backward compatibity with Image5D
	protected ScrollbarWithLabel cSelector, zSelector, tSelector;
	protected Thread thread;
	protected volatile boolean done;
	protected volatile int slice;
	protected ScrollbarWithLabel animationSelector;
	boolean hyperStack;
	int nChannels=1, nSlices=1, nFrames=1;
	int c=1, z=1, t=1;


	public StackWindow(ImagePlus imp) {
		this(imp, null);
	}

	@AstroImageJ(reason = "Add check for autoConvert pref before calling show(); add draw call", modified = true)
    public StackWindow(ImagePlus imp, ImageCanvas ic) {
		super(imp, ic);
		addScrollbars(imp);
		addMouseWheelListener(this);
		if (sliceSelector==null && this.getClass().getName().indexOf("Image5D")!=-1)
			sliceSelector = new Scrollbar(); // prevents Image5D from crashing
		draw();
		pack();
		ic = imp.getCanvas();
		if (ic!=null) ic.setMaxBounds();
		if (!Prefs.get("Astronomy_Tool.autoConvert", false)) show();
		int previousSlice = imp.getCurrentSlice();
		if (previousSlice>1 && previousSlice<=imp.getStackSize())
			imp.setSlice(previousSlice);
		else
			imp.setSlice(1);
		thread = new Thread(this, "zSelector");
		thread.start();
	}

	@AstroImageJ(reason = "Add various save buttons to SP stack plot")
	void draw() {
		if (!getTitle().startsWith("Seeing Profile")) {
			return;
		}
		Panel bottomPanel = new Panel();
		int hgap = IJ.isMacOSX()?1:5;

		var pdf = new JButton(" Summary PDF ");
		pdf.setToolTipText("Save summary image as PDF");
		bottomPanel.add(pdf);

		var png = new JButton(" Summary PNG ");
		png.setToolTipText("Save summary image as PNG");
		bottomPanel.add(png);

		var stackPdf = new JButton(" Stack PDF ");
		stackPdf.setToolTipText("Save full stack as PDF");
		bottomPanel.add(stackPdf);

		var gif = new JButton(" Stack GIF ");
		gif.setToolTipText("Save full stack as GIF");
		bottomPanel.add(gif);

		var copy = new JButton(" Copy... ");
		copy.setToolTipText("Copy plot image or data");
		if (imp.getStack() instanceof PlotVirtualStack) {
			bottomPanel.add(copy);
		}

		var listener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object b = e.getSource();
				Plot summaryPlot = null;
				if (imp.getStack() instanceof PlotVirtualStack plotVirtualStack) {
					summaryPlot = plotVirtualStack.getPlot(1);
				}
				if (b == pdf) {
					String fileName = getTitle().replace("Plot of ","").replace("Measurements in ", "");
					SaveDialog sf = new SaveDialog("Save summary plot as vector PDF", fileName, ".pdf");
					if (sf.getDirectory() == null || sf.getFileName() == null) return;
					PdfPlotOutput.savePlot(summaryPlot, sf.getDirectory()+sf.getFileName());
				} else if (b == png) {
					String fileName = getTitle().replace("Plot of ","").replace("Measurements in ", "");
					SaveDialog sf = new SaveDialog("Save summary plot as PNG",fileName, ".png");
					if (sf.getDirectory() == null || sf.getFileName() == null) return;
					var imp2 = imp.duplicate();
					imp2.setSlice(1);
					IJ.runPlugIn(imp2, "ij.plugin.PNG_Writer", sf.getDirectory()+sf.getFileName());
				} else if (b == gif) {
					String fileName = getTitle().replace("Plot of ","").replace("Measurements in ", "");
					SaveDialog sf = new SaveDialog("Save plot stack as GIF", fileName, ".gif");
					if (sf.getDirectory() == null || sf.getFileName() == null) return;
					GifWriter.save(imp, sf.getDirectory()+sf.getFileName());
				} else if (b == stackPdf) {
					String fileName = getTitle().replace("Plot of ","").replace("Measurements in ", "");
					SaveDialog sf = new SaveDialog("Save plot stack as vector PDF", fileName, ".pdf");
					if (sf.getDirectory() == null || sf.getFileName() == null) return;
					if (imp.getStack() instanceof PlotVirtualStack plotVirtualStack) {
						PdfPlotOutput.savePlotStack(plotVirtualStack, sf.getDirectory()+sf.getFileName());
					}
				}
			}
		};
		png.addActionListener(listener);
		pdf.addActionListener(listener);
		stackPdf.addActionListener(listener);
		gif.addActionListener(listener);
		copy.addActionListener($ -> {
			if (imp.getStack() instanceof PlotVirtualStack plotVirtualStack) {
				Clipboard systemClipboard = null;
				try {systemClipboard = getToolkit().getSystemClipboard();}
				catch (Exception e) {systemClipboard = null; }
				if (systemClipboard==null)
				{IJ.error("Unable to copy to Clipboard."); return;}
				IJ.showStatus("Copying plot values...");
				systemClipboard.setContents(new TransferablePlot(plotVirtualStack.getPlot(imp.getCurrentSlice())), ($1, $2) -> {});
			}
		});

		bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT,hgap,0));
		add(bottomPanel);
	}

	void addScrollbars(ImagePlus imp) {
		ImageStack s = imp.getStack();
		int stackSize = s.getSize();
		int sliderHeight = 0;
		nSlices = stackSize;
		hyperStack = imp.getOpenAsHyperStack();
		//imp.setOpenAsHyperStack(false);
		int[] dim = imp.getDimensions();
		int nDimensions = 2+(dim[2]>1?1:0)+(dim[3]>1?1:0)+(dim[4]>1?1:0);
		if (nDimensions<=3 && dim[2]!=nSlices)
			hyperStack = false;
		if (hyperStack) {
			nChannels = dim[2];
			nSlices = dim[3];
			nFrames = dim[4];
		}
		if (nSlices==stackSize) hyperStack = false;
		if (nChannels*nSlices*nFrames!=stackSize) hyperStack = false;
		if (cSelector!=null||zSelector!=null||tSelector!=null)
			removeScrollbars();
		ImageJ ij = IJ.getInstance();
		//IJ.log("StackWindow: "+hyperStack+" "+nChannels+" "+nSlices+" "+nFrames+" "+imp);
		if (nChannels>1) {
			cSelector = new ScrollbarWithLabel(this, 1, 1, 1, nChannels+1, 'c');
			add(cSelector);
			sliderHeight += cSelector.getPreferredSize().height + ImageWindow.VGAP;
			if (ij!=null) cSelector.addKeyListener(ij);
			cSelector.addAdjustmentListener(this);
			cSelector.setFocusable(false); // prevents scroll bar from blinking on Windows
			cSelector.setUnitIncrement(1);
			cSelector.setBlockIncrement(1);
		}
		if (nSlices>1) {
			char label = nChannels>1||nFrames>1?'z':'t';
			if (stackSize==dim[2] && imp.isComposite()) label = 'c';
			zSelector = new ScrollbarWithLabel(this, 1, 1, 1, nSlices+1, label);
			if (label=='t') animationSelector = zSelector;
			add(zSelector);
			sliderHeight += zSelector.getPreferredSize().height + ImageWindow.VGAP;
			if (ij!=null) zSelector.addKeyListener(ij);
			zSelector.addAdjustmentListener(this);
			zSelector.setFocusable(false);
			int blockIncrement = nSlices/10;
			if (blockIncrement<1) blockIncrement = 1;
			zSelector.setUnitIncrement(1);
			zSelector.setBlockIncrement(blockIncrement);
			sliceSelector = zSelector.bar;
		}
		if (nFrames>1) {
			animationSelector = tSelector = new ScrollbarWithLabel(this, 1, 1, 1, nFrames+1, 't');
			add(tSelector);
			sliderHeight += tSelector.getPreferredSize().height + ImageWindow.VGAP;
			if (ij!=null) tSelector.addKeyListener(ij);
			tSelector.addAdjustmentListener(this);
			tSelector.setFocusable(false);
			int blockIncrement = nFrames/10;
			if (blockIncrement<1) blockIncrement = 1;
			tSelector.setUnitIncrement(1);
			tSelector.setBlockIncrement(blockIncrement);
		}
		ImageWindow win = imp.getWindow();
		if (win!=null)
			win.setSliderHeight(sliderHeight);
	}

	/** Enables or disables the sliders. Used when locking/unlocking an image. */
	public synchronized void setSlidersEnabled(final boolean b) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (sliceSelector != null)     sliceSelector.setEnabled(b);
				if (cSelector != null)         cSelector.setEnabled(b);
				if (zSelector != null)         zSelector.setEnabled(b);
				if (tSelector != null)         tSelector.setEnabled(b);
				if (animationSelector != null) animationSelector.setEnabled(b);
			}
		});
	}

	public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
		if (!running2 || imp.isHyperStack()) {
			if (e.getSource()==cSelector) {
				c = cSelector.getValue();
				if (c==imp.getChannel()&&e.getAdjustmentType()==AdjustmentEvent.TRACK) return;
			} else if (e.getSource()==zSelector) {
				z = zSelector.getValue();
				int slice = hyperStack?imp.getSlice():imp.getCurrentSlice();
				if (z==slice&&e.getAdjustmentType()==AdjustmentEvent.TRACK) return;
			} else if (e.getSource()==tSelector) {
				t = tSelector.getValue();
				if (t==imp.getFrame()&&e.getAdjustmentType()==AdjustmentEvent.TRACK) return;
			}
			slice = (t-1)*nChannels*nSlices + (z-1)*nChannels + c;
			notify();
		}
		if (!running)
			syncWindows(e.getSource());
	}

	private void syncWindows(Object source) {
		if (SyncWindows.getInstance()==null)
			return;
		if (source==cSelector)
			SyncWindows.setC(this, cSelector.getValue());
		else if (source==zSelector) {
			int stackSize = imp.getStackSize();
			if (imp.getNChannels()==stackSize)
				SyncWindows.setC(this, zSelector.getValue());
			else if (imp.getNFrames()==stackSize)
				SyncWindows.setT(this, zSelector.getValue());
			else
				SyncWindows.setZ(this, zSelector.getValue());
		} else if (source==tSelector)
			SyncWindows.setT(this, tSelector.getValue());
		else
			throw new RuntimeException("Unknownsource:"+source);
	}

	public void actionPerformed(ActionEvent e) {
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		synchronized(this) {
			int rotation = e.getWheelRotation();
			boolean ctrl = (e.getModifiers()&Event.CTRL_MASK)!=0;
			if ((ctrl||IJ.shiftKeyDown()) && ic!=null) {
				Point loc = ic.getCursorLoc();
				int x = ic.screenX(loc.x);
				int y = ic.screenY(loc.y);
				if (rotation<0)
					ic.zoomIn(x,y);
				else
					ic.zoomOut(x,y);
				return;
			}
			if (hyperStack) {
				if (rotation>0)
					IJ.run(imp, "Next Slice [>]", "");
				else if (rotation<0)
					IJ.run(imp, "Previous Slice [<]", "");
			} else {
				int slice = imp.getCurrentSlice() + rotation;
				if (slice<1)
					slice = 1;
				else if (slice>imp.getStack().getSize())
					slice = imp.getStack().getSize();
				setSlice(imp,slice);
				imp.updateStatusbarValue();
				SyncWindows.setZ(this, slice);
			}
		}
	}

	public boolean close() {
		if (!super.close())
			return false;
		synchronized(this) {
			done = true;
			notify();
		}
        return true;
	}

	/** Displays the specified slice and updates the stack scrollbar. */
	public void showSlice(int index) {
		if (imp!=null && index>=1 && index<=imp.getStackSize()) {
			setSlice(imp,index);
			SyncWindows.setZ(this, index);
		}
	}

	/** Updates the stack scrollbar. */
	public void updateSliceSelector() {
		if (hyperStack || zSelector==null || imp==null)
			return;
		int stackSize = imp.getStackSize();
		int max = zSelector.getMaximum();
		if (max!=(stackSize+1))
			zSelector.setMaximum(stackSize+1);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (imp!=null && zSelector!=null)
					zSelector.setValue(imp.getCurrentSlice());
			}
		});
	}

	public void run() {
		while (!done) {
			synchronized(this) {
				try {wait();}
				catch(InterruptedException e) {}
			}
			if (done) return;
			if (slice>0) {
				int s = slice;
				slice = 0;
				if (s!=imp.getCurrentSlice()) {
					imp.updatePosition(c, z, t);
					setSlice(imp,s);
				}
			}
		}
	}

	public String createSubtitle() {
		String subtitle = super.createSubtitle();
		if (!hyperStack || imp.getStackSize()==1)
			return subtitle;
    	String s="";
    	int[] dim = imp.getDimensions(false);
    	int channels=dim[2], slices=dim[3], frames=dim[4];
		if (channels>1) {
			s += "c:"+imp.getChannel()+"/"+channels;
			if (slices>1||frames>1) s += " ";
		}
		if (slices>1) {
			s += "z:"+imp.getSlice()+"/"+slices;
			if (frames>1) s += " ";
		}
		if (frames>1)
			s += "t:"+imp.getFrame()+"/"+frames;
		if (running2) return s;
		int index = subtitle.indexOf(";");
		if (index!=-1) {
			int index2 = subtitle.indexOf("(");
			if (index2>=0 && index2<index && subtitle.length()>index2+4 && !subtitle.substring(index2+1, index2+4).equals("ch:")) {
				index = index2;
				s = s + " ";
			}
			subtitle = subtitle.substring(index, subtitle.length());
		} else
			subtitle = "";
    	return s + subtitle;
    }

    public boolean isHyperStack() {
    	return hyperStack && getNScrollbars()>0;
    }

    public void setPosition(int channel, int slice, int frame) {
    	if (cSelector!=null && channel!=c) {
    		c = channel;
			cSelector.setValue(channel);
			SyncWindows.setC(this, channel);
		}
    	if (zSelector!=null && slice!=z) {
    		z = slice;
			zSelector.setValue(slice);
			SyncWindows.setZ(this, slice);
		}
    	if (tSelector!=null && frame!=t) {
    		t = frame;
			tSelector.setValue(frame);
			SyncWindows.setT(this, frame);
		}
		this.slice = (t-1)*nChannels*nSlices + (z-1)*nChannels + c;
		imp.updatePosition(c, z, t);
		if (this.slice>0) {
			int s = this.slice;
			this.slice = 0;
			if (s!=imp.getCurrentSlice())
				imp.setSlice(s);
		}
    }

    private void setSlice(ImagePlus imp, int n) {
		if (imp.isLocked()) {
			IJ.beep();
			IJ.showStatus("Image is locked");
		} else
			imp.setSlice(n);
    }

	public boolean validDimensions() {
		int c = imp.getNChannels();
		int z = imp.getNSlices();
		int t = imp.getNFrames();
		//IJ.log(c+" "+z+" "+t+" "+nChannels+" "+nSlices+" "+nFrames+" "+imp.getStackSize());
		int size = imp.getStackSize();
		if (c==size && c*z*t==size && nSlices==size && nChannels*nSlices*nFrames==size)
			return true;
		if (c!=nChannels||z!=nSlices||t!=nFrames||c*z*t!=size)
			return false;
		else
			return true;
	}

    public void setAnimate(boolean b) {
    	if (running2!=b && animationSelector!=null)
    		animationSelector.updatePlayPauseIcon();
		running2 = b;
    }

    public boolean getAnimate() {
    	return running2;
    }

    public int getNScrollbars() {
    	int n = 0;
    	if (cSelector!=null) n++;
    	if (zSelector!=null) n++;
    	if (tSelector!=null) n++;
    	return n;
    }

    void removeScrollbars() {
    	if (cSelector!=null) {
    		remove(cSelector);
			cSelector.removeAdjustmentListener(this);
    		cSelector = null;
    	}
    	if (zSelector!=null) {
    		remove(zSelector);
			zSelector.removeAdjustmentListener(this);
    		zSelector = null;
    	}
    	if (tSelector!=null) {
    		remove(tSelector);
			tSelector.removeAdjustmentListener(this);
    		tSelector = null;
    	}
    }

}
