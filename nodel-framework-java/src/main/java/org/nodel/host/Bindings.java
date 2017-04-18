package org.nodel.host;

/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

import org.nodel.reflection.Serialisation;
import org.nodel.reflection.Value;

/**
 * Holds all local / remote action / event binding information for a dynamic node.
 */
public class Bindings {
    
    @Value(name = "desc", title = "Description", order = 0.5, desc = "The description of the node.")
    public String desc;
    
    @Value(name = "local", title = "Local", order = 1, desc = "The actions and events this node provides (local).")
    public LocalBindings local = new LocalBindings();
    
    @Value(name = "remote", title = "Remote", order = 2, desc = "The actions and events this node requires of remote (or peer) nodes.")
    public RemoteBindings remote = new RemoteBindings();
    
    @Value(name = "params", title = "Parameters", order = 3, desc = "The global parameters used within the script.", genericClassA = String.class, genericClassB = ParameterBinding.class)
    public ParameterBindings params = new ParameterBindings();
    
    public void clear() {
        this.desc = null;
        this.local.clear();
        this.remote.clear();
        this.params.clear();
    }
    
    public void loadInto(Bindings source) {
        this.desc = source.desc;
        
        if (source.local != null)
            this.local.loadInfo(source.local);
        
        if (source.remote != null)
            this.remote.loadInfo(source.remote);
        
        if (source.params != null)
            this.params.putAll(source.params);
    }    
    
    public String toString() {
        return Serialisation.serialise(this);
    }

}


