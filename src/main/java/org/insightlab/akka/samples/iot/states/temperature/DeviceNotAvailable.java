package org.insightlab.akka.samples.iot.states.temperature;

public final class DeviceNotAvailable implements TemperatureReading {

	@Override
	public boolean equals(Object t) {
		return t instanceof DeviceNotAvailable;
	}
	
}
