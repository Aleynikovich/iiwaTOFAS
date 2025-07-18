package hartu.protocols.constants;

public enum AngularUnit {
    DEGREE(1.0),   // Degrees (base unit for conversions)
    RADIAN(Math.toDegrees(1.0)); // Radians (converted to degrees)

    private final double conversionFactorToDegrees; // Factor to multiply by to get value in Degrees

    AngularUnit(double conversionFactorToDegrees) {
        this.conversionFactorToDegrees = conversionFactorToDegrees;
    }

    public double convertTo(double value, AngularUnit targetUnit) {
        // Convert value to Degrees first, then from Degrees to targetUnit
        return (value * this.conversionFactorToDegrees) / targetUnit.conversionFactorToDegrees;
    }

    public double getConversionFactorToDegrees() {
        return conversionFactorToDegrees;
    }

    public static AngularUnit fromString(String unitString) {
        for (AngularUnit unit : AngularUnit.values()) {
            if (unit.name().equalsIgnoreCase(unitString)) {
                return unit;
            }
        }
        throw new IllegalArgumentException("Unknown angular unit: " + unitString);
    }
}