/*  
 *  Copyright 2010 Tom Castle (www.tc33.org)
 *  Licensed under GNU Lesser General Public License
 * 
 *  This file is part of JHeatChart - the heat maps charting api for Java.
 *
 *  JHeatChart is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published 
 *  by the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  JHeatChart is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JHeatChart.  If not, see <http://www.gnu.org/licenses/>.
 */
package graph;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

/**
 * The <code>HeatChart</code> class describes a chart which can display 
 * 3-dimensions of values - x,y and z, where x and y are the usual 2-dimensional
 * axis and z is portrayed by colour intensity. Heat charts are sometimes known 
 * as heat maps. 
 * 
 * 
 */
public class HeatChart {

	// x, y, z data values.
	private double[][] zValues;
	private String[] xValues;
	private String[] yValues;

	private boolean xValuesHorizontal;
	private boolean yValuesHorizontal;

	// General chart settings.
	private Dimension cellSize;
	private Dimension chartSize;
	private int margin;
	private Color backgroundColour;

	// Title settings.
	private String title;
	private Font titleFont;
	private Color titleColour;
	private Dimension titleSize;
	private int titleAscent;

	// Axis settings.
	private int axisThickness;
	private Color axisColour;
	private Font axisLabelsFont;
	private Color axisLabelColour;
	private String xAxisLabel;
	private String yAxisLabel;
	private Color axisValuesColour;
	private Font axisValuesFont; // The font size will be considered the maximum font size - it may be smaller if needed to fit in.
	private int xAxisValuesFrequency;
	private int yAxisValuesFrequency;
	private boolean showXAxisValues;
	private boolean showYAxisValues;

	// Generated axis properties.
	private int xAxisValuesHeight;
	private int xAxisValuesWidthMax;

	private int yAxisValuesHeight;
	private int yAxisValuesAscent;
	private int yAxisValuesWidthMax;

	private Dimension xAxisLabelSize;
	private int xAxisLabelDescent;

	private Dimension yAxisLabelSize;
	private int yAxisLabelAscent;
	
	// Key co-ordinate positions.
	private Point heatMapTL;
	private Point heatMapBR;
	private Point heatMapC;

	// Heat map dimensions.
	private Dimension heatMapSize;

	// Control variable for mapping z-values to colours.
	private double colourScale;

	/**
	 * Constructs a heatmap for the given z-values against x/y-values that by 
	 * default will be the values 0 to n-1, where n is the number of columns or
	 * rows.
	 * 
	 * @param zValues the z-values, where each element is a row of z-values
	 * in the resultant heat chart.
	 * @param low the minimum possible value, which may or may not appear in the
	 * z-values.
	 * @param high the maximum possible value, which may or may not appear in 
	 * the z-values.
	 */
	public HeatChart(double[][] zValues) {
		this.zValues = zValues;


		// Default chart settings.
		this.cellSize = new Dimension(90, 90);
		this.margin = 30;
		this.backgroundColour = Color.WHITE;

		// Default title settings.
		this.title = "Semantic similarity";
		this.titleFont = new Font("Sans-Serif", Font.BOLD, 16);
		this.titleColour = Color.BLACK;

		// Default axis settings.
		this.xAxisLabel = "Gene 1";
		this.yAxisLabel = "Gene 2";
		this.axisThickness = 2;
		this.axisColour = Color.BLACK;
		this.axisLabelsFont = new Font("Sans-Serif", Font.PLAIN, 17);
		this.axisLabelColour = Color.BLACK;
		this.axisValuesColour = Color.BLACK;
		this.axisValuesFont = new Font("Sans-Serif", Font.PLAIN, 15);
		this.xAxisValuesFrequency = 1;
		this.xAxisValuesHeight = 0;
		this.xValuesHorizontal = false;
		this.showXAxisValues = true;
		this.showYAxisValues = true;
		this.yAxisValuesFrequency = 1;
		this.yAxisValuesHeight = 0;
		this.yValuesHorizontal = true;
	}
	
	/**
	 * Returns the 2-dimensional array of z-values currently in use. Each 
	 * element is a double array which represents one row of the heat map, or  
	 * all the z-values for one y-value.
	 * 
	 * @return an array of the z-values in current use, that is, those values 
	 * which will define the colour of each cell in the resultant heat map.
	 */
	public double[][] getZValues() {
		return zValues;
	}

	/**
	 * Sets the x-values which are plotted along the x-axis. The given x-values
	 * array must be the same length as the z-values array has columns. Each 
	 * of the x-values elements will be displayed according to their toString 
	 * representation.
	 * 
	 * @param xValues an array of elements to be displayed as values along the
	 * x-axis.
	 */
	public void setXValues(String[] xValues) {
		this.xValues = xValues;
	}

	/**
	 * Sets the y-values which are plotted along the y-axis. The given y-values
	 * array must be the same length as the z-values array has columns. Each 
	 * of the y-values elements will be displayed according to their toString 
	 * representation.
	 * 
	 * @param yValues an array of elements to be displayed as values along the
	 * y-axis.
	 */
	public void setYValues(String[] yValues) {
		this.yValues = yValues;
	}

	/**
	 * Returns the x-values which are currently set to display along the x-axis.
	 * The array that is returned is either that which was explicitly set with
	 * <code>setXValues(Object[])</code> or that was generated from the offset
	 * and interval that were given to <code>setXValues(double, double)</code>, 
	 * in which case the object type of each element will be <code>Double</code>.
	 * 
	 * @return an array of the values that are to be displayed along the x-axis.
	 */
	public Object[] getXValues() {
		return xValues;
	}

	/**
	 * Returns the y-values which are currently set to display along the y-axis.
	 * The array that is returned is either that which was explicitly set with
	 * <code>setYValues(Object[])</code> or that was generated from the offset
	 * and interval that were given to <code>setYValues(double, double)</code>, 
	 * in which case the object type of each element will be <code>Double</code>.
	 * 
	 * @return an array of the values that are to be displayed along the y-axis.
	 */
	public Object[] getYValues() {
		return yValues;
	}

	/**
	 * Sets whether the text of the values along the x-axis should be drawn 
	 * horizontally left-to-right, or vertically top-to-bottom.
	 * 
	 * @param xValuesHorizontal true if x-values should be drawn horizontally, 
	 * false if they should be drawn vertically.
	 */
	public void setXValuesHorizontal(boolean xValuesHorizontal) {
		this.xValuesHorizontal = xValuesHorizontal;
	}

	/**
	 * Returns whether the text of the values along the x-axis are to be drawn
	 * horizontally left-to-right, or vertically top-to-bottom.
	 * 
	 * @return true if the x-values will be drawn horizontally, false if they 
	 * will be drawn vertically.
	 */
	public boolean isXValuesHorizontal() {
		return xValuesHorizontal;
	}

	/**
	 * Sets whether the text of the values along the y-axis should be drawn 
	 * horizontally left-to-right, or vertically top-to-bottom.
	 * 
	 * @param yValuesHorizontal true if y-values should be drawn horizontally, 
	 * false if they should be drawn vertically.
	 */
	public void setYValuesHorizontal(boolean yValuesHorizontal) {
		this.yValuesHorizontal = yValuesHorizontal;
	}

	/**
	 * Returns whether the text of the values along the y-axis are to be drawn
	 * horizontally left-to-right, or vertically top-to-bottom.
	 * 
	 * @return true if the y-values will be drawn horizontally, false if they 
	 * will be drawn vertically.
	 */
	public boolean isYValuesHorizontal() {
		return yValuesHorizontal;
	}

	/**
	 * Sets the size of each individual cell that constitutes a value in x,y,z
	 * data space. By setting the cell size, any previously set chart size will
	 * be overwritten with a value calculated based upon this value and the 
	 * number of cells along each axis.
	 * 
	 * @param cellSize the new size to use for each individual data cell.
	 * @since 0.6
	 */
	public void setCellSize(Dimension cellSize) {
		this.cellSize = cellSize;
	}

	/**
	 * Returns the size of each individual data cell that constitutes a value in
	 * the x,y,z space.
	 * 
	 * @return the size of each individual data cell.
	 * @since 0.6
	 */
	public Dimension getCellSize() {
		return cellSize;
	}

	/**
	 * Returns the size of the chart in pixels as calculated according to the 
	 * cell dimensions, chart margin and other size settings.
	 * 
	 * @return the size in pixels of the chart image to be generated.
	 * @since 0.6
	 */
	public Dimension getChartSize() {
		return chartSize;
	}

	/**
	 * Returns the String that will be used as the title of any successive 
	 * calls to generate a chart.
	 * 
	 * @return the title of the chart.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the String that will be used as the title of any successive 
	 * calls to generate a chart. The title will be displayed centralised 
	 * horizontally at the top of any generated charts.
	 * 
	 * <p>
	 * If the title is set to <tt>null</tt> then no title will be displayed.
	 * 
	 * <p>
	 * Defaults to null.
	 * 
	 * @param title the chart title to set.
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Returns the String that will be displayed as a description of the 
	 * x-axis in any generated charts.
	 * 
	 * @return the display label describing the x-axis.
	 */
	public String getXAxisLabel() {
		return xAxisLabel;
	}

	/**
	 * Sets the String that will be displayed as a description of the 
	 * x-axis in any generated charts. The label will be displayed 
	 * horizontally central of the x-axis bar.
	 * 
	 * <p>
	 * If the xAxisLabel is set to <tt>null</tt> then no label will be 
	 * displayed.
	 * 
	 * <p>
	 * Defaults to null.
	 * 
	 * @param xAxisLabel the label to be displayed describing the x-axis.
	 */
	public void setXAxisLabel(String xAxisLabel) {
		this.xAxisLabel = xAxisLabel;
	}

	/**
	 * Returns the String that will be displayed as a description of the 
	 * y-axis in any generated charts.
	 * 
	 * @return the display label describing the y-axis.
	 */
	public String getYAxisLabel() {
		return yAxisLabel;
	}

	/**
	 * Sets the String that will be displayed as a description of the 
	 * y-axis in any generated charts. The label will be displayed 
	 * horizontally central of the y-axis bar.
	 * 
	 * <p>
	 * If the yAxisLabel is set to <tt>null</tt> then no label will be 
	 * displayed.
	 * 
	 * <p>
	 * Defaults to null. 
	 * 
	 * @param yAxisLabel the label to be displayed describing the y-axis.
	 */
	public void setYAxisLabel(String yAxisLabel) {
		this.yAxisLabel = yAxisLabel;
	}

	/**
	 * Returns the width of the margin in pixels to be left as empty space 
	 * around the heat map element.
	 * 
	 * @return the size of the margin to be left blank around the edge of the
	 * chart.
	 */
	public int getChartMargin() {
		return margin;
	}

	/**
	 * Sets the width of the margin in pixels to be left as empty space around
	 * the heat map element. If a title is set then half the margin will be 
	 * directly above the title and half directly below it. Where axis labels 
	 * are set then the axis labels may sit partially in the margin.
	 * 
	 * <p>
	 * Defaults to 20 pixels.
	 * 
	 * @param margin the new margin to be left as blank space around the heat 
	 * map.
	 */
	public void setChartMargin(int margin) {
		this.margin = margin;
	}

	/**
	 * Returns an object that represents the colour to be used as the 
	 * background for the whole chart. 
	 * 
	 * @return the colour to be used to fill the chart background.
	 */
	public Color getBackgroundColour() {
		return backgroundColour;
	}

	/**
	 * Sets the colour to be used on the background of the chart. A transparent
	 * background can be set by setting a background colour with an alpha value.
	 * The transparency will only be effective when the image is saved as a png
	 * or gif. 
	 * 
	 * <p>
	 * Defaults to <code>Color.WHITE</code>.
	 * 
	 * @param backgroundColour the new colour to be set as the background fill.
	 */
	public void setBackgroundColour(Color backgroundColour) {
		if (backgroundColour == null) {
			backgroundColour = Color.WHITE;
		}

		this.backgroundColour = backgroundColour;
	}

	/**
	 * Returns the <code>Font</code> that describes the visual style of the 
	 * title.
	 *  
	 * @return the Font that will be used to render the title.
	 */
	public Font getTitleFont() {
		return titleFont;
	}

	/**
	 * Sets a new <code>Font</code> to be used in rendering the chart's title 
	 * String.
	 * 
	 * <p>
	 * Defaults to Sans-Serif, BOLD, 16 pixels.
	 * 
	 * @param titleFont the Font that should be used when rendering the chart 
	 * title.
	 */
	public void setTitleFont(Font titleFont) {
		this.titleFont = titleFont;
	}

	/**
	 * Returns the <code>Color</code> that represents the colour the title text 
	 * should be painted in.
	 * 
	 * @return the currently set colour to be used in painting the chart title.
	 */
	public Color getTitleColour() {
		return titleColour;
	}

	/**
	 * Sets the <code>Color</code> that describes the colour to be used for the 
	 * chart title String.
	 * 
	 * <p>
	 * Defaults to <code>Color.BLACK</code>.
	 * 
	 * @param titleColour the colour to paint the chart's title String.
	 */
	public void setTitleColour(Color titleColour) {
		this.titleColour = titleColour;
	}

	/**
	 * Returns the width of the axis bars in pixels. Both axis bars have the 
	 * same thickness.
	 * 
	 * @return the thickness of the axis bars in pixels.
	 */
	public int getAxisThickness() {
		return axisThickness;
	}

	/**
	 * Sets the width of the axis bars in pixels. Both axis bars use the same 
	 * thickness.
	 * 
	 * <p>
	 * Defaults to 2 pixels.
	 * 
	 * @param axisThickness the thickness to use for the axis bars in any newly
	 * generated charts.
	 */
	public void setAxisThickness(int axisThickness) {
		this.axisThickness = axisThickness;
	}

	/**
	 * Returns the colour that is set to be used for the axis bars. Both axis
	 * bars use the same colour.
	 * 
	 * @return the colour in use for the axis bars.
	 */
	public Color getAxisColour() {
		return axisColour;
	}

	/**
	 * Sets the colour to be used on the axis bars. Both axis bars use the same
	 * colour.
	 * 
	 * <p>
	 * Defaults to <code>Color.BLACK</code>.
	 * 
	 * @param axisColour the colour to be set for use on the axis bars.
	 */
	public void setAxisColour(Color axisColour) {
		this.axisColour = axisColour;
	}

	/**
	 * Returns the font that describes the visual style of the labels of the 
	 * axis. Both axis' labels use the same font.
	 * 
	 * @return the font used to define the visual style of the axis labels.
	 */
	public Font getAxisLabelsFont() {
		return axisLabelsFont;
	}

	/**
	 * Sets the font that describes the visual style of the axis labels. Both 
	 * axis' labels use the same font.
	 * 
	 * <p>
	 * Defaults to Sans-Serif, PLAIN, 12 pixels.
	 * 
	 * @param axisLabelsFont the font to be used to define the visual style of 
	 * the axis labels.
	 */
	public void setAxisLabelsFont(Font axisLabelsFont) {
		this.axisLabelsFont = axisLabelsFont;
	}

	/**
	 * Returns the current colour of the axis labels. Both labels use the same
	 * colour.
	 * 
	 * @return the colour of the axis label text.
	 */
	public Color getAxisLabelColour() {
		return axisLabelColour;
	}

	/**
	 * Sets the colour of the text displayed as axis labels. Both labels use 
	 * the same colour.
	 * 
	 * <p>
	 * Defaults to Color.BLACK.
	 * 
	 * @param axisLabelColour the colour to use for the axis label text.
	 */
	public void setAxisLabelColour(Color axisLabelColour) {
		this.axisLabelColour = axisLabelColour;
	}

	/**
	 * Returns the font which describes the visual style of the axis values. 
	 * The axis values are those values displayed alongside the axis bars at 
	 * regular intervals. Both axis use the same font.
	 * 
	 * @return the font in use for the axis values.
	 */
	public Font getAxisValuesFont() {
		return axisValuesFont;
	}

	/**
	 * Sets the font which describes the visual style of the axis values. The 
	 * axis values are those values displayed alongside the axis bars at 
	 * regular intervals. Both axis use the same font.
	 * 
	 * <p>
	 * Defaults to Sans-Serif, PLAIN, 10 pixels.
	 * 
	 * @param axisValuesFont the font that should be used for the axis values.
	 */
	public void setAxisValuesFont(Font axisValuesFont) {
		this.axisValuesFont = axisValuesFont;
	}

	/**
	 * Returns the colour of the axis values as they will be painted along the 
	 * axis bars. Both axis use the same colour.
	 * 
	 * @return the colour of the values displayed along the axis bars.
	 */
	public Color getAxisValuesColour() {
		return axisValuesColour;
	}

	/**
	 * Sets the colour to be used for the axis values as they will be painted 
	 * along the axis bars. Both axis use the same colour.
	 * 
	 * <p>
	 * Defaults to Color.BLACK.
	 * 
	 * @param axisValuesColour the new colour to be used for the axis bar values.
	 */
	public void setAxisValuesColour(Color axisValuesColour) {
		this.axisValuesColour = axisValuesColour;
	}

	/**
	 * Returns whether axis values are to be shown at all for the x-axis.
	 * 
	 * <p>
	 * If axis values are not shown then more space is allocated to the heat 
	 * map.
	 * 
	 * @return true if the x-axis values will be displayed, false otherwise.
	 */
	public boolean isShowXAxisValues() {
		//TODO Could get rid of these flags and use a frequency of -1 to signal no values.
		return showXAxisValues;
	}

	/**
	 * Sets whether axis values are to be shown at all for the x-axis.
	 * 
	 * <p>
	 * If axis values are not shown then more space is allocated to the heat 
	 * map.
	 * 
	 * <p>
	 * Defaults to true.
	 * 
	 * @param showXAxisValues true if x-axis values should be displayed, false 
	 * if they should be hidden.
	 */
	public void setShowXAxisValues(boolean showXAxisValues) {
		this.showXAxisValues = showXAxisValues;
	}

	/**
	 * Returns whether axis values are to be shown at all for the y-axis.
	 * 
	 * <p>
	 * If axis values are not shown then more space is allocated to the heat 
	 * map.
	 * 
	 * @return true if the y-axis values will be displayed, false otherwise.
	 */
	public boolean isShowYAxisValues() {
		return showYAxisValues;
	}

	/**
	 * Sets whether axis values are to be shown at all for the y-axis.
	 * 
	 * <p>
	 * If axis values are not shown then more space is allocated to the heat 
	 * map.
	 * 
	 * <p>
	 * Defaults to true.
	 * 
	 * @param showYAxisValues true if y-axis values should be displayed, false 
	 * if they should be hidden.
	 */
	public void setShowYAxisValues(boolean showYAxisValues) {
		this.showYAxisValues = showYAxisValues;
	}

	/**
	 * Returns the scale that is currently in use to map z-value to colour. A 
	 * value of 1.0 will give a <strong>linear</strong> scale, which will 
	 * spread the distribution of colours evenly amoungst the full range of 
	 * represented z-values. A value of greater than 1.0 will give an 
	 * <strong>exponential</strong> scale that will produce greater emphasis 
	 * for the separation between higher values and a value between 0.0 and 1.0
	 * will provide a <strong>logarithmic</strong> scale, with greater 
	 * separation of low values.
	 *  
	 * @return the scale factor that is being used to map from z-value to colour.
	 */
	public double getColourScale() {
		return colourScale;
	}

	/**
	 * Sets the scale that is currently in use to map z-value to colour. A 
	 * value of 1.0 will give a <strong>linear</strong> scale, which will 
	 * spread the distribution of colours evenly amoungst the full range of 
	 * represented z-values. A value of greater than 1.0 will give an 
	 * <strong>exponential</strong> scale that will produce greater emphasis 
	 * for the separation between higher values and a value between 0.0 and 1.0
	 * will provide a <strong>logarithmic</strong> scale, with greater 
	 * separation of low values. Values of 0.0 or less are illegal.
	 * 
	 * <p>
	 * Defaults to a linear scale value of 1.0.
	 * 
	 * @param colourScale the scale that should be used to map from z-value to 
	 * colour.
	 */
	public void setColourScale(double colourScale) {
		this.colourScale = colourScale;
	}

	/**
	 * Generates a new chart <code>Image</code> based upon the currently held 
	 * settings and then attempts to save that image to disk, to the location 
	 * provided as a File parameter. The image type of the saved file will 
	 * equal the extension of the filename provided, so it is essential that a 
	 * suitable extension be included on the file name.
	 * 
	 * <p>
	 * All supported <code>ImageIO</code> file types are supported, including 
	 * PNG, JPG and GIF.
	 * 
	 * <p>
	 * No chart will be generated until this or the related 
	 * <code>getChartImage()</code> method are called. All successive calls 
	 * will result in the generation of a new chart image, no caching is used.
	 * 
	 * @param outputFile the file location that the generated image file should 
	 * be written to. The File must have a suitable filename, with an extension
	 * of a valid image format (as supported by <code>ImageIO</code>).
	 * @throws IOException if the output file's filename has no extension or 
	 * if there the file is unable to written to. Reasons for this include a 
	 * non-existant file location (check with the File exists() method on the 
	 * parent directory), or the permissions of the write location may be 
	 * incorrect.
	 */
	public void saveToFile(File outputFile) throws IOException {
		String filename = outputFile.getName();

		int extPoint = filename.lastIndexOf('.');

		if (extPoint < 0) {
			throw new IOException("Illegal filename, no extension used.");
		}

		// Determine the extension of the filename.
		String ext = filename.substring(extPoint + 1);

		// Handle jpg without transparency.
		if (ext.toLowerCase().equals("jpg") || ext.toLowerCase().equals("jpeg")) {
			BufferedImage chart = (BufferedImage) getChartImage(false);

			// Save our graphic.
			saveGraphicJpeg(chart, outputFile, 1.0f);
		} else {
			BufferedImage chart = (BufferedImage) getChartImage(true);

			ImageIO.write(chart, ext, outputFile);
		}
	}

	private void saveGraphicJpeg(BufferedImage chart, File outputFile, float quality) throws IOException {
		// Setup correct compression for jpeg.
		Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");
		ImageWriter writer = (ImageWriter) iter.next();
		ImageWriteParam iwp = writer.getDefaultWriteParam();
		iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		iwp.setCompressionQuality(quality);

		// Output the image.
		FileImageOutputStream output = new FileImageOutputStream(outputFile);
		writer.setOutput(output);
		IIOImage image = new IIOImage(chart, null, null);
		writer.write(null, image, iwp);
		writer.dispose();

	}

	/**
	 * Generates and returns a new chart <code>Image</code> configured 
	 * according to this object's currently held settings. The given parameter 
	 * determines whether transparency should be enabled for the generated 
	 * image.
	 * 
	 * <p>
	 * No chart will be generated until this or the related 
	 * <code>saveToFile(File)</code> method are called. All successive calls 
	 * will result in the generation of a new chart image, no caching is used.
	 * 
	 * @param alpha whether to enable transparency.
	 * @return A newly generated chart <code>Image</code>. The returned image 
	 * is a <code>BufferedImage</code>.
	 */
	public Image getChartImage(boolean alpha) {
		// Calculate all unknown dimensions.
		measureComponents();
		updateCoordinates();

		// Determine image type based upon whether require alpha or not.
		// Using BufferedImage.TYPE_INT_ARGB seems to break on jpg.
		int imageType = (alpha ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR);

		// Create our chart image which we will eventually draw everything on.
		BufferedImage chartImage = new BufferedImage(chartSize.width, chartSize.height, imageType);
		Graphics2D chartGraphics = chartImage.createGraphics();

		// Use anti-aliasing where ever possible.
		chartGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
				RenderingHints.VALUE_ANTIALIAS_ON);

		// Set the background.
		chartGraphics.setColor(backgroundColour);
		chartGraphics.fillRect(0, 0, chartSize.width, chartSize.height);

		// Draw the title.
		drawTitle(chartGraphics);

		// Draw the heatmap image.
		drawHeatMap(chartGraphics, zValues);

		// Draw the axis labels.
		drawXLabel(chartGraphics);
		drawYLabel(chartGraphics);

		// Draw the axis bars.
		drawAxisBars(chartGraphics);

		// Draw axis values.
		drawXValues(chartGraphics);
		drawYValues(chartGraphics);

		return chartImage;
	}

	/**
	 * Generates and returns a new chart <code>Image</code> configured 
	 * according to this object's currently held settings. By default the image
	 * is generated with no transparency.
	 * 
	 * <p>
	 * No chart will be generated until this or the related 
	 * <code>saveToFile(File)</code> method are called. All successive calls 
	 * will result in the generation of a new chart image, no caching is used.
	 * 
	 * @return A newly generated chart <code>Image</code>. The returned image 
	 * is a <code>BufferedImage</code>.
	 */
	public Image getChartImage() {
		return getChartImage(false);
	}

	/*
	 * Calculates all unknown component dimensions.
	 */
	private void measureComponents() {
		//TODO This would be a good place to check that all settings have sensible values or throw illegal state exception.

		//TODO Put this somewhere so it only gets created once.
		BufferedImage chartImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D tempGraphics = chartImage.createGraphics();

		// Calculate title dimensions.
		if (title != null) {
			tempGraphics.setFont(titleFont);
			FontMetrics metrics = tempGraphics.getFontMetrics();
			titleSize = new Dimension(metrics.stringWidth(title), metrics.getHeight());
			titleAscent = metrics.getAscent();
		} else {
			titleSize = new Dimension(0, 0);
		}

		// Calculate x-axis label dimensions.
		if (xAxisLabel != null) {
			tempGraphics.setFont(axisLabelsFont);
			FontMetrics metrics = tempGraphics.getFontMetrics();
			xAxisLabelSize = new Dimension(metrics.stringWidth(xAxisLabel), metrics.getHeight());
			xAxisLabelDescent = metrics.getDescent();
		} else {
			xAxisLabelSize = new Dimension(0, 0);
		}

		// Calculate y-axis label dimensions.
		if (yAxisLabel != null) {
			tempGraphics.setFont(axisLabelsFont);
			FontMetrics metrics = tempGraphics.getFontMetrics();
			yAxisLabelSize = new Dimension(metrics.stringWidth(yAxisLabel), metrics.getHeight());
			yAxisLabelAscent = metrics.getAscent();
		} else {
			yAxisLabelSize = new Dimension(0, 0);
		}

		// Calculate x-axis value dimensions.
		if (showXAxisValues) {
			tempGraphics.setFont(axisValuesFont);
			FontMetrics metrics = tempGraphics.getFontMetrics();
			xAxisValuesHeight = metrics.getHeight();
			xAxisValuesWidthMax = 0;
			for (Object o: xValues) {
				int w = metrics.stringWidth(o.toString());
				if (w > xAxisValuesWidthMax) {
					xAxisValuesWidthMax = w;
				}
			}
		} else {
			xAxisValuesHeight = 0;
		}

		// Calculate y-axis value dimensions.
		if (showYAxisValues) {
			tempGraphics.setFont(axisValuesFont);
			FontMetrics metrics = tempGraphics.getFontMetrics();
			yAxisValuesHeight = metrics.getHeight();
			yAxisValuesAscent = metrics.getAscent();
			yAxisValuesWidthMax = 0;
			for (Object o: yValues) {
				int w = metrics.stringWidth(o.toString());
				if (w > yAxisValuesWidthMax) {
					yAxisValuesWidthMax = w;
				}
			}
		} else {
			yAxisValuesHeight = 0;
		}

		// Calculate heatmap dimensions.
		int heatMapWidth = (zValues[0].length * cellSize.width);
		int heatMapHeight = (zValues.length * cellSize.height);
		heatMapSize = new Dimension(heatMapWidth, heatMapHeight);

		int yValuesHorizontalSize = 0;
		if (yValuesHorizontal) {
			yValuesHorizontalSize = yAxisValuesWidthMax;
		} else {
			yValuesHorizontalSize = yAxisValuesHeight;
		}

		int xValuesVerticalSize = 0;
		if (xValuesHorizontal) {
			xValuesVerticalSize = xAxisValuesHeight;
		} else {
			xValuesVerticalSize = xAxisValuesWidthMax;
		}

		// Calculate chart dimensions.
		int chartWidth = heatMapWidth + (2 * margin) + yAxisLabelSize.height + yValuesHorizontalSize + axisThickness;
		int chartHeight = heatMapHeight + (2 * margin) + xAxisLabelSize.height + xValuesVerticalSize + titleSize.height + axisThickness;
		chartSize = new Dimension(chartWidth, chartHeight);
	}

	/*
	 * Calculates the co-ordinates of some key positions.
	 */
	private void updateCoordinates() {
		// Top-left of heat map.
		int x = margin + axisThickness + yAxisLabelSize.height;
		x += (yValuesHorizontal ? yAxisValuesWidthMax : yAxisValuesHeight);
		int y = titleSize.height + margin;
		heatMapTL = new Point(x, y);

		// Top-right of heat map.
		x = heatMapTL.x + heatMapSize.width;
		y = heatMapTL.y + heatMapSize.height;
		heatMapBR = new Point(x, y);

		// Centre of heat map.
		x = heatMapTL.x + (heatMapSize.width / 2);
		y = heatMapTL.y + (heatMapSize.height / 2);
		heatMapC = new Point(x, y);
	}

	/*
	 * Draws the title String on the chart if title is not null.
	 */
	private void drawTitle(Graphics2D chartGraphics) {
		if (title != null) {			
			// Strings are drawn from the baseline position of the leftmost char.
			int yTitle = (margin/2) + titleAscent;
			int xTitle = (chartSize.width/2) - (titleSize.width/2);

			chartGraphics.setFont(titleFont);
			chartGraphics.setColor(titleColour);
			chartGraphics.drawString(title, xTitle, yTitle);
		}
	}

	/*
	 * Creates the actual heatmap element as an image, that can then be drawn 
	 * onto a chart.
	 */
	private void drawHeatMap(Graphics2D chartGraphics, double[][] data) {
		// Calculate the available size for the heatmap.
		int noYCells = data.length;
		int noXCells = data[0].length;

		//double dataMin = min(data);
		//double dataMax = max(data);

		BufferedImage heatMapImage = new BufferedImage(heatMapSize.width, heatMapSize.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D heatMapGraphics = heatMapImage.createGraphics();

		for (int x=0; x<noXCells; x++) {
			for (int y=0; y<noYCells; y++) {
				// Set colour depending on zValues.
				heatMapGraphics.setColor(getColor(data[y][x]));

				int cellX = x*cellSize.width;
				int cellY = y*cellSize.height;

				heatMapGraphics.fillRect(cellX, cellY, cellSize.width, cellSize.height);
			}
		}

		// Draw the heat map onto the chart.
		chartGraphics.drawImage(heatMapImage, heatMapTL.x, heatMapTL.y, heatMapSize.width, heatMapSize.height, null);
	}

	/*
	 * Draws the x-axis label string if it is not null.
	 */
	private void drawXLabel(Graphics2D chartGraphics) {
		if (xAxisLabel != null) {
			// Strings are drawn from the baseline position of the leftmost char.
			int yPosXAxisLabel = chartSize.height - (margin / 2) - xAxisLabelDescent;
			//TODO This will need to be updated if the y-axis values/label can be moved to the right.
			int xPosXAxisLabel = heatMapC.x - (xAxisLabelSize.width / 2);

			chartGraphics.setFont(axisLabelsFont);
			chartGraphics.setColor(axisLabelColour);
			chartGraphics.drawString(xAxisLabel, xPosXAxisLabel, yPosXAxisLabel);
		}
	}

	/*
	 * Draws the y-axis label string if it is not null.
	 */
	private void drawYLabel(Graphics2D chartGraphics) {
		if (yAxisLabel != null) {
			// Strings are drawn from the baseline position of the leftmost char.
			int yPosYAxisLabel = heatMapC.y + (yAxisLabelSize.width / 2);
			int xPosYAxisLabel = (margin / 2) + yAxisLabelAscent;

			chartGraphics.setFont(axisLabelsFont);
			chartGraphics.setColor(axisLabelColour);

			// Create 270 degree rotated transform.
			AffineTransform transform = chartGraphics.getTransform();
			AffineTransform originalTransform = (AffineTransform) transform.clone();
			transform.rotate(Math.toRadians(270), xPosYAxisLabel, yPosYAxisLabel);
			chartGraphics.setTransform(transform);

			// Draw string.
			chartGraphics.drawString(yAxisLabel, xPosYAxisLabel, yPosYAxisLabel);

			// Revert to original transform before rotation.
			chartGraphics.setTransform(originalTransform);
		}
	}

	/*
	 * Draws the bars of the x-axis and y-axis.
	 */
	private void drawAxisBars(Graphics2D chartGraphics) {
		if (axisThickness > 0) {
			chartGraphics.setColor(axisColour);

			// Draw x-axis.
			int x = heatMapTL.x - axisThickness;
			int y = heatMapBR.y;
			int width = heatMapSize.width + axisThickness;
			int height = axisThickness;
			chartGraphics.fillRect(x, y, width, height);

			// Draw y-axis.
			x = heatMapTL.x - axisThickness;
			y = heatMapTL.y;
			width = axisThickness;
			height = heatMapSize.height;
			chartGraphics.fillRect(x, y, width, height);
		}
	}

	/*
	 * Draws the x-values onto the x-axis if showXAxisValues is set to true.
	 */
	private void drawXValues(Graphics2D chartGraphics) {
		if (!showXAxisValues) {
			return;
		}

		chartGraphics.setColor(axisValuesColour);

		for (int i=0; i<xValues.length; i++) {
			if (i % xAxisValuesFrequency != 0) {
				continue;
			}

			String xValueStr = xValues[i].toString();

			chartGraphics.setFont(axisValuesFont);
			FontMetrics metrics = chartGraphics.getFontMetrics();

			int valueWidth = metrics.stringWidth(xValueStr);

			if (xValuesHorizontal) {
				// Draw the value with whatever font is now set.
				int valueXPos = (i * cellSize.width) + ((cellSize.width / 2) - (valueWidth / 2));
				valueXPos += heatMapTL.x;
				int valueYPos = heatMapBR.y + metrics.getAscent() + 1;

				chartGraphics.drawString(xValueStr, valueXPos, valueYPos);
			} else {
				int valueXPos = heatMapTL.x + (i * cellSize.width) + ((cellSize.width / 2) + (xAxisValuesHeight / 2));
				int valueYPos = heatMapBR.y + axisThickness + valueWidth;

				// Create 270 degree rotated transform.
				AffineTransform transform = chartGraphics.getTransform();
				AffineTransform originalTransform = (AffineTransform) transform.clone();
				transform.rotate(Math.toRadians(270), valueXPos, valueYPos);
				chartGraphics.setTransform(transform);

				// Draw the string.
				chartGraphics.drawString(xValueStr, valueXPos, valueYPos);

				// Revert to original transform before rotation.
				chartGraphics.setTransform(originalTransform);
			}
		}
	}

	/*
	 * Draws the y-values onto the y-axis if showYAxisValues is set to true.
	 */
	private void drawYValues(Graphics2D chartGraphics) {
		if (!showYAxisValues) {
			return;
		}

		chartGraphics.setColor(axisValuesColour);

		for (int i=0; i<yValues.length; i++) {
			if (i % yAxisValuesFrequency != 0) {
				continue;
			}

			String yValueStr = yValues[i].toString();

			chartGraphics.setFont(axisValuesFont);
			FontMetrics metrics = chartGraphics.getFontMetrics();

			int valueWidth = metrics.stringWidth(yValueStr);

			if (yValuesHorizontal) {
				// Draw the value with whatever font is now set.
				int valueXPos = margin + yAxisLabelSize.height + (yAxisValuesWidthMax - valueWidth);
				int valueYPos = heatMapTL.y + (i * cellSize.height) + (cellSize.height/2) + (yAxisValuesAscent/2);

				chartGraphics.drawString(yValueStr, valueXPos, valueYPos);
			} else {
				int valueXPos = margin + yAxisLabelSize.height + yAxisValuesAscent;
				int valueYPos = heatMapTL.y + (i * cellSize.height) + (cellSize.height/2) + (valueWidth/2);

				// Create 270 degree rotated transform.
				AffineTransform transform = chartGraphics.getTransform();
				AffineTransform originalTransform = (AffineTransform) transform.clone();
				transform.rotate(Math.toRadians(270), valueXPos, valueYPos);
				chartGraphics.setTransform(transform);

				// Draw the string.
				chartGraphics.drawString(yValueStr, valueXPos, valueYPos);

				// Revert to original transform before rotation.
				chartGraphics.setTransform(originalTransform);
			}
		}
	}

	private static Color getColor(double sim)
	{
		Color color;
		if(sim > 0.9)
			color = Color.decode("#FF4400");
		else if(sim > 0.8)
			color = Color.decode("#FF8800");
		else if(sim > 0.7)
			color = Color.decode("#FFAA00");
		else if(sim > 0.6)
			color = Color.decode("#FFCC00");
		else if(sim > 0.5)
			color = Color.decode("#FFEE00");
		else if(sim > 0.4)
			color = Color.decode("#FFFF66");
		else if(sim > 0.3)
			color = Color.decode("#FFFF88");
		else if(sim > 0.2)
			color = Color.decode("#FFFFAA");
		else if(sim > 0.1)
			color = Color.decode("#FFFFCC");
		else
			color = Color.decode("#FFFFFF");
		return color;
	}
}

