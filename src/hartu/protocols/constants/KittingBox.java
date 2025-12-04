package hartu.protocols.constants;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class KittingBox {

    /**
     * Defines the two types of kitting boxes (A and B), each implying a different
     * physical layout and thus a different robot trajectory.
     */
    public enum BoxType {
        TYPE_A,
        TYPE_B
    }

    // --- Box Properties ---
    private final String id;
    private final BoxType type;
    private final String referenceFrame;
    private final Map<Integer, Section> sections; // Key is section index (0-3)

    // --- Piece ID Constants ---
    public static final String WORKPIECE_ID_1 = "WP1";
    public static final String WORKPIECE_ID_2 = "WP2";
    public static final String WORKPIECE_ID_3 = "WP3";

    /**
     * Represents a single section within the KittingBox.
     */
    public static class Section {
        private boolean isFilled;
        private final String allowedWorkpieceId;

        /**
         * Initializes an empty section.
         * @param allowedWorkpieceId The specific workpiece ID this section is designed to hold.
         */
        public Section(String allowedWorkpieceId) {
            this.allowedWorkpieceId = allowedWorkpieceId;
            this.isFilled = false; // Starts empty
        }

        // --- Getters ---
        public boolean isFilled() {
            return isFilled;
        }

        public String getAllowedWorkpieceId() {
            return allowedWorkpieceId;
        }

        // --- Setters/Mutators ---
        public boolean fill(String workpieceId) {
            if (Objects.equals(workpieceId, allowedWorkpieceId) && !isFilled) {
                this.isFilled = true;
                return true; // Successfully filled
            }
            return false; // Workpiece ID mismatch or already filled
        }

        public boolean empty() {
            if (isFilled) {
                this.isFilled = false;
                return true; // Successfully emptied
            }
            return false; // Already empty
        }
    }

    /**
     * Constructs a KittingBox with a generated ID, specified type, and reference frame.
     * The sections are initialized based on the capacity requirements:
     * - Two sections for WP1
     * - Two sections for WP2 OR WP3 (We'll use WP2 for the allowed ID for simplicity)
     *
     * @param type The BoxType (A or B).
     * @param referenceFrame The reference coordinate frame for the box.
     */
    public KittingBox(BoxType type, String referenceFrame) {
        this.id = UUID.randomUUID().toString(); // Generate a unique ID
        this.type = type;
        this.referenceFrame = referenceFrame;
        this.sections = new HashMap<>();

        // Initialize the 4 sections based on capacity requirements
        // Capacity: 2 x WP1, 2 x (WP2 or WP3)
        sections.put(0, new Section(WORKPIECE_ID_1)); // Section 1: WP1
        sections.put(1, new Section(WORKPIECE_ID_1)); // Section 2: WP1
        sections.put(2, new Section(WORKPIECE_ID_2)); // Section 3: WP2/WP3
        sections.put(3, new Section(WORKPIECE_ID_2)); // Section 4: WP2/WP3
    }

    // --- Public Getters ---

    public String getId() {
        return id;
    }

    public BoxType getType() {
        return type;
    }

    public String getReferenceFrame() {
        return referenceFrame;
    }

    public Section getSection(int index) {
        return sections.get(index);
    }

    // --- Type-Specific Trajectory Methods ---

    /**
     * Calculates the robot's target pose (position and orientation) to place/pick
     * a workpiece in the specified section, based on the box type.
     *
     * @param sectionIndex The index of the section (0-3).
     * @return A string representing the robot's target pose.
     */
    public String calculateRobotTrajectory(int sectionIndex) {
        String basePose = String.format("FRAME:%s_SECTION:%d", referenceFrame, sectionIndex);

        //

        // The logic for the trajectory differs significantly based on the box type.
        switch (type) {
            case TYPE_A:
                // Type A has sections in a linear arrangement (example logic)
                return basePose + "_LINEAR_TRAJ";
            case TYPE_B:
                // Type B has sections in a square arrangement (example logic)
                return basePose + "_SQUARE_TRAJ";
            default:
                throw new IllegalStateException("Unknown Box Type: " + type);
        }
    }

    // --- Example Usage/Utility Method ---

    /**
     * Finds the index of the first empty section that allows the specified workpiece ID.
     * @param workpieceId The ID of the workpiece to be placed.
     * @return The index (0-3) of an empty, compatible section, or -1 if none is found.
     */
    public int findEmptySectionForWorkpiece(String workpieceId) {
        for (Map.Entry<Integer, Section> entry : sections.entrySet()) {
            int index = entry.getKey();
            Section section = entry.getValue();

            // Check for compatibility with WP2 or WP3 if the section allows WP2/WP3
            boolean isCompatible = Objects.equals(section.getAllowedWorkpieceId(), workpieceId) ||
                    (section.getAllowedWorkpieceId().equals(WORKPIECE_ID_2) &&
                            (workpieceId.equals(WORKPIECE_ID_2) || workpieceId.equals(WORKPIECE_ID_3)));

            if (!section.isFilled() && isCompatible) {
                return index;
            }
        }
        return -1;
    }
}