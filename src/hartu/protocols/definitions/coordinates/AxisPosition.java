package hartu.protocols.definitions.coordinates;

import hartu.protocols.constants.AngularUnit;
import java.util.Arrays;

public class AxisPosition extends TargetPosition {
    private final double[] axisValues;
    private final AngularUnit angularUnit;

    public AxisPosition(double[] axisValues, AngularUnit angularUnit) {
        if (axisValues == null) {
            this.axisValues = new double[0];
        } else {
            this.axisValues = Arrays.copyOf(axisValues, axisValues.length);
        }
        this.angularUnit = angularUnit;
    }

    public AxisPosition(double[] axisValues) {
        this(axisValues, AngularUnit.DEGREE);
    }

    public double[] getAxisValues() { return Arrays.copyOf(axisValues, axisValues.length); }
    public AngularUnit getAngularUnit() { return angularUnit; }

    public double getAxisValue(int index, AngularUnit targetUnit) {
        if (index < 0 || index >= axisValues.length) {
            throw new IndexOutOfBoundsException("Axis index out of bounds: " + index);
        }
        return angularUnit.convertTo(axisValues[index], targetUnit);
    }

    public double[] getAxisValues(AngularUnit targetUnit) {
        double[] convertedValues = new double[axisValues.length];
        for (int i = 0; i < axisValues.length; i++) {
            convertedValues[i] = angularUnit.convertTo(axisValues[i], targetUnit);
        }
        return convertedValues;
    }

    @Override
    public String toString() {
        return "AxisPosition [axisValues=" + Arrays.toString(axisValues) + " " + angularUnit.name() + "]";
    }
}