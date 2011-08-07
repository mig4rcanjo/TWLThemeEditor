/*
 * Copyright (c) 2008-2010, Matthias Mann
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.model.Property;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @param <T> type of value
 * @param <P> type of property holder
 * 
 * @author Matthias Mann
 */
public class PropertyAccessor<T, P extends Property<T>> {

    private final P property;
    private final BooleanModel activeModel;

    private T value;
    private Widget[] widgetsToEnable;
    private Runnable focusWidgetCB;

    public PropertyAccessor(P property, BooleanModel activeModel) {
        this.property = property;
        this.activeModel = activeModel;

        value = property.getPropertyValue();

        if(activeModel != null) {
            activeModel.setValue(value != null);
            activeModel.addCallback(new Runnable() {
                public void run() {
                    setPropertyValue();
                    syncWithActive();
                }
            });
        }
    }

    public void setWidgetsToEnable(Widget ... widgetsToEnable) {
        this.widgetsToEnable = widgetsToEnable;
        syncWithActive();
    }

    public void setFocusWidgetCB(Runnable focusWidgetCB) {
        this.focusWidgetCB = focusWidgetCB;
    }

    public void focusWidget() {
        if(focusWidgetCB != null) {
            focusWidgetCB.run();
        }
    }
    
    public boolean isActive() {
        return (activeModel == null) || activeModel.getValue();
    }

    public void setActive(boolean active) {
        if(activeModel != null) {
            activeModel.setValue(active);
        }
    }
    
    public void addActiveCallback(Runnable cb) {
        if(activeModel != null) {
            activeModel.addCallback(cb);
        }
    }
    
    public T getValue(T defaultValue) {
        T storedValue = property.getPropertyValue();
        if(activeModel != null) {
            activeModel.setValue(storedValue != null);
        }
        if(storedValue != null) {
            value = storedValue;
        }
        return (value != null) ? value : defaultValue;
    }
    
    public void setValue(T value) {
        this.value = value;
        setPropertyValue();
    }

    public boolean hasValue() {
        return (value != null) || (property.getPropertyValue() != null);
    }
    
    public String getDisplayName() {
        return property.getName();
    }

    public P getProperty() {
        return property;
    }

    void setPropertyValue() {
        try {
            T effectiveValue = isActive() ? value : null;
            if(effectiveValue != null || property.canBeNull()) {
                property.setPropertyValue(effectiveValue);
            }
        } catch (Exception ex) {
            Logger.getLogger(PropertyAccessor.class.getName()).log(Level.SEVERE,
                    "Could not set property value", ex);
        }
    }

    void syncWithActive() {
        if(widgetsToEnable != null) {
            boolean isActive = isActive();
            for (Widget w : widgetsToEnable) {
                w.setEnabled(isActive);
            }
        }
    }
}
