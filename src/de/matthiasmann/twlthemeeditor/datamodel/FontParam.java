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
package de.matthiasmann.twlthemeeditor.datamodel;

import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CloneNodeOperation;
import de.matthiasmann.twlthemeeditor.dom.Element;
import de.matthiasmann.twlthemeeditor.properties.AttributeProperty;
import de.matthiasmann.twlthemeeditor.properties.ColorProperty;
import de.matthiasmann.twlthemeeditor.properties.ConditionProperty;
import de.matthiasmann.twlthemeeditor.properties.HasProperties;
import de.matthiasmann.twlthemeeditor.properties.IntegerProperty;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Matthias Mann
 */
public class FontParam extends ThemeTreeNode implements HasProperties {

    protected final ConditionProperty conditionProperty;

    public FontParam(ThemeFile themeFile, TreeTableNode parent, Element element) {
        super(themeFile, parent, element);

        conditionProperty = new ConditionProperty(
                new AttributeProperty(element, "if", "if", true),
                new AttributeProperty(element, "unless", "unless", true),
                "Condition");
        addProperty(conditionProperty);

        addProperty(new ColorProperty(new AttributeProperty(element, "color", "Font color", true), this));
        addProperty(new IntegerProperty(new AttributeProperty(element, "offsetX", "Offset X", true), -100, 100));
        addProperty(new IntegerProperty(new AttributeProperty(element, "offsetY", "Offset Y", true), -100, 100));
    }

    @Override
    public String getName() {
        Condition condition = conditionProperty.getPropertyValue();
        return condition.getType().name() + "(" + condition.getCondition() + ")";
    }

    public Kind getKind() {
        return Kind.NONE;
    }

    public void addChildren() throws IOException {
    }

    public void addToXPP(DomXPPParser xpp) {
        xpp.addElement(this, element);
    }

    @Override
    public List<ThemeTreeOperation> getOperations() {
        List<ThemeTreeOperation> operations = super.getOperations();
        operations.add(new CloneNodeOperation(element, this));
        return operations;
    }

}
