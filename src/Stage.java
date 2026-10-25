// Author: Macaraig
// Production stages at the main branch, in order.
public enum Stage {
    NEW, RESIZE, PRINT, PRESS, CUTTING, SEWING, PACKING;

    // once printing starts, the jerseys can no longer be changed or cancelled
    public boolean isBeforePrinting() {
        return this.ordinal() < PRINT.ordinal();
    }

    // how the stage is shown on screen, e.g. CUTTING -> "Cutting"
    public String label() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
