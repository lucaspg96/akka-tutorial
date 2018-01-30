package org.insightlab.akka.samples.iot.actors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.insightlab.akka.samples.iot.messages.temperature.ReadTemperature;
import org.insightlab.akka.samples.iot.messages.temperature.RespondAllTemperatures;
import org.insightlab.akka.samples.iot.messages.temperature.RespondTemperature;
import org.insightlab.akka.samples.iot.states.temperature.DeviceNotAvailable;
import org.insightlab.akka.samples.iot.states.temperature.DeviceTimedOut;
import org.insightlab.akka.samples.iot.states.temperature.Temperature;
import org.insightlab.akka.samples.iot.states.temperature.TemperatureNotAvailable;
import org.insightlab.akka.samples.iot.states.temperature.TemperatureReading;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.concurrent.duration.FiniteDuration;

//This class is responsible to query the temperature from all devices
//from a group
public class DeviceGroupQuery extends AbstractActor {
	
	//This is an auxiliary class to help trigger a timeout
	public static final class CollectionTimeout{
	}
	
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	//We must know the id of a givend actor device
	final Map<ActorRef,String> actorToDevice;
	final long requestId;
	//We must know who made the temperatures request
	final ActorRef requester;
	
	Cancellable queryTimeoutTimer;
	
	public DeviceGroupQuery(Map<ActorRef,String> actorToDevice, long requestId, ActorRef requester, FiniteDuration timeout){
		this.actorToDevice = actorToDevice;
		this.requestId = requestId;
		this.requester = requester;
		
		//Given a duration, this will trigger a CollectionTimeout message
		queryTimeoutTimer = getContext().getSystem().scheduler().scheduleOnce(
				timeout, getSelf(), new CollectionTimeout(), getContext().dispatcher(), getSelf());
	}
	
	//This static method is used to create an actor indirectly
	public static Props props(Map<ActorRef,String> actorToDevice, long requestId, ActorRef requester, FiniteDuration timeout){
		return Props.create(DeviceGroupQuery.class, actorToDevice, requestId, requester, timeout);
	}
	
	//This method executes when the actor starts
	@Override
	public void preStart(){
		//A ReadTemperature message is sent to each device actor
		for (ActorRef deviceActor : actorToDevice.keySet()){
			getContext().watch(deviceActor);
			deviceActor.tell(new ReadTemperature(0L), getSelf());
		}
	}
	
	//This method executes when the actor stops
	@Override
	public void postStop(){
		queryTimeoutTimer.cancel();
	}
	
	//At this method we define, for each message pattern, the behavior of the actor
	@Override
	public Receive createReceive() {
		//The creation of the Receive is delegated to another method,
		//allowing the actor to change it's strategy of the actor for each
		//answer given by the devices
		return waitingForReplies(new HashMap<>(), actorToDevice.keySet());
	}
	
	//This method returns a Recieve that is buit according to the
	//remaining actors that hasn't answered yet and the responses
	public Receive waitingForReplies(Map<String,TemperatureReading> repliesSoFar, Set<ActorRef> stillWaiting){
		//For each message that is received (excepted by the CollectionTimeout,
		//the answer will be sent to a function that will map the answer and
		//change the strategy of message treatment
		return receiveBuilder()
				
				//When a temperature is answered, it will be stored
				.match(RespondTemperature.class, r -> {
					ActorRef deviceActor = getSender();
					TemperatureReading reading = r.value.map(v -> (TemperatureReading) new Temperature(v))
													.orElse(new TemperatureNotAvailable());
					
					receivedResponse(deviceActor, reading, stillWaiting, repliesSoFar);
				})
				
				//When an actor finish its execution before answers,
				//a DeviceNotAvailable is stored
				.match(Terminated.class, t -> receivedResponse(t.getActor(), new DeviceNotAvailable(), stillWaiting, repliesSoFar))
				
				//When timeout happens, all devices that hasn't answered, a
				//DeviceTimedOut is stored and the result is sent to the requester
				.match(CollectionTimeout.class,  t -> {
					Map<String, TemperatureReading> replies = new HashMap<>(repliesSoFar);
					for(ActorRef deviceActor : stillWaiting){
						String deviceId = actorToDevice.get(deviceActor);
						replies.put(deviceId, new DeviceTimedOut());
					}
					
					requester.tell(new RespondAllTemperatures(requestId, replies), getSelf());
					getContext().stop(getSelf());
				})
				.build();
	}
	
	//This method process a TemperatureReading from the message received and changes
	//the actor strategy
	private void receivedResponse(ActorRef deviceActor, TemperatureReading reading, Set<ActorRef> stillWaiting,
			Map<String, TemperatureReading> repliesSoFar) {
		
		//Since we collected an answer from some actor, we don't need
		//to watch it anymore
		getContext().unwatch(deviceActor);
		
		//We get the device id
		String deviceId = actorToDevice.get(deviceActor);
		
		//We create a new set of actors that hasn't answered yet
		Set<ActorRef> newStillWaiting = new HashSet<>(stillWaiting);
		newStillWaiting.remove(deviceActor);
		
		//We update the map of temperatures
		Map<String, TemperatureReading> newRepliesSoFar = new HashMap<>(repliesSoFar);
		newRepliesSoFar.put(deviceId, reading);
		
		//If every actor has answered, the result will be sent to the requester
		if(newStillWaiting.isEmpty()){
			requester.tell(new RespondAllTemperatures(requestId, newRepliesSoFar), getSelf());
			getContext().stop(getSelf());
		}
		//Otherwise, we change the message strategy, now with the new list of
		//remaining actors
		else{
			getContext().become(waitingForReplies(newRepliesSoFar, newStillWaiting));
		}
	}
	
}
