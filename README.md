
## How to Run

You need Java 8 and Maven installed.

```
mvn clean package
mvn exec:java
```

Server runs at http://localhost:8080/api/v1/

## API Endpoints

- GET /api/v1/ - discovery
- GET /api/v1/rooms - list all rooms
- POST /api/v1/rooms - create a room
- GET /api/v1/rooms/{id} - get a room
- DELETE /api/v1/rooms/{id} - delete a room
- GET /api/v1/sensors - list sensors (optional ?type= filter)
- POST /api/v1/sensors - create a sensor
- GET /api/v1/sensors/{id}/readings - get readings
- POST /api/v1/sensors/{id}/readings - add a reading

## Sample curl Commands

```
curl http://localhost:8080/api/v1/

curl http://localhost:8080/api/v1/rooms

curl -X POST http://localhost:8080/api/v1/rooms -H "Content-Type: application/json" -d '{"id":"LIB-301","name":"Library","capacity":50}'

curl -X POST http://localhost:8080/api/v1/sensors -H "Content-Type: application/json" -d '{"id":"CO2-001","type":"CO2","status":"ACTIVE","roomId":"LIB-301"}'

curl "http://localhost:8080/api/v1/sensors?type=CO2"

curl -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings -H "Content-Type: application/json" -d '{"value":412.5}'

curl http://localhost:8080/api/v1/sensors/CO2-001/readings
```

---

# Report

# Part 1.1. JAX-RS Resource Lifecycle

JAX-RS makes an instance of a resource class for every request by default. This means that if you store things in instance variables they get reset each time which's not very useful.

To fix this issue I created a DataStore class with fields so that all the resources can share the same data. I also used ConcurrentHashMap of HashMap because multiple requests can come in at once. A normal HashMap is not thread safe so it could mess up the data or overwrite things.

# Part 1.2. HATEOAS

HATEOAS is about adding links in the response so the client knows what to do. Of hardcoding URLs the client just follows links like you would on a website.

This is helpful because if the JAX-RS API changes later the client does not have to break. In this project the discovery endpoint shows this because it returns links to rooms and sensors.

#Part 2.1. IDs vs Full Objects

There are pros and cons for both IDs and full objects. If you return IDs the response is smaller and faster but then the client has to send more requests to actually get the details.

If you return objects everything is there straight away but the response is bigger. In this JAX-RS API I went with objects because realistically you usually need the details anyway and it is not like there are thousands of rooms so size is not really a big issue.

# 2.2. Is Idempotent

Yes DELETE is idempotent. If you delete a room the time it works and removes it. If you send the request again the room is already gone, so nothing really changes.

Even though you might get a response like 204 first then 404 the end result is still the same, which is what matters for the JAX-RS API.

# Part 3.1. Wrong Content-Type

If the client sends the content type like text/plain instead of JSON JAX-RS actually handles it automatically. Since the endpoint only accepts JSON it just returns a 415 error.

The method does not even run, which is nice because you do not need to write code for it. This makes things easier for the JAX-RS API.

# Part 3.2. @QueryParam vs Path Segment

Using something like /sensors?type=CO2 just makes sense than /sensors/type/CO2.

Path segments are usually for identifying a thing so using them for filtering is a bit confusing. Query params are made for filtering. You can easily add more, like status=ACTIVE or whatever to the JAX-RS API.

# Part 4.1. Sub-Resource Locator Pattern

The sub-resource locator is used in SensorResource to pass control to another class, SensorReadingResource. The method itself does not have a GET or POST annotation. Jax-RS still knows what to do.

This helps keep things organized instead of putting everything in one massive class. It is easier to read and also easier to test stuff for the JAX-RS API.

# Part 5.2. 422 Vs 404

404 means the URL itself does not exist. But in this case the endpoint is fine it is the data that is wrong like the room ID not existing.

So 422 makes sense because it means the request was understood but could not be processed properly. It is more accurate for the JAX-RS API.

# Part 5.4. Stack Trace Security Risk

If you send a stack trace back to the client it is kind of risky. It shows stuff like class names, file paths and even library versions.

An attacker could use that information to find weaknesses. So instead the GenericExceptionMapper just returns a 500 error without exposing anything for the JAX-RS API.

Part 5.5. Filters vs Inline Logging

If you put logging inside every method it gets really repetitive and annoying to maintain. You also might forget to add it

Using a filter is way better because you write it once and it applies everywhere automatically. It is cleaner. Follows the rule of not repeating yourself which is what you want for things, like logging in the JAX-RS API.