// OverlayCanvas.java
package astroj;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.StackWindow;

import java.awt.*;
import java.util.Enumeration;
import java.util.Vector;

/**
 * A special ImageCanvas which displays lists of ROIs in a non-destructive overlay.
 */
public class OverlayCanvas extends ImageCanvas
	{
	/** A Vector for storing the ROIs in the overlay. */
	Vector<Roi> rois;

	/**
	 * Constructor for the overlay, attached to a particular ImagePlus.
	 */
	public OverlayCanvas (ImagePlus imp)
		{
		super (imp);
		rois = new Vector<>();
		}

	/**
	 * Number of rois attached to overlay.
	 */
	public void add (Roi roi)
		{
		// Don't add duplicate rois - roi's equal's method (used by Vector#contains) does not check if rois
		// are of different types
		var index = rois.indexOf(roi);
		if (index >= 0 && rois.get(index).getClass().isAssignableFrom(roi.getClass())) return;

		rois.addElement (roi);
		}

	/**
	 * Removes a particular roi from the overlay list if it encloses the pixel position (x,y).
	 */
	public boolean removeRoi (int x, int y)
		{
		boolean removed = false;
		Enumeration<Roi> e = rois.elements();
		while (e.hasMoreElements())
			{
			Roi roi = e.nextElement();
            if (roi instanceof astroj.ApertureRoi)
                {
                ApertureRoi apRoi = (ApertureRoi)roi;
                double xPos = apRoi.getXpos();
                double yPos = apRoi.getYpos();
                double radius = apRoi.getRadius();
                if ((x-xPos)*(x-xPos)+(y-yPos)*(y-yPos) <= radius*radius)
                    {
                    rois.remove (roi);
                    removed = true;
                    return removed;
                    }
                }
            else if (roi.contains(x, y))
				{
				rois.remove (roi);
				removed = true;
                return removed;
				}
			}
		return removed;
		}
    
    
	/**
	 * Removes a particular roi from the overlay list if it encloses the pixel position (x,y).
	 */
	public boolean removeAnnotateRoi (double x, double y)
		{
        for (int i=0; i<rois.size();i++)
            {
            Roi roi = rois.get(i);
            if (roi instanceof astroj.AnnotateRoi)
                {    
                AnnotateRoi annotateRoi = (AnnotateRoi)roi;
                double xPos = annotateRoi.getXpos();
                double yPos = annotateRoi.getYpos();
                double radius = annotateRoi.getRadius();
                if ((x-xPos)*(x-xPos)+(y-yPos)*(y-yPos) <= radius*radius)
                    {                
                    rois.removeElementAt(i);
                    return true; // Remove first found
                    }
                }   
            }        
		return false;
		}
    
	/**
	 * Returns a particular Annotate roi from the overlay list if it encloses the pixel position (x,y).
	 */
	public AnnotateRoi findAnnotateRoi (double x, double y)
		{
        for (int i=0; i<rois.size();i++)
            {
            Roi roi = rois.get(i);
            if (roi instanceof astroj.AnnotateRoi)
                {    
                AnnotateRoi annotateRoi = (AnnotateRoi)roi;
                double xPos = annotateRoi.getXpos();
                double yPos = annotateRoi.getYpos();
                double radius = annotateRoi.getRadius();
                if ((x-xPos)*(x-xPos)+(y-yPos)*(y-yPos) <= radius*radius)
                    {                
                    return (AnnotateRoi)roi;
                    }
                }   
            }        
		return null;
		}    
    
	/**
	 * Returns a particular roi from the overlay list if it encloses the pixel position (x,y).
	 */
	public ApertureRoi findApertureRoi (double x, double y, double radiusSlack)
		{
        for (int i=0; i<rois.size();i++)
            {
            Roi roi = rois.get(i);
            if (roi instanceof astroj.ApertureRoi)
                {    
                ApertureRoi apertureRoi = (ApertureRoi)roi;
                double xPos = apertureRoi.getXpos();
                double yPos = apertureRoi.getYpos();
                double radius = apertureRoi.getRadius()+radiusSlack;
                if ((x-xPos)*(x-xPos)+(y-yPos)*(y-yPos) <= radius*radius)
                    {                
                    return (ApertureRoi)roi;
                    }
                }   
            }        
		return null;
		}     
    
	/**
	 * Returns a particular roi from the overlay list if it matches the aperture number (zero based).
	 */
	public ApertureRoi findApertureRoiByNumber (int ap)
		{
        for (int i=0; i<rois.size();i++)
            {
            Roi roi = rois.get(i);
            if (roi instanceof astroj.ApertureRoi)
                {    
                ApertureRoi apertureRoi = (ApertureRoi)roi;
                String name = apertureRoi.getName();
                name = name.substring(1);
                int num = IJU.parseInteger(name);
                if (num == ap+1)
                    {                
                    return (ApertureRoi)roi;
                    }
                }   
            }        
		return null;
		}     
    
	/**
	 * Removes all annotate rois from the overlay list.
	 */
	public void removeAnnotateRois ()
		{
			rois.removeIf(roi -> roi instanceof AnnotateRoi);
		}   
    
    /**
	 * Removes all astrometry annotate rois from the overlay list.
	 */
	public void removeAstrometryAnnotateRois ()
		{
			rois.removeIf(roi -> roi instanceof AnnotateRoi && ((AnnotateRoi) roi).isFromAstrometry);
		}
    
	/**
	 * Removes all aperture rois from the overlay list.
	 */
	public void removeApertureRois ()
		{
			rois.removeIf(roi -> roi instanceof astroj.ApertureRoi);
		}

    /**
	 * Moves all aperture rois in the overlay list.
	 */
	public void moveApertureRois (double dx, double dy)
		{
			rois.forEach(roi -> {
				if (roi instanceof astroj.ApertureRoi) ((ApertureRoi) roi).move(dx, dy);
			});
		}
    
	public void removePixelRois ()
		{
			rois.removeIf(roi -> roi instanceof astroj.PixelRoi);
		}    
    
    	/**
	 * Returns a particular Annotate roi from the overlay list if it encloses the pixel position (x,y).
	 */
	public MeasurementRoi findMeasurementRoi (double x, double y)
		{
        for (int i=0; i<rois.size();i++)
            {
            Roi roi = rois.get(i);
            if (roi instanceof astroj.MeasurementRoi)
                {    
                MeasurementRoi measurementRoi = (MeasurementRoi)roi;
                if (measurementRoi.contains(x, y) && !measurementRoi.getIsLiveMeasRoi()) return measurementRoi;
                }   
            }        
		return null;
		}   
    
    	/**
	 * Removes a particular roi from the overlay list if it encloses the pixel position (x,y).
	 */
	public boolean removeMeasurementRoi (MeasurementRoi targetRoi)
		{
        for (int i=0; i<rois.size();i++)
            {
            Roi roi = rois.get(i);
            if (roi instanceof astroj.MeasurementRoi)
                {    
                MeasurementRoi mRoi = (MeasurementRoi)roi;
                if (!mRoi.getIsLiveMeasRoi()&&targetRoi.getX1()==mRoi.getX1()&&targetRoi.getY1()==mRoi.getY1()&&
                        targetRoi.getX2()==mRoi.getX2()&&targetRoi.getY2()==mRoi.getY2())
                    {                
                    rois.removeElementAt(i);
                    return true; // Remove first found
                    }
                }   
            }
		return false;
		}
    
    /**
	 * Removes a particular roi from the overlay list if it encloses the pixel position (x,y).
	 */
	public boolean removeApertureRoi (ApertureRoi targetRoi)
		{
        for (int i=0; i<rois.size();i++)
            {
            Roi roi = rois.get(i);
            if (roi instanceof astroj.ApertureRoi)
                {    
                ApertureRoi aRoi = (ApertureRoi)roi;
                if ((targetRoi.getXpos()-aRoi.getXpos())*(targetRoi.getXpos()-aRoi.getXpos())+
                    (targetRoi.getYpos()-aRoi.getYpos())*(targetRoi.getYpos()-aRoi.getYpos()) <= aRoi.getRadius()*aRoi.getRadius())
                    {                
                    rois.removeElementAt(i);
                    return true; // Remove first found
                    }
                }   
            }
		return false;
		}
        	/**
	 * Removes the measurement roi that displays when dragging to define the ROI.
	 */
	public boolean removeLiveMeasurementRoi ()
		{
        for (int i=0; i<rois.size();i++)
            {
            Roi roi = rois.get(i);
            if (roi instanceof astroj.MeasurementRoi)
                {    
                MeasurementRoi mRoi = (MeasurementRoi)roi;
                if (mRoi.getIsLiveMeasRoi())
                    {                
                    rois.removeElementAt(i);
                    return true; // Remove first found
                    }
                }   
            }
		return false;
		}

	/**
	 * Deletes all of the current ROIs.
	 */
	public void clearRois ()
		{
		rois.clear();
		}

	/**
	 * Lists current ROIs.
	 */
	public void listRois()
		{
		int n = rois.size();
		if (n == 0) return;
		rois.forEach(roi -> ((ApertureRoi)roi).log());
		}

	/**
	 * Returns an array of copies of the current ROIs.
	 */
	public Roi[] getRois()
		{
		int n = rois.size();
		if (n == 0) return null;

		return rois.toArray(new Roi[n]);
		}

	/**
	 * Returns the number of rois in the overlay's list.
	 */
	public int numberOfRois()
		{
		return rois.size();
		}

	/**
	 * Displays the overlay and it's ROIs.
	 */
	public void paint(Graphics g)
		{
		super.paint (g);
		drawOverlayCanvas (g);
		}

	public void drawOverlayCanvas (Graphics g)
		{
//		g.setColor(Color.green);
			rois.forEach(roi -> roi.draw(g));
		}

	/**
	 * A handy routine which checks if the image has an OverlayCanvas.
	 */
	public static boolean hasOverlayCanvas (ImagePlus imag)
		{
		// ImageCanvas canvas = imag.getCanvas();	    // ImageJ 1.38
		ImageCanvas canvas = null;
		ImageWindow win = imag.getWindow();
		if (win != null) canvas = win.getCanvas();
		if (canvas == null)
			return false;
		else if (canvas instanceof OverlayCanvas)
			return true;
		else
			return false;
		}

	/**
	 * A handy routine which returns an OverlayCanvas, creating one if necessary.
	 */
	public static OverlayCanvas getOverlayCanvas (ImagePlus imag)
		{
		if (OverlayCanvas.hasOverlayCanvas(imag))
			{
			ImageWindow win = imag.getWindow();
			return (OverlayCanvas)win.getCanvas();
			}
		else	{
			OverlayCanvas canv = new OverlayCanvas (imag);
			if (imag.getStackSize () > 1)
				new StackWindow (imag,canv);
			else
				new ImageWindow (imag,canv);
			return canv;
			}
		}
	}
