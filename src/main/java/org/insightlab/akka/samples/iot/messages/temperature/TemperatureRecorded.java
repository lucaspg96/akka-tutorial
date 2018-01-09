package org.insightlab.akka.samples.iot.messages.temperature;

public final class TemperatureRecorded {
	public final long requestId;
	
	public TemperatureRecorded(long requestedId){
		this.requestId = requestedId;
	}
}
