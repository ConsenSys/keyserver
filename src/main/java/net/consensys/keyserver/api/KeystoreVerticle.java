package net.consensys.keyserver.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeystoreVerticle extends AbstractVerticle {
	
	private static final Logger log = LoggerFactory.getLogger(KeystoreVerticle.class);
	private EventBus eb;
	
	@Override
	public void start() {
		 eb = vertx.eventBus();
		 eb.consumer("keystore.get", this::doGet);
		 eb.consumer("keystore.post", this::doPost);
		 eb.consumer("keystore.put", this::doPut);
		 eb.consumer("keystore.delete", this::doDelete);
		 log.info("Keystore Verticle ok");
	}
	
	private void doGet(Message<JsonObject> event) {
		String identifier = event.body().getString("identifier");
		String providedToken = event.body().getString("token");
		JsonObject mongo = new JsonObject()
				.put("action", "find")
				.put("collection", "keystores")
				.put("matcher", new JsonObject().put("identifier", identifier));
		
		eb.send("vertx.mongopersistor", mongo,  mongoEvent -> {
				if (mongoEvent.succeeded()) {
					JsonObject mongoResult = (JsonObject)mongoEvent.result().body();
					JsonArray results = mongoResult.getJsonArray("results");
					if (results.size() > 0 ) {
						JsonObject first = results.getJsonObject(0);
						String token = first.getString("token");
						if(token.equals(providedToken)){
	 						replySuccess(event,first);
						}else{
							replyFail(event,new JsonObject().put("message","bad token"));
						}
					}else{
						replyFail(event,new JsonObject().put("message","identifier:"+identifier+" not found"));
					}
				} else {
					log.error("error reading from mongo", mongoEvent.cause());
					replyError(event,"error reading from mongo:" + mongoEvent.cause().getMessage());
				}
			}
		);
		
	}
	
	private void doPost(Message<JsonObject> event) {
		JsonObject keystoreDocument = makeKeyStoreDocument(event);
		
		JsonObject mongo = new JsonObject()
				.put("action", "save")
				.put("collection", "keystores")
				.put("document", keystoreDocument);
		log.debug("Storing keystore:" + keystoreDocument);
		
		eb.send("vertx.mongopersistor", mongo, mongoEvent -> {
				if (mongoEvent.succeeded()) {
					replySuccess(event,(JsonObject)mongoEvent.result().body());
				} else {
					if(mongoEvent.cause().getMessage().startsWith("E11000")){
						replyFail(event,(new JsonObject()).put("message","identifier '"+event.body().getString("identifier")+"' already used"));
					}else{
						log.error("error storing in mongo", mongoEvent.cause());
						replyError(event,"error storing in mongo:" + mongoEvent.cause().getMessage());
					}
				}
			}
		);
	}
	
	

	private void doPut(Message<JsonObject> event) {
		String identifier = event.body().getString("identifier");
		String providedToken = event.body().getString("token");
		JsonObject mongo = new JsonObject()
				.put("action", "find")
				.put("collection", "keystores")
				.put("matcher", new JsonObject().put("identifier", identifier));
		
		eb.send("vertx.mongopersistor", mongo,  mongoEvent -> {
				if (mongoEvent.succeeded()) {
					JsonObject mongoResult = (JsonObject)mongoEvent.result().body();
					JsonArray results = mongoResult.getJsonArray("results");
					if (results.size() > 0 ) {
						JsonObject first = results.getJsonObject(0);
						String token = first.getString("token");
						if(token.equals(providedToken)){
							event.body().put("_id", first.getString("_id"));
							doPost(event);
						}else{
							replyFail(event,new JsonObject().put("message","bad token"));
						}
					}else{
						replyFail(event,new JsonObject().put("message","identifier:"+identifier+" not found"));
					}
				} else {
					log.error("error reading from mongo", mongoEvent.cause());
					replyError(event,"error reading from mongo:" + mongoEvent.cause().getMessage());
				}
			}
		);
	}
	
	private void doDelete(Message<JsonObject> event) {
		String identifier = event.body().getString("identifier");
		String providedToken = event.body().getString("token");
		JsonObject mongo = new JsonObject()
				.put("action", "removeOne")
				.put("collection", "keystores")
				.put("matcher", new JsonObject()
							.put("identifier", identifier)
							.put("token", providedToken)
				);
		
		eb.send("vertx.mongopersistor", mongo,  mongoEvent -> {
				if (mongoEvent.succeeded()) {
					replySuccess(event,(JsonObject)mongoEvent.result().body());
				} else {
					log.error("error removing from mongo", mongoEvent.cause());
					replyError(event,"error removing from mongo:" + mongoEvent.cause().getMessage());
				}
			}
		);
	}
	
	private JsonObject makeKeyStoreDocument(Message<JsonObject> event){
		JsonObject doc= new JsonObject()
			.put("identifier", event.body().getString("identifier"))
			.put("keystore", event.body().getJsonObject("requestBody").getJsonObject("keystore"))
			.put("token", event.body().getJsonObject("requestBody").getString("token"));
		
		if(event.body().containsKey("_id")){
			doc.put("_id", event.body().getString("_id"));
		}
		
		return doc;
	}
	
	private void replySuccess(Message<JsonObject> event, JsonObject data) {
		JsonObject reply= new JsonObject()
				.put("status", "success")
				.put("data", data);
		event.reply(reply);
	}

	private void replyFail(Message<JsonObject> event, JsonObject data) {
		JsonObject reply= new JsonObject()
				.put("status", "fail")
				.put("data", data);
		event.reply(reply);
	}
	

	private void replyError(Message<JsonObject> event, String message) {
		replyError(event,message,-1,null);
	}
	
	private void replyError(Message<JsonObject> event, String message, int code, JsonObject data) {
		JsonObject reply= new JsonObject()
				.put("status", "error")
				.put("message", message);
		if(code!= -1){
			reply.put("code",code);
		}
		if(data!=null){
			reply.put("data", data);
		}
		event.reply(reply);
	}
	
}

