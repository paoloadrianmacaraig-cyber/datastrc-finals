// Author: Macaraig
public enum ItemType {
    JERSEY_ONLY("Jersey only"),
    JERSEY_SET("Jersey + shorts set");

    private final String label;

    ItemType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
