package se.sics.kompics.wan.master.plab.plc.events;


import se.sics.kompics.Request;
import se.sics.kompics.wan.master.plab.PlanetLabCredentials;

public class GetNodesRequest extends Request {
	
	private final PlanetLabCredentials cred;
	
	public GetNodesRequest(PlanetLabCredentials cred) {
		this.cred = cred;
	}
	
	/**
	 * @return the cred
	 */
	public PlanetLabCredentials getCred() {
		return cred;
	}
}