## Socket
This project is a fork of the BL project, so anyone wishing to develop plugins for socket can simply fork this repo and follow the instructions below to run a development build of BL.

The socket plugin has the following events:

```
SocketBroadcastPacket
SocketMembersUpdate
SocketPlayerJoin
SocketPlayerLeave
SocketReceivePacket
SocketShutdown
SocketStartup
```

The events ```SocketShutdown``` and ```SocketStartup``` are new to v3.0.0, and are fired when the plugin startup() and shutdown() are called respectively. Additionally for those who didn't know, ```SocketMemebersUpdate``` was added in v2.0.9.

## Example Usage

For a simple example, if you wanted to send your players location along with their name via socket you'd do something like the following:
```
JSONObject packet = new JSONObject();
WorldPoint wp = client.getLocalPlayer().getWorldLocation();
packet.put("x", wp.getX());
packet.put("y", wp.getY());
packet.put("plane", wp.getPlane());
packet.put("name", client.getLocalPlayer().getName());
JSONObject payload = new JSONObject();
payload.put("your-key", packet); //This can be anything, it's just an identifier
eventBus.post(new SocketBroadcastPacket(payload));
```
where ```eventBus``` is injected in your plugin e.g. ```@Inject private EventBus eventbus;```

On the receiving end, to do something with this you'd do:

```
@Subscribe
public void onSocketPacketReceivePacket(SocketReceivePacket event)
{
  try
  {
    JSONObject payload = event.getPayload();
    if(payload.has("your-key"))
    {
      JSONObject data = payload.getJSONObject("your-key");
      WorldPoint wp = new WorldPoint(data.getInt("x"), data.getInt("y"), data.getInt("plane));
      String playerName = data.getString("name");
      //Do something with this
    }
  }
  catch(Exception e)
  {
    //Handle Exception
  }
}
```
If you have any questions or need any help feel free to dm me on discord at caps lock13#0001

## Dependencies
This project assumes you have the latest BL launcher. It uses the plugin jar and patches jar from client. You can update these by launching the launcher which will download the latest files. After a runelite update run client.repack task in gradle to create a repacked client jar from the latest runelite. 

## Starting the Client
Run the Client.main() gradle task to start the client. It will load externals as well as the plugins that come with the client so if you're working on your own external make sure its not also present in the externals folder or it will take that one over the classes in this project. It supports hotswapping classes. If this is enabled simply click the build button(the green hammer) for the Client.main() task while it is running. It may sometimes fail to build because theres conflicting classes on the classpath which aren't loaded in the right order. If this happens hotswapping will not work until you restart the client. I have no fix for this currently.

## Setting up externals
Make sure the package for your plugin starts with net.runelite.client.plugins. it is the same as developing a plugin inside the runelite project. When the project is build the jar containing all of the plugins will be in plugins/build/libs/plugins-1.0.jar. You can open this jar with a zip manager and delete the plugins you dont want to bundle. Feel free to rename the plugins-1.0.jar as well.
