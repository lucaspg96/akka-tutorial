package org.insightlab.akka.samples.iot.actors;

import java.util.HashMap;
import java.util.Map;

import org.insightlab.akka.samples.iot.messages.registration.RequestTrackDevice;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;

//This class is responsible to manage the group actors
public final class DeviceManager extends AbstractActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	//The actor must know the reference of each group
	private final Map<String, ActorRef> groupToActor = new HashMap<>();
	//The actor must also know the group for each reference
	private final Map<ActorRef, String> actorToGroup = new HashMap<>();
	
	//This static method is used to create an actor indirectly
	public static Props props(){
		return Props.create(DeviceManager.class);
	}
	
	//Method to add a new device or group
	private void onTrackDevice(RequestTrackDevice trackMsg){
		String groupId = trackMsg.groupId;
		ActorRef ref = groupToActor.get(groupId);
		
		//If the group already exists, we just forward the message
		if(ref!=null){
			ref.forward(trackMsg, getContext());
		}
		//Otherwise, we create an actor for the group and forward the message 
		else{
			log.info("Creating device group actor for {}", groupId);
			ActorRef groupActor = getContext().actorOf(DeviceGroup.props(groupId),"group-"+groupId);
			getContext().watch(groupActor);
			
			//The manage actor must watch every group to, if they shutdown,
			//it must be removed
			groupActor.forward(trackMsg, getContext());
			
			//Then, the new actor is stored on the maps
			groupToActor.put(groupId, groupActor);
			actorToGroup.put(groupActor, groupId);
		}
	}
	
	//Method to handle a group shutdown
	private void onTerminated(Terminated t){
		//If a group stops, we must remove it
		ActorRef groupActor = t.getActor();
		String groupId = actorToGroup.get(groupActor);
		
		log.info("Device group {} has been terminated",groupId);
		
		actorToGroup.remove(groupActor);
		groupToActor.remove(groupId);
	}
	
	//This method executes when the actor starts
	@Override
	public void preStart() {
		log.info("DeviceManager started");
	}

	//This method executes when the actor stops
	@Override
	public void postStop() {
	  log.info("DeviceManager stopped");
	}
	
	//At this method we define, for each message pattern, the behavior of the actor
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(RequestTrackDevice.class, this::onTrackDevice)
				.match(Terminated.class, this::onTerminated)
				.build();
	}
}
