# SpringLiveStomp
SpringLiveStomp is a spring library that works as a fast auto-update channel configuration.
It lets you define custom events and map those event to an endpoint, available for frontend subscription with client 
libraries.

## Client libraries
;:
Client libraries currently developed:

<ul>
    <li><a href="https://github.com/KatonKalu/ngx-livestomp">ngx-livestomp</a> an npm package for angular that serve as a client for SpringLiveStomp</li>
</ul>

### Installation
First, add the github repo for SpringLiveStomp in your pom.xml:
```xml
<repositories>
    <repository>
        <id>SpringLiveStomp-mvn-repo</id>
        <url>https://github.com/KatonKalu/SpringLiveStomp/raw/mvn-repo/</url>
        <snapshots>
            <enabled>true</enabled>
            <updatePolicy>always</updatePolicy>
        </snapshots>
    </repository>
</repositories>
```

Now just define the dependency in you project pom.xml like the following:

```xml
<dependency>
    <groupId>it.redbyte</groupId>
    <artifactId>spring-livestomp</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Usage

1) Define your WebSocketConfiguration class (see https://spring.io/guides/gs/messaging-stomp-websocket), for instance:

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // path for websocket connection
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // path to enable broker on (paths that will be used for sending messages back to the client)
        registry.enableSimpleBroker("/topic");


        registry.setApplicationDestinationPrefixes("/app");
    }
}
```
2) Define your event classes, extending ```ChangeEvent```, for instance:

```java
@ToString
public class HeroCreatedEvent extends ChangeEvent {
    public HeroCreatedEvent(Hero source) {
        super(source);
    }
    
    @Override
    public Hero getSource() {
        return (Hero) super.getSource();
    }

    @Override
    public ChangeEventType getChangeType() {
        return ChangeEventType.CREATED;
    }
}
```

3) Define a ```ChangeEventListenerConfig``` extending ```ChangeEventListenerConfigurer```, where you will put all your mappings:

```java
@Configuration
public class ChangeEventListenerConfig extends ChangeEventListenerConfigurer {

    @Autowired
    public ChangeEventListenerConfig(SimpMessagingTemplate messagingTemplate) {
        super(messagingTemplate);
    }

    @Override
    public void configureChangeEventMappings(ChangeEventMappings changeEventMappings) {
        // this maps events of type HeroChangeEvent.class (and subtypes) to 2 endpoints:
        // "/topic/heroes/updates"
        // "/topic/heroes/{heroID}/updates"
        // both of the endpoints will be available for subscription by the client
        // ATTENTION! in order to send messages to client, you must enable broker for the path.
        // See WebSocketConfig.
        changeEventMappings
                .mapEvent(HeroChangeEvent.class)
                    .toEndpoint("/topic/heroes/updates")
                    .toEndpoint((heroChangeEvent -> String.format("/topic/heroes/%s/updates", heroChangeEvent.getSource().getId())))
                    .and()
                .mapEvent(PosterChangeEvent.class)
                    .toEndpoint("/topic/poster/updates");
    }
}
```

4) Require ```ApplicationEventPublisher``` in your services via autowiring or where you need to publish an event:
```java
@Service
public class HeroesService {
    private final ApplicationEventPublisher eventPublisher;
    private final HeroesRepository heroesRepository;

    @Autowired
    public HeroesService(ApplicationEventPublisher eventPublisher, HeroesRepository heroesRepository) {
        this.eventPublisher = eventPublisher;
        this.heroesRepository = heroesRepository;
    }

    public Mono<Hero> addHero(Hero hero) {
        return heroesRepository.existsById(hero.getId())
                .flatMap(exists -> exists ?
                        Mono.error(new Exception("Hero existing")) :
                        heroesRepository.save(hero))
                .doOnSuccess(savedHero -> eventPublisher.publishEvent(new HeroCreatedEvent(savedHero)));
    }
}
```

When publishing events that were mapped in the listener with ```mapEvent(eventClass)```, the library automatically handles
sending a message to the client with the type of update and the 'subject' of the update, hence the source of the event.


For more usages examples see: https://github.com/KatonKalu/Stomp-Reactive-Example