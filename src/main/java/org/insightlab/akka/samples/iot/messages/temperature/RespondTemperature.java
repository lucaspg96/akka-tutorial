package org.insightlab.akka.samples.iot.messages.temperature;

import java.util.Optional;

public final class RespondTemperature {
	public long requestId;
	public final Optional<Double> value;
	
	public RespondTemperature(long requestId, Optional<Double> value){
		this.requestId = requestId;
		this.value = value;
	}
}
