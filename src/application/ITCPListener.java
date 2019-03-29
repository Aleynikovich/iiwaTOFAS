package application;

public interface ITCPListener {
		
	public void OnTCPMessageReceived(String datagram);
	public void OnTCPConnection();
	
}
