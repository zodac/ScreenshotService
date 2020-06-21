# Screenshot Service

The architecture diagram can be found in `doc/architecture.png`.

The RAML description of the service's REST endpoints can be found in `doc/screenshot-service.html`.  


## Required software

- Java JDK 8
- Apache maven v3.6.0
- Docker For Windows v2.0.0.3 (Windows 10)
- Postman (for REST requests)


## First build the service artifacts:
   
    mvn clean install
    
This will generate the **screenshot-service** EAR and move it into the *docker/wildfly/ear* directory
    
## Now bring the docker containers online:

    docker-compose up --build --scale selenium-chrome=5
    
This generates 4 containers:
    
    
#### wildfly
    The Wildfly application server, which deploys any EARs contained in the *docker/wildfly/ear* directory.
    
#### selenium-hub
    The Selenium hub that Wildfly will send requests to, that forwards to the browser-specific container.

#### selenium-chrome
    The Google Chrome remote browser. Nothing will send requests to this directly, instead requests are sent to **selenium-hub** which will forward requests on to this container.

#### postgres
    The PostgreSQL database container, used to persist requests and results. 

We also specify `--scale selenium-chrome=5` in order to create 5 instances of the **selenium-chrome** container.


## Execute the integration tests (in another window)

    mvn clean install -Dts
    
This will execute some basic tests:

- Send a valid POST request with 10 URLs, retrieve the response, retrieve the screenshot
- Send an invalid POST request (with malformed URLs), and receive a 400 HTTP response
- Send an invalid GET request for a job ID, and receive a 404 HTTP response
- Send an invalid GET request for a screenshot file name, and receive a 404 HTTP response