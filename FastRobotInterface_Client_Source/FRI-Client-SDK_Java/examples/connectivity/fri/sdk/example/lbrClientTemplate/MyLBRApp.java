package connectivity.fri.sdk.example.lbrClientTemplate;

import com.kuka.connectivity.fastRobotInterface.clientSDK.base.ClientApplication;
import com.kuka.connectivity.fastRobotInterface.clientSDK.connection.UdpConnection;

/**
 * Template implementation of a FRI client hartu.application.
 * <p>
 * The hartu.application provides a {@link ClientApplication#connect}, a {@link ClientApplication#step()} and a
 * {@link ClientApplication#disconnect} method, which will be called successively in the hartu.application life-cycle.
 * 
 * 
 * @see ClientApplication#connect
 * @see ClientApplication#step()
 * @see ClientApplication#disconnect
 */
public class MyLBRApp
{

    private static final int DEFAULT_PORTID = 30200;

    /**
     * @param argv
     *            the arguments
     */
    public static void main(String[] argv)
    {
        // create new client
        MyLBRClient client = new MyLBRClient();

        /***************************************************************************/
        /*                                                                         */
        /* Standard hartu.application structure */
        /* Configuration */
        /*                                                                         */
        /***************************************************************************/

        // create new udp connection
        UdpConnection connection = new UdpConnection();

        // pass connection and client to a new FRI client hartu.application
        ClientApplication app = new ClientApplication(connection, client);

        // connect client hartu.application to KUKA Sunrise controller
        app.connect(DEFAULT_PORTID);

        /***************************************************************************/
        /*                                                                         */
        /* Standard hartu.application structure */
        /* Execution mainloop */
        /*                                                                         */
        /***************************************************************************/

        // repeatedly call the step routine to receive and process FRI packets
        boolean success = true;
        while (success)
        {
            success = app.step();
        }

        /***************************************************************************/
        /*                                                                         */
        /* Standard hartu.application structure */
        /* Dispose */
        /*                                                                         */
        /***************************************************************************/

        // disconnect from controller
        app.disconnect();
    }
}
