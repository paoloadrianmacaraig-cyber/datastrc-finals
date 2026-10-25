// Author: Tengonciang
// Jersey and shorts sizes, smallest to largest. ordinal() is used as an array index.
public enum Size {
    XS, S, M, L, XL, XXL, XXXL;

    // returns null if the text is not a valid size
    public static Size fromText(String text) {
        for (Size s : values()) {
            if (s.name().equalsIgnoreCase(text.trim())) {
                return s;
            }
        }
        return null;
    }
}
