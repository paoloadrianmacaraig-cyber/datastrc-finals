// Author: Macaraig
// Where a job order is in its life cycle.
public enum Status {
    DRAFT,          // still being filled in at the branch
    IN_LINE,        // endorsed to the main branch, waiting or being made
    FOR_RELEASE,    // finished jerseys are back at the branch
    RELEASED,       // claimed by the team
    CANCELLED;

    // how the status is shown on screen, e.g. FOR_RELEASE -> "For release"
    public String label() {
        String text = name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
