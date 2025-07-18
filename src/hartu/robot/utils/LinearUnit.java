package hartu.robot.utils;

public enum LinearUnit {
    MM(1.0),       // Millimeters (base unit for conversions)
    M(1000.0),     // Meters
    INCH(25.4),    // Inches
    FOOT(304.8);   // Feet

    private final double conversionFactorToMM; // Factor to multiply by to get value in MM

    LinearUnit(double conversionFactorToMM) {
        this.conversionFactorToMM = conversionFactorToMM;
    }

    public double convertTo(double value, LinearUnit targetUnit) {
        // Convert value to MM first, then from MM to targetUnit
        return (value * this.conversionFactorToMM) / targetUnit.conversionFactorToMM;
    }

    public double getConversionFactorToMM() {
        return conversionFactorToMM;
    }

    public static LinearUnit fromString(String unitString) {
        for (LinearUnit unit : LinearUnit.values()) {
            if (unit.name().equalsIgnoreCase(unitString)) {
                return unit;
            }
        }
        throw new IllegalArgumentException("Unknown linear unit: " + unitString);
    }
}