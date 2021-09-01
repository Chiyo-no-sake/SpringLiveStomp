package it.redbyte.springlivestomp;

import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Class behaviour:
 * eventMappings
 *      .mapEvent(Event.class)
 *          .endpoint("mapping1")
 *          .endpoint("mapping2")
 *          .subjectEndpoint((eventSubject) -> string)
 *          .and()
 *
 * eventMapping should now map event from the class Event.class
 * to /mapping1, /mapping2 and the endpoint created from the formatted string
 */
public class ChangeEventMappings {

    public static class ChangeEventMappingConfiguration<T extends ChangeEvent> {
        private final Class<T> eventClass;
        private final ChangeEventMappings father;

        protected ChangeEventMappingConfiguration(ChangeEventMappings father, Class<T> eventClass){
            this.eventClass = eventClass;
            this.father = father;
        }

        public ChangeEventMappingConfiguration<T> toEndpoint(String mapping){
            return toEndpoint((event) -> mapping);
        }

        public ChangeEventMappingConfiguration<T> toEndpoint(Function<T, String> endpointProvider) {
            this.father.mappings.get(eventClass).add(endpointProvider);
            return this;
        }

        public ChangeEventMappings and() {
            return father;
        }
    }

    private final Map<Class<? extends ChangeEvent>, Collection<Function<? extends ChangeEvent, String>>> mappings;

    public ChangeEventMappings() {
        mappings = new HashMap<>();
    }

    public <T extends ChangeEvent> ChangeEventMappingConfiguration<T> mapEvent(Class<T> eventClass){
        this.mappings.put(eventClass, new ArrayList<>());
        return new ChangeEventMappingConfiguration<>(this, eventClass);
    }

    public <T extends ChangeEvent> Flux<String> getForClass(Class<? extends ChangeEvent> eventClass, T event){
        return Flux.fromStream(this.mappings.keySet().parallelStream())
                .filter(mapClass -> mapClass.isAssignableFrom(eventClass))
                .map(this.mappings::get)
                .flatMap((mappings) -> Flux.fromStream(mappings.parallelStream()))
                .map((endpointProducer) -> ((Function<T, String>)endpointProducer).apply(event));
    }
}
