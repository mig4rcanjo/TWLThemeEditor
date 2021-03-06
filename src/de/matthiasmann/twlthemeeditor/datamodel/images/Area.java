/*
 * Copyright (c) 2008-2012, Matthias Mann
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
package de.matthiasmann.twlthemeeditor.datamodel.images;

import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twl.renderer.Texture;
import de.matthiasmann.twl.renderer.Texture.Rotation;
import de.matthiasmann.twlthemeeditor.datamodel.Image;
import de.matthiasmann.twlthemeeditor.datamodel.Images;
import de.matthiasmann.twlthemeeditor.datamodel.Split.Axis;
import de.matthiasmann.twlthemeeditor.dom.Element;
import de.matthiasmann.twlthemeeditor.properties.AttributeProperty;
import de.matthiasmann.twlthemeeditor.properties.BooleanProperty;
import de.matthiasmann.twlthemeeditor.properties.EnumProperty;
import de.matthiasmann.twlthemeeditor.properties.SplitProperty;

/**
 *
 * @author Matthias Mann
 */
public class Area extends Image {

    protected final ImageRectProperty rectProperty;
    protected final BooleanProperty tiledProperty;

    public Area(Images textures, TreeTableNode parent, Element node) {
        super(textures, parent, node);
        this.rectProperty = new ImageRectProperty(node);
        this.tiledProperty = new BooleanProperty(new AttributeProperty(element, "tiled", "Tiled", true), false);
        addProperty(rectProperty);
        addProperty(new HSplitProperty());
        addProperty(new VSplitProperty());
        addProperty(new BooleanProperty(new AttributeProperty(element, "nocenter", "No Center", true), false)
                .withTooltip("This parameter is only used when either Split X/Y is enabled"));
        addProperty(tiledProperty);
        addProperty(new RotationProperty());
    }

    @Override
    protected String getIcon() {
        return "image-area";
    }

    protected class HSplitProperty extends SplitProperty {
        public HSplitProperty() {
            super(new AttributeProperty(element, "splitx", "Split X positions", true), Axis.HORIZONTAL);
        }

        @Override
        public int getLimit() {
            return rectProperty.getPropertyValue().width;
        }
    }

    protected class VSplitProperty extends SplitProperty {
        public VSplitProperty() {
            super(new AttributeProperty(element, "splity", "Split Y positions", true), Axis.VERTICAL);
        }

        @Override
        public int getLimit() {
            return rectProperty.getPropertyValue().height;
        }
    }
    
    protected class RotationProperty extends EnumProperty<Texture.Rotation>  {
        public RotationProperty() {
            super(new AttributeProperty(element, "rot", "Rotation", true), Texture.Rotation.NONE);
        }

        @Override
        protected Rotation parse(String value) {
            int rot = Integer.parseInt(value);
            switch(rot) {
                case 90: return Rotation.CLOCKWISE_90;
                case 180: return Rotation.CLOCKWISE_180;
                case 270: return Rotation.CLOCKWISE_270;
                default: return null;
            }
        }

        @Override
        protected String toString(Rotation value) {
            switch(value) {
                default:
                    return null;
                case CLOCKWISE_90:
                    return "90";
                case CLOCKWISE_180:
                    return "180";
                case CLOCKWISE_270:
                    return "270";
            }
        }
    }
}
