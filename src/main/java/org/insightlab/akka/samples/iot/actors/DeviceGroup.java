package org.insightlab.akka.samples.iot.actors;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.insightlab.akka.samples.iot.messages.registration.RequestTrackDevice;
import org.insightlab.akka.samples.iot.messages.temperature.RequestAllTemperatures;
import org.insightlab.akka.samples.iot.messages.temperature.RespondAllTemperatures;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.concurrent.duration.FiniteDuration;

//This class represents a  IoT device group.
//It will be responsible for manage the devices of that group
public class DeviceGroup extends AbstractActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	//Each group have an id
	public final String groupId;
	
	//The actor must know the reference of each device
	private final Map<String,ActorRef> deviceActors;
	//The actor must also know the device for each reference
	private final Map<ActorRef,String> actorsIds;
	
	public DeviceGroup(String groupId) {
		this.groupId = groupId;
		deviceActors = new HashMap<>();
		actorsIds = new HashMap<>();
	}
	
	//This static method is used to create an actor indirectly
	public static Props props(String groupId){
		return Props.create(DeviceGroup.class, groupId);
	}
	
	//Helper message class just to request the list of devices
	//from that group
	public static final class RequestDeviceList{
		final long requestId;
		
		public RequestDeviceList(long requestId){
			this.requestId = requestId;
		}
	}
	
	//Helper message class just to request the list of devices
	//from that group
	public static final class ReplyDeviceList{
		final long requestId;
		final Set<String> ids;
		
		public ReplyDeviceList(long requestId, Set<String> ids){
			this.requestId = requestId;
			this.ids = ids;
		}
	}
	
	//Method to create the DeviceGroupQuery, which will handle the
	//request of all temperatures
	private void onAllTemperatures(RequestAllTemperatures r){
		getContext().actorOf(DeviceGroupQuery.props(
				actorsIds, r.requestId, getSender(), new FiniteDuration(3, TimeUnit.SECONDS)));
	}
	
	//Method to add a new device
	private void onTrackDevice(RequestTrackDevice trackMsg){
		if(this.groupId.equals(trackMsg.groupId)){
			//If the group is correct, we proceed
			ActorRef deviceActor = deviceActors.get(trackMsg.deviceId);
			
			//If the device actor already exists, we just forward the message
			if(deviceActor != null){
				deviceActor.forward(trackMsg, getContext());
			}
			else{
				//Otherwise, we must create a new one and forward the message
				log.info("Creating device actor for {}", trackMsg.deviceId);
				deviceActor = getContext().actorOf(Device.props(groupId, trackMsg.deviceId),"device-"+trackMsg.deviceId);
				
				//The group actor must watch every device to, if they shutdown,
				//it must be removed from the group
				getContext().watch(deviceActor);
				
				//Then, the new actor is stored on the maps
				deviceActors.put(trackMsg.deviceId, deviceActor);
				actorsIds.put(deviceActor, trackMsg.deviceId);
				deviceActor.forward(trackMsg, getContext());
			}
			
		}
		else{
			//Else, a warning is shown
			log.warning("Ignoring TrackDevice request for {}. This actor is responsible for {}.",trackMsg.groupId, groupId);
		}
	}
	
	//Method to handle a device shutdown
	private void onTerminate(Terminated t){
		//If a device stops, we must remove it from the group
		ActorRef deviceActor = t.getActor();
		String deviceId = actorsIds.get(deviceActor);
		
		log.info("Device actor {} has been terminated",deviceId);
		
		actorsIds.remove(deviceActor);
		deviceActors.remove(deviceId);
		
	}
	
	//Method to handle devices list request
	private void onDeviceList(RequestDeviceList r){
		//Answer the request with the list of devices from the group
		getSender().tell(new ReplyDeviceList(r.requestId, deviceActors.keySet()), getSelf());
	}
	
	//This method executes when the actor starts
	@Override
	public void preStart() {
		log.info("DeviceGroup {} started", groupId);
	}
	
	//This method executes when the actor stops
	@Override
	public void postStop() {
		log.info("DeviceGroup {} stopped", groupId);
	}
	
	//At this method we define, for each message pattern, the behavior of the actor
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(RequestTrackDevice.class, this::onTrackDevice)
				.match(Terminated.class, this::onTerminate)
				.match(RequestDeviceList.class, this::onDeviceList)
				.match(RequestAllTemperatures.class, this::onAllTemperatures)
				.build();
	}

}
