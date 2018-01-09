package org.insightlab.akka.samples.iot.messages.temperature;

public final class RequestAllTemperatures {
	public final long requestId;
	
	public RequestAllTemperatures(long requestId){
		this.requestId = requestId;
	}
}
