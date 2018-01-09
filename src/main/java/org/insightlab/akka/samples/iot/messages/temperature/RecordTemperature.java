package org.insightlab.akka.samples.iot.messages.temperature;

public final class RecordTemperature {
	public final double value;
	public final long requestId;
	
	public RecordTemperature(long requestId, double value){
		this.requestId = requestId;
		this.value = value;
	}
}
