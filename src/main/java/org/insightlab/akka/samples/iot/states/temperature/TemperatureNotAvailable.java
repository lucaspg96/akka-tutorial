package org.insightlab.akka.samples.iot.states.temperature;

public final class TemperatureNotAvailable implements TemperatureReading {

	@Override
	public boolean equals(Object t) {
		return t instanceof TemperatureNotAvailable;
	}

}
