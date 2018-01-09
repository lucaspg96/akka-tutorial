package org.insightlab.akka.samples.iot.states.temperature;

public final class Temperature implements TemperatureReading {
	public final double value;
	
	public Temperature(double value){
		this.value = value;
	}
	
	@Override
	public boolean equals(Object t){
		if( t instanceof Temperature){
			if(((Temperature) t).value==value)
				return true;
		}
		
		return false;
	}
	
	@Override
	public String toString(){
		return Double.toString(value);
	}
}
