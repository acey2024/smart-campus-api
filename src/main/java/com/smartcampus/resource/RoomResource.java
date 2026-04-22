package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.ErrorResponse;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(DataStore.rooms.values());
        return Response.ok(roomList).build();
    }

    @POST
    public Response createRoom(Room room) {
        if (room.getId() == null || room.getId().isEmpty()) {
            room.setId(UUID.randomUUID().toString());
        }
        DataStore.rooms.put(room.getId(), room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.rooms.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("NOT_FOUND", "Room not found: " + roomId))
                    .build();
        }
        return Response.ok(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.rooms.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("NOT_FOUND", "Room not found: " + roomId))
                    .build();
        }
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException("Room " + roomId + " still has active sensors assigned.");
        }
        DataStore.rooms.remove(roomId);
        return Response.noContent().build();
    }
}
