/*
 * Copyright 2005-2006 Jeremias Maerki.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.krysalis.barcode4j.fop;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.fop.area.PageViewport;
import org.apache.fop.render.Graphics2DAdapter;
import org.apache.fop.render.AbstractRenderer;
import org.apache.fop.render.Graphics2DImagePainter;
import org.apache.fop.render.ImageAdapter;
import org.apache.fop.render.Renderer;
import org.apache.fop.render.RendererContext;
import org.apache.fop.render.XMLHandler;
import org.apache.fop.render.ps.PSImageUtils;
import org.apache.fop.render.ps.PSRenderer;
import org.apache.fop.render.ps.PSRendererContextConstants;
import org.apache.xmlgraphics.ps.PSGenerator;

import org.krysalis.barcode4j.BarcodeDimension;
import org.krysalis.barcode4j.BarcodeGenerator;
import org.krysalis.barcode4j.BarcodeUtil;
import org.krysalis.barcode4j.output.BarcodeCanvasSetupException;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;
import org.krysalis.barcode4j.output.eps.EPSCanvasProvider;
import org.krysalis.barcode4j.output.java2d.Java2DCanvasProvider;
import org.krysalis.barcode4j.output.svg.SVGCanvasProvider;
import org.krysalis.barcode4j.tools.ConfigurationUtil;
import org.krysalis.barcode4j.tools.UnitConv;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 * XMLHandler for Apache FOP that handles the Barcode XML by converting it to
 * SVG or by rendering it directly to the output format.
 * 
 * @author Jeremias Maerki
 * @version $Id: BarcodeXMLHandler.java,v 1.7 2006/11/07 16:45:08 jmaerki Exp $
 */
public class BarcodeXMLHandler implements XMLHandler, PSRendererContextConstants {

    private static final boolean DEBUG = false;
    
    protected String getMessage(Configuration cfg) throws ConfigurationException {
        try {
            return cfg.getAttribute("message");
        } catch (ConfigurationException ce) {
            return cfg.getAttribute("msg"); //for compatibility
        }
    }
    
    /** @see org.apache.fop.render.XMLHandler */
    public void handleXML(RendererContext context, 
            Document doc, String ns) throws Exception {
        Configuration cfg = ConfigurationUtil.buildConfiguration(doc);
        String msg = getMessage(cfg);
        if (DEBUG) System.out.println("Barcode message: " + msg);
        String renderMode = cfg.getAttribute("render-mode", "native");
        int orientation = cfg.getAttributeAsInteger("orientation", 0);
        orientation = BarcodeDimension.normalizeOrientation(orientation);
        
        PageViewport page = (PageViewport)context.getProperty(PAGE_VIEWPORT);

        BarcodeGenerator bargen = BarcodeUtil.getInstance().
                createBarcodeGenerator(cfg);
        String expandedMsg = VariableUtil.getExpandedMessage(
                page, msg);

        boolean handled = false;
        String effRenderMode = renderMode;
        if ("native".equals(renderMode)) {
            AbstractRenderer renderer = context.getRenderer();
            if (renderer instanceof PSRenderer) {
                renderUsingEPS(context, bargen, expandedMsg, orientation);
                effRenderMode = "native";
                handled = true;
            }
        } else if ("g2d".equals(renderMode)) {
            handled = renderUsingGraphics2D(context, bargen, expandedMsg, orientation);
            if (handled) {
                effRenderMode = "g2d";
            }
        } else if ("bitmap".equals(renderMode)) {
            handled = renderUsingBitmap(context, bargen, expandedMsg, orientation);
            if (handled) {
                effRenderMode = "bitmap";
            }
        }
        if (!handled) {
            //Convert the Barcode XML to SVG and let it render through 
            //an SVG handler
            convertToSVG(context, bargen, expandedMsg, orientation);
            effRenderMode = "svg";
        }
        if (DEBUG) System.out.println("Effective render mode: " + effRenderMode);
    }

    private void renderUsingEPS(RendererContext context, BarcodeGenerator bargen, 
                String msg, int orientation) throws IOException {
        PSGenerator gen = (PSGenerator)context.getProperty(PS_GENERATOR);
        ByteArrayOutputStream baout = new ByteArrayOutputStream(1024);
        EPSCanvasProvider canvas = new EPSCanvasProvider(baout, orientation);
        bargen.generateBarcode(canvas, msg);
        canvas.finish();
        
        float width = ((Integer)context.getProperty(WIDTH)).intValue() / 1000f;
        float height = ((Integer)context.getProperty(HEIGHT)).intValue() / 1000f;
        float x = ((Integer)context.getProperty(XPOS)).intValue() / 1000f;
        float y = ((Integer)context.getProperty(YPOS)).intValue() / 1000f;
        
        if (DEBUG) System.out.println(" --> EPS");
        PSImageUtils.renderEPS(baout.toByteArray(), "Barcode:" + msg, 
                x, y, width, height, 0, 0, width, height, gen);
    }
    
    private boolean renderUsingGraphics2D(RendererContext context, 
            final BarcodeGenerator bargen, 
            final String msg, final int orientation) throws IOException {
        
        Graphics2DAdapter g2dAdapter = context.getRenderer().getGraphics2DAdapter();
        if (g2dAdapter != null) {
            final BarcodeDimension barDim = bargen.calcDimensions(msg);

            // get the 'width' and 'height' attributes of the barcode
            final int w = (int)Math.ceil(UnitConv.mm2pt(barDim.getWidthPlusQuiet())) * 1000;
            final int h = (int)Math.ceil(UnitConv.mm2pt(barDim.getHeightPlusQuiet())) * 1000;

            Graphics2DImagePainter painter = new Graphics2DImagePainter() {

                public void paint(Graphics2D g2d, Rectangle2D area) {
                    Java2DCanvasProvider canvas = new Java2DCanvasProvider(null, orientation);
                    canvas.setGraphics2D(g2d);
                    g2d.scale(area.getWidth() / barDim.getWidthPlusQuiet(),
                            area.getHeight() / barDim.getHeightPlusQuiet());
                    bargen.generateBarcode(canvas, msg);
                }

                public Dimension getImageSize() {
                    return new Dimension(w, h);
                }

            };
            
            if (DEBUG) System.out.println(" --> Java2D");
            g2dAdapter.paintImage(painter,
                    context,
                    ((Integer)context.getProperty("xpos")).intValue(),
                    ((Integer)context.getProperty("ypos")).intValue(),
                    ((Integer)context.getProperty("width")).intValue(),
                    ((Integer)context.getProperty("height")).intValue());
            return true;
        } else {
            //We can't paint the barcode
            return false;
        }
    }

    private boolean renderUsingBitmap(RendererContext context, 
            final BarcodeGenerator bargen, 
            final String msg, final int orientation) throws IOException {
        ImageAdapter imgAdapter = context.getRenderer().getImageAdapter();
        if (imgAdapter != null) {
            
            BitmapCanvasProvider canvas = new BitmapCanvasProvider(
                    300, BufferedImage.TYPE_BYTE_BINARY, false, orientation);
            bargen.generateBarcode(canvas, msg);
            
            if (DEBUG) System.out.println(" --> Bitmap");
            imgAdapter.paintImage(canvas.getBufferedImage(),
                    context,
                    ((Integer)context.getProperty("xpos")).intValue(),
                    ((Integer)context.getProperty("ypos")).intValue(),
                    ((Integer)context.getProperty("width")).intValue(),
                    ((Integer)context.getProperty("height")).intValue());
            return true;
        } else {
            //We can't paint the barcode
            return false;
        }
    }

    /**
     * Converts the barcode XML to SVG.
     * @param context the renderer context
     * @param bargen the barcode generator
     * @param msg the barcode message
     * @throws BarcodeCanvasSetupException In case of an error while generating the barcode
     */
    private void convertToSVG(RendererContext context, 
            BarcodeGenerator bargen, String msg, int orientation) 
                throws BarcodeCanvasSetupException {
        DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();

        SVGCanvasProvider canvas = new SVGCanvasProvider(impl, true, orientation);
        bargen.generateBarcode(canvas, msg);
        Document svg = canvas.getDOM();
        
        //Call the renderXML() method of the renderer to render the SVG
        if (DEBUG) System.out.println(" --> SVG");
        context.getRenderer().renderXML(context, 
                svg, SVGDOMImplementation.SVG_NAMESPACE_URI);
    }

    /** @see org.apache.fop.render.XMLHandler#getMimeType() */
    public String getMimeType() {
        return XMLHandler.HANDLE_ALL;
    }

    /** @see org.apache.fop.render.XMLHandler#getNamespace() */
    public String getNamespace() {
        return BarcodeElementMapping.NAMESPACE;
    }

    /** @see org.apache.fop.render.XMLHandler#supportsRenderer() */
    public boolean supportsRenderer(Renderer renderer) {
        return (renderer instanceof PSRenderer) 
                || (renderer.getGraphics2DAdapter() != null);
    }

}
