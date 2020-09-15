package de.vemaeg.pdf.ua;

/**
 * override wg image caching... :-(
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextFSImage;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.PDFAsImage;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.swing.NaiveUserAgent;
import org.xhtmlrenderer.util.ImageUtil;
import org.xhtmlrenderer.util.XRLog;

import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;

/**
 * Modifikation des ITextUserAgent, da das Caching dort viel zu klein dimensioniert ist, 
 * die Cache-Capacity in der Original-Klassse aber private ist und somit nicht per subclassing überschreibbar.
 * 
 * Zudem wurde ein connection timeout eingeführt, damit inkorrekte Image-URLs den PDF-Prozees nicht zu lange blocken.
 * Fehlgeschlagene URLs werden protokolliert und anschließend nichtmehr angesprochen
 */
public class ITextUserAgentWithCache extends NaiveUserAgent {
    private static final int IMAGE_CACHE_CAPACITY = 128;
    
    private static final int CONNECT_TIMEOUT = 1000;
    private static final int READ_TIMEOUT = 1000;

    private SharedContext _sharedContext;
    private List<String> failedUriList = new ArrayList<String>();

    private final ITextOutputDevice _outputDevice;

    public ITextUserAgentWithCache(ITextOutputDevice outputDevice) {
		super(IMAGE_CACHE_CAPACITY);
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
	public ImageResource getImageResource(String uriStr) {
        ImageResource resource = null;
        if (ImageUtil.isEmbeddedBase64Image(uriStr)) {
            resource = loadEmbeddedBase64ImageResource(uriStr);
        } else {
            uriStr = resolveURI(uriStr);
            
            // failed before? -> return immediately
            if (failedUriList.contains(uriStr)) {
            	return new ImageResource(uriStr, null);
            }
            
            resource = (ImageResource) _imageCache.get(uriStr);
            if (resource == null) {
                InputStream is = resolveAndOpenStream(uriStr);
                if (is != null) {
                    try {
                        URI uri = new URI(uriStr);
                        if (uri.getPath() != null && uri.getPath().toLowerCase().endsWith(".pdf")) {
                            PdfReader reader = _outputDevice.getReader(uri);
                            PDFAsImage image = new PDFAsImage(uri);
                            Rectangle rect = reader.getPageSizeWithRotation(1);
                            image.setInitialWidth(rect.getWidth() * _outputDevice.getDotsPerPoint());
                            image.setInitialHeight(rect.getHeight() * _outputDevice.getDotsPerPoint());
                            resource = new ImageResource(uriStr, image);
                        } else {
                            Image image = Image.getInstance(readStream(is));
                            scaleToOutputResolution(image);
                            resource = new ImageResource(uriStr, new ITextFSImage(image));
                        }
                        _imageCache.put(uriStr, resource);
                    } catch (Exception e) {
                    	failedUriList.add(uriStr);
                        XRLog.exception("Can't read image file; unexpected problem for URI '" + uriStr + "'", e);
                    } finally {
                        try {
                            is.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                } else {
                	failedUriList.add(uriStr);
                }
            }

            if (resource != null) {
                FSImage image=resource.getImage();
                if (image instanceof ITextFSImage) {
                    image=(FSImage) ((ITextFSImage) resource.getImage()).clone();
                }
                resource = new ImageResource(resource.getImageUri(), image);
            } else {
                resource = new ImageResource(uriStr, null);
            }
        }
        return resource;
    }
    
    private ImageResource loadEmbeddedBase64ImageResource(final String uri) {
        try {
            byte[] buffer = ImageUtil.getEmbeddedBase64Image(uri);
            Image image = Image.getInstance(buffer);
            scaleToOutputResolution(image);
            return new ImageResource(null, new ITextFSImage(image));
        } catch (Exception e) {
            XRLog.exception("Can't read XHTML embedded image.", e);
        }
        return new ImageResource(null, null);
    }

    private void scaleToOutputResolution(Image image) {
        float factor = _sharedContext.getDotsPerPixel();
        if (factor != 1.0f) {
            image.scaleAbsolute(image.getPlainWidth() * factor, image.getPlainHeight() * factor);
        }
    }

    public SharedContext getSharedContext() {
        return _sharedContext;
    }

    public void setSharedContext(SharedContext sharedContext) {
        _sharedContext = sharedContext;
    }
    
    /**
     * override, um vernünftige timeouts zu setzen
     */
    protected InputStream resolveAndOpenStream(String uri) {
        java.io.InputStream is = null;
        uri = resolveURI(uri);
        try {
            URL url = new URL(uri);
            URLConnection urlConn = url.openConnection();
            urlConn.setConnectTimeout(CONNECT_TIMEOUT);
            urlConn.setReadTimeout(READ_TIMEOUT);
            urlConn.setAllowUserInteraction(false);
            is = urlConn.getInputStream();
        } catch (java.net.MalformedURLException e) {
            XRLog.exception("bad URL given: " + uri, e);
        } catch (java.io.FileNotFoundException e) {
            XRLog.exception("item at URI " + uri + " not found");
        } catch (java.io.IOException e) {
            XRLog.exception("IO problem for " + uri, e);
        }
        return is;
    }

}
