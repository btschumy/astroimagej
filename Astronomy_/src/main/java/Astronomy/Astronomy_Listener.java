package Astronomy;//Astronomy_Listener.java

import astroj.AstroCanvas;
import astroj.AstroStackWindow;
import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.PlugIn;

import java.awt.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author K.A. Collins, University of Louisville
 * @version 1.0
 * @date 2010-Jul-25
 */
public class Astronomy_Listener implements PlugIn, ImageListener
    {
    public static final boolean REFRESH = true;
    public static final boolean NEW = false;
    public static final boolean RESIZE = true;
    public static final boolean NORESIZE = false;

    public void run (String arg)
		{
        if (IJ.versionLessThan("1.35l")) return;
        ImagePlus.addImageListener(this);
        }


    public void imageOpened(ImagePlus imp)
        {
//        IJ.log("Image Opened: "+imp.getTitle());
        boolean autoConvert = true;
        Frame openFrame = imp.getWindow();
        
        if (openFrame instanceof ij.gui.PlotWindow || imp.getTitle().startsWith("About") || imp.getTitle().startsWith("Profile of") || imp.getTitle().startsWith("Seeing Profile")) return;
        autoConvert = Prefs.get("Astronomy_Tool.autoConvert", autoConvert);
        if (autoConvert)
            {
//            Class<?> mainComponentClass = openFrame.getClass();
            if (!(openFrame instanceof astroj.AstroStackWindow))//(mainComponentClass.getName() != "astroj.AstroStackWindow")
                {
                AstroCanvas ac = new AstroCanvas(imp);
                new AstroStackWindow(imp, ac, NEW, RESIZE);
                }
            }

        }

    public void imageClosed(ImagePlus imp)
        {
        }

    public void imageUpdated(ImagePlus imp)
        {
//        IJ.log("Image Updated: "+imp.getTitle());
        Frame openFrame = imp.getWindow();
        if (openFrame instanceof ij.gui.PlotWindow || imp.getTitle().startsWith("About ImageJ") || imp.getTitle().startsWith("Profile of") || imp.getTitle().startsWith("Seeing Profile")) return;

//        Class<?> mainComponentClass = openFrame.getClass();
        if (openFrame instanceof astroj.AstroStackWindow)//mainComponentClass.getName().equals("astroj.AstroStackWindow"))
            {
            astroj.AstroStackWindow asw = (astroj.AstroStackWindow) openFrame;
            if (asw.isReady && !asw.minMaxChanged)
                {
//                IJ.log("Actually Updating Image: "+imp.getTitle());
                ;
                ex.execute(() -> asw.setAstroProcessor(false));
                }
            else
                {
                asw.minMaxChanged = false;
                }
            }

        }
        final Executor ex = Executors.newCachedThreadPool();

    }

