package com.kuka.generated.ioAccess;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.ioModel.AbstractIOGroup;
import com.kuka.roboticsAPI.ioModel.IOTypes;

/**
 * Automatically generated class to abstract I/O access to I/O group <b>IOFlange</b>.<br>
 * <i>Please, do not modify!</i>
 * <p>
 * <b>I/O group description:</b><br>
 * ./.
 */
@Singleton
public class IOFlangeIOGroup extends AbstractIOGroup
{
	/**
	 * Constructor to create an instance of class 'IOFlange'.<br>
	 * <i>This constructor is automatically generated. Please, do not modify!</i>
	 *
	 * @param controller
	 *            the controller, which has access to the I/O group 'IOFlange'
	 */
	@Inject
	public IOFlangeIOGroup(Controller controller)
	{
		super(controller, "IOFlange");

		addInput("DI_Flange1", IOTypes.BOOLEAN, 1);
		addInput("DI_Flange2", IOTypes.BOOLEAN, 1);
		addInput("DI_Flange3", IOTypes.BOOLEAN, 1);
		addInput("DI_Flange4", IOTypes.BOOLEAN, 1);
		addInput("DI_Flange5", IOTypes.BOOLEAN, 1);
		addInput("DI_Flange6", IOTypes.BOOLEAN, 1);
		addInput("DI_Flange7", IOTypes.BOOLEAN, 1);
		addInput("DI_Flange8", IOTypes.BOOLEAN, 1);
		addDigitalOutput("DO_Flange1", IOTypes.BOOLEAN, 1);
		addDigitalOutput("DO_Flange2", IOTypes.BOOLEAN, 1);
		addDigitalOutput("DO_Flange3", IOTypes.BOOLEAN, 1);
		addDigitalOutput("DO_Flange4", IOTypes.BOOLEAN, 1);
		addDigitalOutput("DO_Flange5", IOTypes.BOOLEAN, 1);
		addDigitalOutput("DO_Flange6", IOTypes.BOOLEAN, 1);
		addDigitalOutput("DO_Flange7", IOTypes.BOOLEAN, 1);
		addDigitalOutput("DO_Flange8", IOTypes.BOOLEAN, 1);
	}

	/**
	 * Gets the value of the <b>digital input '<i>DI_Flange1</i>'</b>.<br>
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
	 * @return current value of the digital input 'DI_Flange1'
	 */
	public boolean getDI_Flange1()
	{
		return getBooleanIOValue("DI_Flange1", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>DI_Flange2</i>'</b>.<br>
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
	 * @return current value of the digital input 'DI_Flange2'
	 */
	public boolean getDI_Flange2()
	{
		return getBooleanIOValue("DI_Flange2", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>DI_Flange3</i>'</b>.<br>
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
	 * @return current value of the digital input 'DI_Flange3'
	 */
	public boolean getDI_Flange3()
	{
		return getBooleanIOValue("DI_Flange3", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>DI_Flange4</i>'</b>.<br>
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
	 * @return current value of the digital input 'DI_Flange4'
	 */
	public boolean getDI_Flange4()
	{
		return getBooleanIOValue("DI_Flange4", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>DI_Flange5</i>'</b>.<br>
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
	 * @return current value of the digital input 'DI_Flange5'
	 */
	public boolean getDI_Flange5()
	{
		return getBooleanIOValue("DI_Flange5", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>DI_Flange6</i>'</b>.<br>
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
	 * @return current value of the digital input 'DI_Flange6'
	 */
	public boolean getDI_Flange6()
	{
		return getBooleanIOValue("DI_Flange6", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>DI_Flange7</i>'</b>.<br>
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
	 * @return current value of the digital input 'DI_Flange7'
	 */
	public boolean getDI_Flange7()
	{
		return getBooleanIOValue("DI_Flange7", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>DI_Flange8</i>'</b>.<br>
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
	 * @return current value of the digital input 'DI_Flange8'
	 */
	public boolean getDI_Flange8()
	{
		return getBooleanIOValue("DI_Flange8", false);
	}

	/**
	 * Gets the value of the <b>digital output '<i>DO_Flange1</i>'</b>.<br>
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
	 * @return current value of the digital output 'DO_Flange1'
	 */
	public boolean getDO_Flange1()
	{
		return getBooleanIOValue("DO_Flange1", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>DO_Flange1</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'DO_Flange1'
	 */
	public void setDO_Flange1(java.lang.Boolean value)
	{
		setDigitalOutput("DO_Flange1", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>DO_Flange2</i>'</b>.<br>
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
	 * @return current value of the digital output 'DO_Flange2'
	 */
	public boolean getDO_Flange2()
	{
		return getBooleanIOValue("DO_Flange2", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>DO_Flange2</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'DO_Flange2'
	 */
	public void setDO_Flange2(java.lang.Boolean value)
	{
		setDigitalOutput("DO_Flange2", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>DO_Flange3</i>'</b>.<br>
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
	 * @return current value of the digital output 'DO_Flange3'
	 */
	public boolean getDO_Flange3()
	{
		return getBooleanIOValue("DO_Flange3", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>DO_Flange3</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'DO_Flange3'
	 */
	public void setDO_Flange3(java.lang.Boolean value)
	{
		setDigitalOutput("DO_Flange3", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>DO_Flange4</i>'</b>.<br>
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
	 * @return current value of the digital output 'DO_Flange4'
	 */
	public boolean getDO_Flange4()
	{
		return getBooleanIOValue("DO_Flange4", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>DO_Flange4</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'DO_Flange4'
	 */
	public void setDO_Flange4(java.lang.Boolean value)
	{
		setDigitalOutput("DO_Flange4", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>DO_Flange5</i>'</b>.<br>
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
	 * @return current value of the digital output 'DO_Flange5'
	 */
	public boolean getDO_Flange5()
	{
		return getBooleanIOValue("DO_Flange5", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>DO_Flange5</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'DO_Flange5'
	 */
	public void setDO_Flange5(java.lang.Boolean value)
	{
		setDigitalOutput("DO_Flange5", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>DO_Flange6</i>'</b>.<br>
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
	 * @return current value of the digital output 'DO_Flange6'
	 */
	public boolean getDO_Flange6()
	{
		return getBooleanIOValue("DO_Flange6", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>DO_Flange6</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'DO_Flange6'
	 */
	public void setDO_Flange6(java.lang.Boolean value)
	{
		setDigitalOutput("DO_Flange6", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>DO_Flange7</i>'</b>.<br>
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
	 * @return current value of the digital output 'DO_Flange7'
	 */
	public boolean getDO_Flange7()
	{
		return getBooleanIOValue("DO_Flange7", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>DO_Flange7</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'DO_Flange7'
	 */
	public void setDO_Flange7(java.lang.Boolean value)
	{
		setDigitalOutput("DO_Flange7", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>DO_Flange8</i>'</b>.<br>
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
	 * @return current value of the digital output 'DO_Flange8'
	 */
	public boolean getDO_Flange8()
	{
		return getBooleanIOValue("DO_Flange8", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>DO_Flange8</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'DO_Flange8'
	 */
	public void setDO_Flange8(java.lang.Boolean value)
	{
		setDigitalOutput("DO_Flange8", value);
	}

}
