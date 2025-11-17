package hartu.robot.commands.io;

import hartu.protocols.constants.ActionTypes;

public class IoCommandData
{
    private final int ioPoint;
    private final int ioPin;
    private final boolean ioState;
    private final ActionTypes actionType;

    public IoCommandData(int ioPoint, int ioPin, boolean ioState, ActionTypes actionType)
    {
        this.ioPoint = ioPoint;
        this.ioPin = ioPin;
        this.ioState = ioState;
        this.actionType = actionType;
    }

    public int getIoPoint()
    {
        return ioPoint;
    }

    public int getIoPin()
    {
        return ioPin;
    }

    public boolean getIoState()
    {
        return ioState;
    }

    public ActionTypes getActionType()
    {
        return actionType;
    }

    public boolean isOutputCommand()
    {
        return actionType == ActionTypes.ACTIVATE_IO;
    }

    public boolean isDigitalInputCommand()
    {
        return actionType == ActionTypes.DIGITAL_INPUT;
    }

    public boolean isAnalogInputCommand()
    {
        return actionType == ActionTypes.ANALOG_INPUT;
    }
}
