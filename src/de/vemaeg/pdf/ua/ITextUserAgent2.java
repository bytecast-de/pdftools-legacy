package de.vemaeg.pdf.ua;

/**
 * override wg image caching... :-(
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextFSImage;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.PDFAsImage;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.swing.NaiveUserAgent;
import org.xhtmlrenderer.util.XRLog;

import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;




public class ITextUserAgent2 extends NaiveUserAgent {
    private static final int IMAGE_CACHE_CAPACITY = 128;

    private SharedContext _sharedContext;
    private List<String> failedUriList = new ArrayList<String>();

    private final ITextOutputDevice _outputDevice;

    public ITextUserAgent2(ITextOutputDevice outputDevice) {
		super(IMAGE_CACHE_CAPACITY);
		System.err.println("CACHE SIZE: " + IMAGE_CACHE_CAPACITY);
		_outputDevice = outputDevice;
    }

    private byte[] readStream(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(is.available());
        byte[] buf = new byte[10240];
        int i;
        while ( (i = is.read(buf)) != -1) {
            out.write(buf, 0, i);
        }
        out.close();
        return out.toByteArray();
    }

    @SuppressWarnings("unchecked")
	public ImageResource getImageResource(String uri) {
        ImageResource resource = null;
        uri = resolveURI(uri);
        
        // failed before? -> return immediately
        if (failedUriList.contains(uri)) {
        	return new ImageResource(uri, null);
        }
        
        // try cache
        resource = (ImageResource) _imageCache.get(uri);
        
        if (resource == null) {
            InputStream is = resolveAndOpenStream(uri);
            if (is != null) {
                try {
                    URL url = new URL(uri);
                    if (url.getPath() != null &&
                            url.getPath().toLowerCase().endsWith(".pdf")) {
                        PdfReader reader = _outputDevice.getReader(url);
                        PDFAsImage image = new PDFAsImage(url);
                        Rectangle rect = reader.getPageSizeWithRotation(1);
                        image.setInitialWidth(rect.getWidth()*_outputDevice.getDotsPerPoint());
                        image.setInitialHeight(rect.getHeight()*_outputDevice.getDotsPerPoint());
                        resource = new ImageResource(uri, image);
                    } else {
	                    Image image = Image.getInstance(readStream(is));
	                    scaleToOutputResolution(image);
	                    resource = new ImageResource(uri, new ITextFSImage(image));
                    }
                    
                    _imageCache.put(uri, resource);
                } catch (Exception e) {
                	failedUriList.add(uri);
                    XRLog.exception("Can't read image file; unexpected problem for URI '" + uri + "'", e);
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            } 
        } 

        if (resource != null) {
            resource = new ImageResource(resource.getImageUri(), (FSImage)((ITextFSImage)resource.getImage()).clone());
        } else {
            resource = new ImageResource(uri, null);
        }

        return resource;
    }

    private void scaleToOutputResolution(Image image) {
        float factor = _sharedContext.getDotsPerPixel();
        image.scaleAbsolute(image.getPlainWidth() * factor, image.getPlainHeight() * factor);
    }

    public SharedContext getSharedContext() {
        return _sharedContext;
    }

    public void setSharedContext(SharedContext sharedContext) {
        _sharedContext = sharedContext;
    }
}
