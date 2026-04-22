package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.ErrorResponse;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> sensorList = new ArrayList<>(DataStore.sensors.values());
        if (type != null && !type.isEmpty()) {
            sensorList = sensorList.stream()
                    .filter(s -> type.equalsIgnoreCase(s.getType()))
                    .collect(Collectors.toList());
        }
        return Response.ok(sensorList).build();
    }

    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor.getRoomId() == null || !DataStore.rooms.containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                    "Room not found with id: " + sensor.getRoomId() + ". Register the room first.");
        }
        if (sensor.getId() == null || sensor.getId().isEmpty()) {
            sensor.setId(UUID.randomUUID().toString());
        }
        if (sensor.getStatus() == null || sensor.getStatus().isEmpty()) {
            sensor.setStatus("ACTIVE");
        }
        DataStore.sensors.put(sensor.getId(), sensor);
        DataStore.rooms.get(sensor.getRoomId()).getSensorIds().add(sensor.getId());
        DataStore.readings.put(sensor.getId(), new ArrayList<>());
        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.sensors.get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("NOT_FOUND", "Sensor not found: " + sensorId))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    // Sub-resource locator — no HTTP method annotation
    @Path("/{sensorId}/readings")
    public SensorReadingResource getSensorReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
