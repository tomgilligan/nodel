package org.nodel.host;

/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.nodel.SimpleName;
import org.nodel.reflection.Schema;
import org.nodel.reflection.Serialisation;
import org.nodel.reflection.Value;

/**
 * Local binding information,
 */
public class LocalBindings {
    
    @Value(name = "actions", order = 1, genericClassA = SimpleName.class, genericClassB = Binding.class)
    public Map<SimpleName, Binding> actions = new LinkedHashMap<>();
    
    @Value(name = "events", order = 2, genericClassA = SimpleName.class, genericClassB = Binding.class)
    public Map<SimpleName, Binding> events = new LinkedHashMap<>();
    
    public String toString() {
        return Serialisation.serialise(this);
    }
    
    public void clear() {
        this.actions.clear();
        this.events.clear();
    }
    
    /**
     * (assumes 'clear' has been previously called.)
     */
    public void loadInfo(LocalBindings source) {
        if (source.actions != null)
            this.actions.putAll(source.actions);
        
        if (this.events != null)
            this.events.putAll(source.events);
    }
    
    public Map<String, Object> asSchema() {
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("title", "Local");
        schema.put("desc", "Holds the local bindings.");

        Map<String, Object> properties = new LinkedHashMap<String, Object>();

        schema.put("properties", properties);
        properties.put("actions", actionsAsSchema());
        properties.put("events", eventsAsSchema());
        return schema;        
    }
    
    class LocalBindingInfo {
        
        @Value(name = "title", title = "Title", desc = "A short title.", order = 2)
        public String title;
        
        @Value(name = "desc", title = "Description", desc = "A short description.", order = 3)
        public String desc;
        
        @Value(name = "group", title = "Group", desc = "A group name.", order = 5)
        public String group;
        
        @Value(name = "caution", title = "Caution", order = 6, desc = "A caution message if appropriate.")
        public String caution;        
        
        public LocalBindingInfo(Binding binding) {
            this.title = binding.title;
            this.desc = binding.desc;
            this.group = binding.group;
            this.caution = binding.caution;
        }
        
        public String toString() {
            return Serialisation.serialise(this); 
        }        
    }    

    private Map<String, Object> actionsAsSchema() {
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        schema.put("title", "Actions");
        schema.put("desc", "Holds local actions.");
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<String, Object>();

        schema.put("properties", properties);

        for (Entry<SimpleName, Binding> entry : this.actions.entrySet()) {
            String name = entry.getKey().getOriginalName();
            LocalBindingInfo bindingInfo = new LocalBindingInfo(entry.getValue());
            
            Map<String, Object> actionSchema = Schema.getSchemaObject(bindingInfo);
            actionSchema.put("title", name);
            properties.put(name, actionSchema);
        } // (for)

        return schema;
    } // (method)
    
    private Map<String, Object> eventsAsSchema() {
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        schema.put("title", "Events");
        schema.put("desc", "Holds local events.");
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<String, Object>();

        schema.put("properties", properties);

        for (Entry<SimpleName, Binding> entry : this.events.entrySet()) {
            String name = entry.getKey().getOriginalName();
            LocalBindingInfo bindingInfo = new LocalBindingInfo(entry.getValue());
            
            Map<String, Object> eventSchema = Schema.getSchemaObject(bindingInfo);
            eventSchema.put("title", name);
            properties.put(name, eventSchema);
        } // (for)

        return schema;
    } // (method)    
    
    public Map<String, Object> asValue() {
        Map<String, Object> value = new LinkedHashMap<String, Object>();
        
        Map<String, Object> actionsSection = new LinkedHashMap<String, Object>();

        for (Entry<SimpleName, Binding> entry : this.actions.entrySet()) {
            actionsSection.put(entry.getKey().getOriginalName(), entry.getValue());
        } // (for)
        
        Map<String, Object> eventsSection = new LinkedHashMap<String, Object>();
        
        for (Entry<SimpleName, Binding> entry : this.events.entrySet()) {
            eventsSection.put(entry.getKey().getOriginalName(), entry.getValue());
        } // (for)
        
        value.put("actions", actionsSection);
        value.put("events", eventsSection);

        return value;
    } // (method)    

} // (class)
