package pdftk.com.lowagie.text.pdf;
public class GrayColor extends ExtendedColor {
    private static final long serialVersionUID = -6571835680819282746L;
    float gray;
    public GrayColor(int intGray) {
        this((float)intGray / 255f);
    }
    public GrayColor(float floatGray) {
        super(TYPE_GRAY, floatGray, floatGray, floatGray);
        gray = normalize(floatGray);
    }
    public float getGray() {
        return gray;
    }
}