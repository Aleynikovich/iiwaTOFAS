package com.kuka.generated.ioAccess;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.ioModel.AbstractIOGroup;
import com.kuka.roboticsAPI.ioModel.IOTypes;

/**
 * Automatically generated class to abstract I/O access to I/O group <b>Ethercat_x44</b>.<br>
 * <i>Please, do not modify!</i>
 * <p>
 * <b>I/O group description:</b><br>
 * ./.
 */
@Singleton
public class Ethercat_x44IOGroup extends AbstractIOGroup
{
	/**
	 * Constructor to create an instance of class 'Ethercat_x44'.<br>
	 * <i>This constructor is automatically generated. Please, do not modify!</i>
	 *
	 * @param controller
	 *            the controller, which has access to the I/O group 'Ethercat_x44'
	 */
	@Inject
	public Ethercat_x44IOGroup(Controller controller)
	{
		super(controller, "Ethercat_x44");

		addInput("Input1", IOTypes.BOOLEAN, 1);
		addInput("Input2", IOTypes.BOOLEAN, 1);
		addInput("Input3", IOTypes.BOOLEAN, 1);
		addInput("Input4", IOTypes.BOOLEAN, 1);
		addInput("Input5", IOTypes.BOOLEAN, 1);
		addInput("Input6", IOTypes.BOOLEAN, 1);
		addInput("Input7", IOTypes.BOOLEAN, 1);
		addInput("Input8", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Output1", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Output2", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Output3", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Output4", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Output5", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Output6", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Output7", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Output8", IOTypes.BOOLEAN, 1);
	}

	/**
	 * Gets the value of the <b>digital input '<i>Input1</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'Input1'
	 */
	public boolean getInput1()
	{
		return getBooleanIOValue("Input1", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>Input2</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'Input2'
	 */
	public boolean getInput2()
	{
		return getBooleanIOValue("Input2", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>Input3</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'Input3'
	 */
	public boolean getInput3()
	{
		return getBooleanIOValue("Input3", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>Input4</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'Input4'
	 */
	public boolean getInput4()
	{
		return getBooleanIOValue("Input4", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>Input5</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'Input5'
	 */
	public boolean getInput5()
	{
		return getBooleanIOValue("Input5", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>Input6</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'Input6'
	 */
	public boolean getInput6()
	{
		return getBooleanIOValue("Input6", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>Input7</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'Input7'
	 */
	public boolean getInput7()
	{
		return getBooleanIOValue("Input7", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>Input8</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'Input8'
	 */
	public boolean getInput8()
	{
		return getBooleanIOValue("Input8", false);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Output1</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital output 'Output1'
	 */
	public boolean getOutput1()
	{
		return getBooleanIOValue("Output1", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Output1</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'Output1'
	 */
	public void setOutput1(java.lang.Boolean value)
	{
		setDigitalOutput("Output1", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Output2</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital output 'Output2'
	 */
	public boolean getOutput2()
	{
		return getBooleanIOValue("Output2", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Output2</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'Output2'
	 */
	public void setOutput2(java.lang.Boolean value)
	{
		setDigitalOutput("Output2", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Output3</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital output 'Output3'
	 */
	public boolean getOutput3()
	{
		return getBooleanIOValue("Output3", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Output3</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'Output3'
	 */
	public void setOutput3(java.lang.Boolean value)
	{
		setDigitalOutput("Output3", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Output4</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital output 'Output4'
	 */
	public boolean getOutput4()
	{
		return getBooleanIOValue("Output4", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Output4</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'Output4'
	 */
	public void setOutput4(java.lang.Boolean value)
	{
		setDigitalOutput("Output4", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Output5</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital output 'Output5'
	 */
	public boolean getOutput5()
	{
		return getBooleanIOValue("Output5", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Output5</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'Output5'
	 */
	public void setOutput5(java.lang.Boolean value)
	{
		setDigitalOutput("Output5", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Output6</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital output 'Output6'
	 */
	public boolean getOutput6()
	{
		return getBooleanIOValue("Output6", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Output6</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'Output6'
	 */
	public void setOutput6(java.lang.Boolean value)
	{
		setDigitalOutput("Output6", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Output7</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital output 'Output7'
	 */
	public boolean getOutput7()
	{
		return getBooleanIOValue("Output7", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Output7</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'Output7'
	 */
	public void setOutput7(java.lang.Boolean value)
	{
		setDigitalOutput("Output7", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Output8</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital output 'Output8'
	 */
	public boolean getOutput8()
	{
		return getBooleanIOValue("Output8", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Output8</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'Output8'
	 */
	public void setOutput8(java.lang.Boolean value)
	{
		setDigitalOutput("Output8", value);
	}

}
