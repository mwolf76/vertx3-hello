# vertx3-hello
A simple demo webapp built with vert.x3.

To start the application you need to provide a valid oauth2 credential setup via the google developer site: https://console.developers.google.com

What this actually boils down to is to generate a valid clientID and clientSecret to put into the configuration. 

Refer to the google official documentation for more details: https://developers.google.com/identity/protocols/OAuth2

Once the setup is done, you can run the deploy.sh script to run the application or - if you're lucky enough to use IDEA ;-) - just run using the run configuration enclosed in the IDEA project.
 
This application uses the Pebble Templating Engine: http://www.mitchellbosecke.com/pebble/home by Mitchell Bösecke.
 


