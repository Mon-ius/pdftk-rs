package pdftk.com.lowagie.text;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;
public class Rectangle implements Element, MarkupAttributes {
	public static final int UNDEFINED = -1;
	public static final int TOP = 1;
	public static final int BOTTOM = 2;
	public static final int LEFT = 4;
	public static final int RIGHT = 8;
	public static final int NO_BORDER = 0;
	public static final int BOX = TOP + BOTTOM + LEFT + RIGHT;
	protected float llx;
	protected float lly;
	protected float urx;
	protected float ury;
	protected int border = UNDEFINED;
	protected float borderWidth = UNDEFINED;
	protected Color color = null;
	protected Color borderColorLeft = null;
	protected Color borderColorRight = null;
	protected Color borderColorTop = null;
	protected Color borderColorBottom = null;
	protected float borderWidthLeft = UNDEFINED;
	protected float borderWidthRight = UNDEFINED;
	protected float borderWidthTop = UNDEFINED;
	protected float borderWidthBottom = UNDEFINED;
	protected boolean useVariableBorders = false;
	protected Color background = null;
	protected float grayFill = 0;
	protected int rotation = 0;
	protected Properties markupAttributes;
	public Rectangle(float llx, float lly, float urx, float ury) {
		this.llx = llx;
		this.lly = lly;
		this.urx = urx;
		this.ury = ury;
	}
	public Rectangle(float urx, float ury) {
		this(0, 0, urx, ury);
	}
	public Rectangle(Rectangle rect) {
		this(rect.llx, rect.lly, rect.urx, rect.ury);
		cloneNonPositionParameters(rect);
	}
	public void cloneNonPositionParameters(Rectangle rect) {
		this.rotation = rect.rotation;
		this.border = rect.border;
		this.borderWidth = rect.borderWidth;
		this.color = rect.color;
		this.background = rect.background;
		this.grayFill = rect.grayFill;
		this.borderColorLeft = rect.borderColorLeft;
		this.borderColorRight = rect.borderColorRight;
		this.borderColorTop = rect.borderColorTop;
		this.borderColorBottom = rect.borderColorBottom;
		this.borderWidthLeft = rect.borderWidthLeft;
		this.borderWidthRight = rect.borderWidthRight;
		this.borderWidthTop = rect.borderWidthTop;
		this.borderWidthBottom = rect.borderWidthBottom;
		this.useVariableBorders = rect.useVariableBorders;
	}
	public void softCloneNonPositionParameters(Rectangle rect) {
		if (rect.rotation != 0)
			this.rotation = rect.rotation;
		if (rect.border != UNDEFINED)
			this.border = rect.border;
		if (rect.borderWidth != UNDEFINED)
			this.borderWidth = rect.borderWidth;
		if (rect.color != null)
			this.color = rect.color;
		if (rect.background != null)
			this.background = rect.background;
		if (rect.grayFill != 0)
			this.grayFill = rect.grayFill;
		if (rect.borderColorLeft != null)
			this.borderColorLeft = rect.borderColorLeft;
		if (rect.borderColorRight != null)
			this.borderColorRight = rect.borderColorRight;
		if (rect.borderColorTop != null)
			this.borderColorTop = rect.borderColorTop;
		if (rect.borderColorBottom != null)
			this.borderColorBottom = rect.borderColorBottom;
		if (rect.borderWidthLeft != UNDEFINED)
			this.borderWidthLeft = rect.borderWidthLeft;
		if (rect.borderWidthRight != UNDEFINED)
			this.borderWidthRight = rect.borderWidthRight;
		if (rect.borderWidthTop != UNDEFINED)
			this.borderWidthTop = rect.borderWidthTop;
		if (rect.borderWidthBottom != UNDEFINED)
			this.borderWidthBottom = rect.borderWidthBottom;
		if (useVariableBorders)
			this.useVariableBorders = rect.useVariableBorders;
	}
	public boolean process(ElementListener listener) {
		try {
			return listener.add(this);
		} catch (DocumentException de) {
			return false;
		}
	}
	public int type() {
		return Element.RECTANGLE;
	}
	public ArrayList getChunks() {
		return new ArrayList();
	}
	public void normalize() {
		if (llx > urx) {
			float a = llx;
			llx = urx;
			urx = a;
		}
		if (lly > ury) {
			float a = lly;
			lly = ury;
			ury = a;
		}
	}
	public Rectangle rectangle(float top, float bottom) {
		Rectangle tmp = new Rectangle(this);
		if (top() > top) {
			tmp.setTop(top);
			tmp.setBorder(border - (border & TOP));
		}
		if (bottom() < bottom) {
			tmp.setBottom(bottom);
			tmp.setBorder(border - (border & BOTTOM));
		}
		return tmp;
	}
	public Rectangle rotate() {
		Rectangle rect = new Rectangle(lly, llx, ury, urx);
		rect.rotation = rotation + 90;
		rect.rotation %= 360;
		return rect;
	}
	public void setLeft(float value) {
		llx = value;
	}
	public void setRight(float value) {
		urx = value;
	}
	public void setTop(float value) {
		ury = value;
	}
	public void setBottom(float value) {
		lly = value;
	}
	public void setBorder(int value) {
		border = value;
	}
	public void enableBorderSide(int side) {
		if (border == UNDEFINED) {
			border = 0;
		}
		border |= side;
	}
	public void disableBorderSide(int side) {
		if (border == UNDEFINED) {
			border = 0;
		}
		border &= ~side;
	}
	public void setBorderWidth(float value) {
		borderWidth = value;
	}
	public void setBorderColor(Color value) {
		color = value;
	}
	public void setBorderColorRight(Color value) {
		borderColorRight = value;
	}
	public void setBorderColorLeft(Color value) {
		borderColorLeft = value;
	}
	public void setBorderColorTop(Color value) {
		borderColorTop = value;
	}
	public void setBorderColorBottom(Color value) {
		borderColorBottom = value;
	}
	public void setBackgroundColor(Color value) {
		background = value;
	}
	public void setGrayFill(float value) {
		if (value >= 0 && value <= 1.0) {
			grayFill = value;
		}
	}
	public float left() {
		return llx;
	}
	public float right() {
		return urx;
	}
	public float top() {
		return ury;
	}
	public float bottom() {
		return lly;
	}
	public float left(float margin) {
		return llx + margin;
	}
	public float right(float margin) {
		return urx - margin;
	}
	public float top(float margin) {
		return ury - margin;
	}
	public float bottom(float margin) {
		return lly + margin;
	}
	public float width() {
		return urx - llx;
	}
	public float height() {
		return ury - lly;
	}
	public boolean hasBorders() {
		return (border > 0)
				&& ((borderWidth > 0) || (borderWidthLeft > 0)
						|| (borderWidthRight > 0) || (borderWidthTop > 0) || (borderWidthBottom > 0));
	}
	public boolean hasBorder(int type) {
		return border != UNDEFINED && (border & type) == type;
	}
	public int border() {
		return border;
	}
	public float borderWidth() {
		return borderWidth;
	}
	public Color borderColor() {
		return color;
	}
	public Color backgroundColor() {
		return background;
	}
	public float grayFill() {
		return grayFill;
	}
	public int getRotation() {
		return rotation;
	}
	public void setMarkupAttribute(String name, String value) {
		if (markupAttributes == null)
			markupAttributes = new Properties();
		markupAttributes.put(name, value);
	}
	public void setMarkupAttributes(Properties markupAttributes) {
		this.markupAttributes = markupAttributes;
	}
	public String getMarkupAttribute(String name) {
		return (markupAttributes == null) ? null : String
				.valueOf(markupAttributes.get(name));
	}
	public Set getMarkupAttributeNames() {
		return Chunk.getKeySet(markupAttributes);
	}
	public Properties getMarkupAttributes() {
		return markupAttributes;
	}
	public Color getBorderColorLeft() {
		return borderColorLeft;
	}
	public Color getBorderColorRight() {
		return borderColorRight;
	}
	public Color getBorderColorTop() {
		return borderColorTop;
	}
	public Color getBorderColorBottom() {
		return borderColorBottom;
	}
	public float getBorderWidthLeft() {
		return getVariableBorderWidth(borderWidthLeft, LEFT);
	}
	public void setBorderWidthLeft(float borderWidthLeft) {
		this.borderWidthLeft = borderWidthLeft;
		updateBorderBasedOnWidth(borderWidthLeft, LEFT);
	}
	public float getBorderWidthRight() {
		return getVariableBorderWidth(borderWidthRight, RIGHT);
	}
	public void setBorderWidthRight(float borderWidthRight) {
		this.borderWidthRight = borderWidthRight;
		updateBorderBasedOnWidth(borderWidthRight, RIGHT);
	}
	public float getBorderWidthTop() {
		return getVariableBorderWidth(borderWidthTop, TOP);
	}
	public void setBorderWidthTop(float borderWidthTop) {
		this.borderWidthTop = borderWidthTop;
		updateBorderBasedOnWidth(borderWidthTop, TOP);
	}
	public float getBorderWidthBottom() {
		return getVariableBorderWidth(borderWidthBottom, BOTTOM);
	}
	public void setBorderWidthBottom(float borderWidthBottom) {
		this.borderWidthBottom = borderWidthBottom;
		updateBorderBasedOnWidth(borderWidthBottom, BOTTOM);
	}
	private void updateBorderBasedOnWidth(float width, int side) {
		useVariableBorders = true;
		if (width > 0) {
			enableBorderSide(side);
		} else {
			disableBorderSide(side);
		}
	}
	private float getVariableBorderWidth(float variableWidthValue, int side) {
		if ((border & side) != 0) {
			return variableWidthValue != UNDEFINED ? variableWidthValue
					: borderWidth;
		} else {
			return 0;
		}
	}
	public boolean isUseVariableBorders() {
		return useVariableBorders;
	}
	public void setUseVariableBorders(boolean useVariableBorders) {
		this.useVariableBorders = useVariableBorders;
	}
	public String toString() {
		StringBuffer buf = new StringBuffer("Rectangle: ");
		buf.append(width());
		buf.append("x");
		buf.append(height());
		buf.append(" (rot: ");
		buf.append(rotation);
		buf.append(" degrees)");
		return buf.toString();
	}
}