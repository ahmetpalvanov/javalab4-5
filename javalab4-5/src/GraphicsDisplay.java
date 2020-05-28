import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public class GraphicsDisplay extends JPanel {

	private ArrayList<Double[]> graphicsData;
	private int selectedMarker = -1;
	private double minX;
	private double maxX;
	private double minY;
	private double maxY;
	
	private double scaleX;
	private double scaleY;
	
	private double[][] viewport = new double[2][2];
	private boolean showAxis = true;
	private boolean showMarkers = true;


	private Font axisFont;
	private Font labelsFont;
	
	private BasicStroke axisStroke;
	private BasicStroke markerStroke;
	private BasicStroke selectionStroke;
	private static DecimalFormat formatter=(DecimalFormat)NumberFormat.getInstance();
	
	private boolean scaleMode = false;
	private double[] originalPoint = new double[2];
	private Rectangle2D.Double selectionRect = new Rectangle2D.Double();
	
	public GraphicsDisplay ()	{
		
		setBackground(Color.WHITE);
		
		markerStroke = new BasicStroke(6.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, null, 0.0f);
		axisStroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f, null, 0.0f);
		selectionStroke = new BasicStroke(1.0F, 0, 0, 10.0F, new float[] { 10, 10 }, 0.0F);				
		axisFont = new Font("Serif", Font.BOLD, 36);
		labelsFont = new java.awt.Font("Serif",0,20);
		addMouseMotionListener(new MouseMotionHandler());
		addMouseListener(new MouseHandler());
	}
	
	public void showGraphics(ArrayList<Double[]> graphicsData)	{
		this.graphicsData = graphicsData;
	    this.minX = ((Double[])graphicsData.get(0))[0].doubleValue();
	    this.maxX = ((Double[])graphicsData.get(graphicsData.size() - 1))[0].doubleValue();
	    this.minY = ((Double[])graphicsData.get(0))[1].doubleValue();
	    this.maxY = this.minY;
		
	    for (int i = 1; i < graphicsData.size(); i++) {
	        if (((Double[])graphicsData.get(i))[1].doubleValue() < this.minY) {
	          this.minY = ((Double[])graphicsData.get(i))[1].doubleValue();
	        }
	        if (((Double[])graphicsData.get(i))[1].doubleValue() > this.maxY) {
	          this.maxY = ((Double[])graphicsData.get(i))[1].doubleValue();
	        }
	    }
		
		zoomToRegion(minX, maxY, maxX, minY);
		
	}
	
	public void zoomToRegion(double x1,double y1,double x2,double y2)	{
		this.viewport[0][0]=x1;
		this.viewport[0][1]=y1;
		this.viewport[1][0]=x2;
		this.viewport[1][1]=y2;
		this.repaint();
	}
	public void setShowAxis(boolean showAxis) {
		this.showAxis = showAxis;
		repaint();
	}

	public void setShowMarkers(boolean showMarkers) {
		this.showMarkers = showMarkers;
		repaint();
	}
	
	protected Point2D.Double xyToPoint(double x, double y) {
		double deltaX = x - viewport[0][0];
		double deltaY = viewport[0][1] - y;
		return new Point2D.Double(deltaX*scaleX, deltaY*scaleY);
	}
	 
	protected double[] translatePointToXY(int x, int y)
	  {
	    return new double[] { this.viewport[0][0] + x / this.scaleX, this.viewport[0][1] - y / this.scaleY };
	  }
		
	protected Point2D.Double shiftPoint(Point2D.Double src, double deltaX, double deltaY) {
		Point2D.Double dest = new Point2D.Double();
		dest.setLocation(src.getX() + deltaX, src.getY() + deltaY);
		return dest;
	}
	
	protected void paintGraphics (Graphics2D canvas) {
		canvas.setStroke(this.markerStroke);
	    canvas.setColor(Color.RED);
	    // Линии
	    Double currentX = null;
	    Double currentY = null;
	    for (Double[] point : this.graphicsData)
	    {
	      if ((point[0].doubleValue() >= this.viewport[0][0]) && (point[1].doubleValue() <= this.viewport[0][1]) && 
	        (point[0].doubleValue() <= this.viewport[1][0]) && (point[1].doubleValue() >= this.viewport[1][1]))
	      {
	        if ((currentX != null) && (currentY != null)) {
	          canvas.draw(new Line2D.Double(xyToPoint(currentX.doubleValue(), currentY.doubleValue()), 
	            xyToPoint(point[0].doubleValue(), point[1].doubleValue())));
	        }
	        currentX = point[0];
	        currentY = point[1];
	      }
	    }
	}
	
	protected void paintAxis(Graphics2D canvas){
		// Оси
		canvas.setStroke(this.axisStroke);
		canvas.setColor(java.awt.Color.BLACK);
		canvas.setFont(this.axisFont);
		FontRenderContext context=canvas.getFontRenderContext();
		if (!(viewport[0][0] > 0|| viewport[1][0] < 0)){
			canvas.draw(new Line2D.Double(xyToPoint(0, viewport[0][1]),
					xyToPoint(0, viewport[1][1])));
			canvas.draw(new Line2D.Double(xyToPoint(-(viewport[1][0] - viewport[0][0]) * 0.0025,
					viewport[0][1] - (viewport[0][1] - viewport[1][1]) * 0.015),xyToPoint(0,viewport[0][1])));
			canvas.draw(new Line2D.Double(xyToPoint((viewport[1][0] - viewport[0][0]) * 0.0025,
					viewport[0][1] - (viewport[0][1] - viewport[1][1]) * 0.015),
					xyToPoint(0, viewport[0][1])));
			Rectangle2D bounds = axisFont.getStringBounds("y",context);
			Point2D.Double labelPos = xyToPoint(0.0, viewport[0][1]);
			canvas.drawString("y",(float)labelPos.x + 10,(float)(labelPos.y + bounds.getHeight() / 2));
			}
		if (!(viewport[1][1] > 0.0D || viewport[0][1] < 0.0D)){
			canvas.draw(new Line2D.Double(xyToPoint(viewport[0][0],0),
					xyToPoint(viewport[1][0],0)));
			canvas.draw(new Line2D.Double(xyToPoint(viewport[1][0] - (viewport[1][0] - viewport[0][0]) * 0,
					(viewport[0][1] - viewport[1][1]) * 0.005), xyToPoint(viewport[1][0], 0)));
			canvas.draw(new Line2D.Double(xyToPoint(viewport[1][0] - (viewport[1][0] - viewport[0][0]) * 0.01,
					-(viewport[0][1] - viewport[1][1]) * 0.005), xyToPoint(viewport[1][0], 0)));
			Rectangle2D bounds = axisFont.getStringBounds("x",context);
			Point2D.Double labelPos = xyToPoint(this.viewport[1][0],0.0D);
			canvas.drawString("x",(float)(labelPos.x - bounds.getWidth() - 10),(float)(labelPos.y - bounds.getHeight() / 2));
			}
	}
	
	protected void paintMarkers(Graphics2D canvas) {
		
		for(Double [] point : graphicsData)
		{	canvas.setStroke(markerStroke);
		canvas.setColor(Color.BLUE); 
		canvas.setPaint(Color.BLUE);
		if(uslowie(point[1]))
		{
			canvas.setColor(Color.BLACK); 
			canvas.setPaint(Color.BLACK);
		}
		GeneralPath path = new GeneralPath();
		Point2D.Double center = xyToPoint(point[0],point[1]);
		path.moveTo(center.getX(), center.getY());
		path.lineTo(center.getX()-5, center.getY()+7);
		path.lineTo(center.getX()+5, center.getY()+7);
		path.closePath();
		canvas.draw(path);
	
		}
		
	}
	boolean uslowie(double point)
	{
		int value = (int) point;
		boolean temp = false;
		for(int i = 0 ; i<20 ; i++)
		{
			if(value==i*i)
				{
				temp = true;
				break;
				}
			
		}
		return temp;
	}
	
	private void paintLabels(Graphics2D canvas){
		canvas.setColor(Color.BLACK);
		canvas.setFont(this.labelsFont);
		FontRenderContext context=canvas.getFontRenderContext();
		if (selectedMarker >= 0)
	    {
	      Point2D.Double point = xyToPoint(((Double[])graphicsData.get(selectedMarker))[0].doubleValue(), 
	    		  ((Double[])graphicsData.get(selectedMarker))[1].doubleValue());
	      String label = "X=" + formatter.format(((Double[])graphicsData.get(selectedMarker))[0]) + 
	    		  ", Y=" + formatter.format(((Double[])graphicsData.get(selectedMarker))[1]);
	      Rectangle2D bounds = labelsFont.getStringBounds(label, context);
	      canvas.setColor(Color.BLACK);
	      canvas.drawString(label, (float)(point.getX() + 5.0D), (float)(point.getY() - bounds.getHeight()));
	    }
	}
	
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		scaleX=this.getSize().getWidth() / (this.viewport[1][0] - this.viewport[0][0]);
		scaleY=this.getSize().getHeight() / (this.viewport[0][1] - this.viewport[1][1]);
		if ((this.graphicsData == null) || (this.graphicsData.size() == 0)) return;
		Graphics2D canvas = (Graphics2D) g;
		Stroke oldStroke = canvas.getStroke();
		Color oldColor = canvas.getColor();
		Font oldFont = canvas.getFont();
		Paint oldPaint = canvas.getPaint();
		if (showAxis) 
			{paintAxis(canvas);
			
			}
		
		paintGraphics(canvas);
		paintLabels(canvas);
		if (showMarkers) 
			paintMarkers(canvas);
		paintSelection(canvas);
		canvas.setFont(oldFont);
		canvas.setPaint(oldPaint);
		canvas.setColor(oldColor);
		canvas.setStroke(oldStroke);
	}
	
	private void paintSelection(Graphics2D canvas) {
	    if (!scaleMode) return;
	    canvas.setStroke(selectionStroke);
	    canvas.setColor(Color.BLACK);
	    canvas.draw(selectionRect);
	  }

	//Приближаем
	protected int findSelectedPoint(int x, int y)
	  {
	    if (graphicsData == null) return -1;
	    int pos = 0;
	    for (Double[] point : graphicsData) {
	      Point2D.Double screenPoint = xyToPoint(point[0].doubleValue(), point[1].doubleValue());
	      double distance = (screenPoint.getX() - x) * (screenPoint.getX() - x) + (screenPoint.getY() - y) * (screenPoint.getY() - y);
	      if (distance < 100) return pos;
	      pos++;
	    }	    return -1;
	  }
	
	 public class MouseHandler extends MouseAdapter {
		   
		    public void mouseClicked(MouseEvent ev) {
		    	if(ev.getButton()==3)
		    	{   
		    		zoomToRegion(minX, maxY, maxX, minY);
		    		repaint();
		    	}
		    }

		    public void mousePressed(MouseEvent ev) {
		      if (ev.getButton() != 1) return;
		      originalPoint = translatePointToXY(ev.getX(), ev.getY());
		        scaleMode = true;
		        setCursor(Cursor.getPredefinedCursor(5));
		        selectionRect.setFrame(ev.getX(), ev.getY(), 1.0D, 1.0D);
		      
		    }

		    public void mouseReleased(MouseEvent ev) {
		      if (ev.getButton() != 1) return;
		        scaleMode = false;
		        double[] finalPoint = translatePointToXY(ev.getX(), ev.getY());
		        zoomToRegion(originalPoint[0], originalPoint[1], finalPoint[0], finalPoint[1]);
		        repaint();
		      
		    }
		  }
	 
	 // Оброботчик движения мыши
	 public class MouseMotionHandler implements MouseMotionListener {
	
		 public void mouseDragged(MouseEvent ev) {
			if(true) {
				 double width = ev.getX() - selectionRect.getX();
				 if (width < 5.0D) {
					 width = 5.0D;
				 }
	        double height = ev.getY() - selectionRect.getY();
	        if (height < 5.0D) {
	          height = 5.0D;
	        }
	        selectionRect.setFrame(selectionRect.getX(), selectionRect.getY(), width, height);
	        repaint();
	      }
	}
		 //перемещения мыши
	public void mouseMoved(MouseEvent ev) {
		selectedMarker = findSelectedPoint(ev.getX(), ev.getY());	      
	    	  setCursor(Cursor.getPredefinedCursor(0));
	      repaint();
	}
	
}
	 
}
