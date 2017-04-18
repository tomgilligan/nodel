package org.nodel.jyhost;

import java.io.InputStream;

/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

import org.nodel.io.Stream;

/**
 * Built-in examples. 
 */
public class Examples {
    
    private String pysp_example;
    
    public String pysp_example() {
        return pysp_example;
    }
    
    private String py_example;
    
    public String py_example() {
        return py_example;
    }    
    
    /**
     * Extracts examples once.
     * 
     * (private constructor)
     */
    private Examples() {
        try {
            try (InputStream is = Examples.class.getResourceAsStream("example.pysp")) {
                pysp_example = Stream.readFully(is).replace("$VERSION", Launch.VERSION);
            }

            try (InputStream is = Examples.class.getResourceAsStream("example.py")) {
                py_example = Stream.readFully(is).replace("$VERSION", Launch.VERSION);
            }
        } catch (Exception exc) {
            throw new RuntimeException("Could not extract examples; build error likely", exc);
        }
    }

    /**
     * (singleton, thread-safe, non-blocking)
     */
    private static class Instance {
        private static final Examples INSTANCE = new Examples();
    }

    /**
     * Returns the singleton instance of this class.
     */
    public static Examples instance() {
        return Instance.INSTANCE;
    }    

}
