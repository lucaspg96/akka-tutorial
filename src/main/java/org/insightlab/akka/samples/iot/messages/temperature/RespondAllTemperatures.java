package org.insightlab.akka.samples.iot.messages.temperature;

import java.util.Map;

import org.insightlab.akka.samples.iot.states.temperature.TemperatureReading;

public final class RespondAllTemperatures {
	public final long requestId;
	public final Map<String, TemperatureReading> temperatures;
	
	public RespondAllTemperatures(long requestId, Map<String, TemperatureReading> temperatures){
		this.requestId = requestId;
		this.temperatures = temperatures;
	}
}
