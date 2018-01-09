package org.insightlab.akka.samples.iot.messages.temperature;

public final class ReadTemperature {
	public long requestId;
	
	public ReadTemperature(long requestId){
		this.requestId = requestId;
	}

}
