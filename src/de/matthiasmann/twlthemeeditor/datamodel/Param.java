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
package de.matthiasmann.twlthemeeditor.datamodel;

import de.matthiasmann.twl.Alignment;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twlthemeeditor.VirtualFile;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CloneNodeOperation;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateChildOperation;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateNewParam;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateNewParamEnum;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateNewParamFontDef;
import de.matthiasmann.twlthemeeditor.properties.AttributeProperty;
import de.matthiasmann.twlthemeeditor.properties.BooleanProperty;
import de.matthiasmann.twlthemeeditor.properties.BorderProperty;
import de.matthiasmann.twlthemeeditor.properties.DimensionProperty;
import de.matthiasmann.twlthemeeditor.properties.ElementTextProperty;
import de.matthiasmann.twlthemeeditor.properties.GapProperty;
import de.matthiasmann.twlthemeeditor.properties.HasProperties;
import de.matthiasmann.twlthemeeditor.properties.IntegerProperty;
import de.matthiasmann.twlthemeeditor.properties.NameProperty;
import de.matthiasmann.twlthemeeditor.properties.DerivedNodeReferenceProperty;
import de.matthiasmann.twlthemeeditor.properties.EnumProperty;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.jdom.Content;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public class Param extends ThemeTreeNode implements HasProperties {

    protected final Theme theme;
    protected final NameProperty nameProperty;
    protected final Element valueElement;

    protected String icon;
    
    protected Property<?> valueProperty;
    protected ArrayList<VirtualFile> virtualFontFiles;
    protected Property<String> fileNameProperty;

    protected DerivedNodeReferenceProperty refProperty;

    public Param(Theme theme, TreeTableNode parent, Element element) throws IOException {
        super(theme.getThemeFile(), parent, element);
        this.theme = theme;
        
        this.nameProperty = new NameProperty(new AttributeProperty(element, "name"), null, null, false) {
            @Override
            public void validateName(String name) throws IllegalArgumentException {
                if(name.endsWith(".") || name.startsWith(".")) {
                    throw new IllegalArgumentException("Name can not start or end with '.'");
                }
                if(name.indexOf('*') >= 0) {
                    throw new IllegalArgumentException("name can not contain '*'");
                }
            }
        };
        addProperty(nameProperty);

        valueElement = getFirstChildElement(element);
        if(valueElement != null) {
            if(isFontDef()) {
                initFontDef();
                icon = "param-fontdef";
            } else if(isInputMapDef()) {
                initInputMapDef();
                icon = "param-inputmapdef";
            } else {
                initValueProperty();

                String tagName = valueElement.getName();
                if("image".equals(tagName)) {
                    icon = "param-image";
                } else if("font".equals(tagName)) {
                    icon = "param-font";
                } else if("inputmap".equals(tagName)) {
                    icon = "param-inputmap";
                } else if("cursor".equals(tagName)) {
                    icon = "param-cursor";
                }
            }
        }
    }
    
    protected final boolean isMap() {
        return valueElement != null && "map".equals(valueElement.getName());
    }

    protected final boolean isFontDef() {
        return valueElement != null && "fontDef".equals(valueElement.getName());
    }

    protected final boolean isInputMapDef() {
        return valueElement != null && "inputMapDef".equals(valueElement.getName());
    }

    private void initFontDef() throws IOException {
        virtualFontFiles = new ArrayList<VirtualFile>();
        fileNameProperty = new AttributeProperty(valueElement, "filename", "Font file name", true);
        fileNameProperty.addValueChangedCallback(new Runnable() {
            public void run() {
                try {
                    registerFontFiles();
                } catch(IOException ignore) {
                }
            }
        });
        addProperty(fileNameProperty);
        FontDef.addCommonFontDefProperties(this, valueElement);
        registerFontFiles();
    }

    private void initInputMapDef() {
        refProperty = new DerivedNodeReferenceProperty(
                new AttributeProperty(valueElement, "ref", "Base input map reference", true),
                this, Kind.INPUTMAP);
        addProperty(refProperty);
    }

    private void initValueProperty() {
        valueProperty = createProperty(valueElement, this, theme.getLimit());
        if(valueProperty != null) {
            addProperty(valueProperty);
        }
    }

    @Override
    public String getName() {
        return nameProperty.getPropertyValue();
    }

    @Override
    protected String getIcon() {
        return icon;
    }

    boolean isWildcard() {
        return (valueProperty instanceof DerivedNodeReferenceProperty) &&
                ((DerivedNodeReferenceProperty)valueProperty).isWildcard();
    }

    @Override
    public String getDisplayName() {
        String name = getName();
        if(isWildcard()) {
            if(name.isEmpty()) {
                return "*";
            } else {
                return name.concat(".*");
            }
        }
        return name;
    }

    @Override
    public Object getTooltipContent(int column) {
        if(isWildcard()) {
            return "Wildcard reference";
        }
        return null;
    }

    public Kind getKind() {
        return Kind.NONE;
    }

    @Override
    protected String getType() {
        return "param-" + valueElement.getName();
    }

    public Element getValueElement() {
        return valueElement;
    }

    public Property<?> getValueProperty() {
        return valueProperty;
    }
    
    public void addChildren() throws IOException {
        if(isMap()) {
            addChildren(theme.getThemeFile(), valueElement, new DomWrapper() {
                public TreeTableNode wrap(ThemeFile themeFile, ThemeTreeNode parent, Element element) throws IOException {
                    if("param".equals(element.getName())) {
                        return new Param(theme, parent, element);
                    }
                    return null;
                }
            });
        }
        if(isFontDef()) {
            addChildren(theme.getThemeFile(), valueElement, new FontDef.DomWrapperImpl());
        }
        if(isInputMapDef()) {
            addChildren(theme.getThemeFile(), valueElement, new InputMapDef.DomWrapperImpl());
        }
    }

    @SuppressWarnings("unchecked")
    public void addToXPP(DomXPPParser xpp) {
        if(isMap()) {
            xpp.addStartTag(this, element.getName(), element.getAttributes());
            xpp.addStartTag(this, "map");
            Utils.addToXPP(xpp, this);
            xpp.addEndTag("map");
            xpp.addEndTag(element.getName());
        } else if(isFontDef()) {
            xpp.addStartTag(this, element.getName(), element.getAttributes());
            Utils.addToXPP(xpp, "fontDef", this, valueElement.getAttributes());
            xpp.addEndTag(element.getName());
        } else if(isInputMapDef()) {
            xpp.addStartTag(this, element.getName(), element.getAttributes());
            Utils.addToXPP(xpp, "inputMapDef", this, valueElement.getAttributes());
            xpp.addEndTag(element.getName());
        } else {
            xpp.addElement(this, element);
        }
    }

    @Override
    public List<ThemeTreeOperation> getOperations() {
        List<ThemeTreeOperation> operations = super.getOperations();
        operations.add(new CloneNodeOperation(element, this));
        return operations;
    }

    @Override
    public List<CreateChildOperation> getCreateChildOperations() {
        List<CreateChildOperation> operations = super.getCreateChildOperations();
        if(isMap()) {
            addCreateParam(operations, this, valueElement);
        }
        if(isFontDef()) {
            FontDef.addFontParamOperations(operations, this, valueElement);
        }
        if(isInputMapDef()) {
            InputMapDef.addInputMapActionOperations(operations, this, valueElement);
        }
        return operations;
    }


    private URL getFontFileURL() throws MalformedURLException {
        String value = fileNameProperty.getPropertyValue();
        return (value != null) ? themeFile.getURL(value) : null;
    }
    
    private void registerFontFiles() throws IOException {
        FontDef.registerFontFiles(getThemeFile().getEnv(), virtualFontFiles, getFontFileURL());
    }
    
    static void addCreateParam(List<CreateChildOperation> operations, ThemeTreeNode node, Element element) {
        operations.add(new CreateNewParam(element, "image", node, "none"));
        operations.add(new CreateNewParam(element, "border", node, "0"));
        operations.add(new CreateNewParam(element, "int", node, "0"));
        operations.add(new CreateNewParam(element, "bool", node, "false"));
        operations.add(new CreateNewParam(element, "gap", node, ""));
        operations.add(new CreateNewParam(element, "dimension", node, "0,0"));
        operations.add(new CreateNewParam(element, "string", node, ""));
        operations.add(new CreateNewParam(element, "font", node, "default"));
        operations.add(new CreateNewParamFontDef(element, node));
        operations.add(new CreateNewParam(element, "cursor", node, "text"));
        operations.add(new CreateNewParam(element, "map", node, "\n"));
        operations.add(new CreateNewParamEnum(element, node, "alignment", "CENTER"));
        operations.add(new CreateNewParam(element, "inputMap", node, "-defaultInputMap"));
        operations.add(new CreateNewParam(element, "inputMapDef", node, ""));
    }

    static Property<?> createProperty(Element e, ThemeTreeNode node, ThemeTreeNode limit) {
        String tagName = e.getName();
        if("image".equals(tagName)) {
            return new DerivedNodeReferenceProperty(new ElementTextProperty(e, "Image reference"), limit, Kind.IMAGE, true);
        }
        if("border".equals(tagName)) {
            return new BorderProperty(new ElementTextProperty(e, "Border"), 0, true);
        }
        if("int".equals(tagName)) {
            return new IntegerProperty(new ElementTextProperty(e, "Integer value"), Short.MIN_VALUE, Short.MAX_VALUE);
        }
        if("bool".equals(tagName)) {
            return new BooleanProperty(new ElementTextProperty(e, "Boolean value"), false);
        }
        if("gap".equals(tagName)) {
            return new GapProperty(new ElementTextProperty(e, "Layout gap"));
        }
        if("dimension".equals(tagName)) {
            return new DimensionProperty(new ElementTextProperty(e, "Dimesnion"));
        }
        if("string".equals(tagName)) {
            return new ElementTextProperty(e, "String value");
        }
        if("font".equals(tagName)) {
            return new DerivedNodeReferenceProperty(new ElementTextProperty(e, "Font reference"), limit, Kind.FONT);
        }
        if("cursor".equals(tagName)) {
            return new DerivedNodeReferenceProperty(new ElementTextProperty(e, "Cursor reference"), limit, Kind.CURSOR);
        }
        if("enum".equals(tagName)) {
            String type = e.getAttributeValue("type");
            if("alignment".equals(type)) {
                return createEnumProperty(e, Alignment.class);
            }
        }
        if("inputMap".equals(tagName)) {
            return new DerivedNodeReferenceProperty(new ElementTextProperty(e, "InputMap reference"), limit, Kind.INPUTMAP);
        }
        return null;
    }

    private static <T extends Enum<T>> EnumProperty<T> createEnumProperty(Element e, Class<T> enumType) {
        return EnumProperty.create(new ElementTextProperty(e, enumType.getSimpleName()), enumType);
    }

    private static Element getFirstChildElement(Element parent) {
        for(int i=0,n=parent.getContentSize() ; i<n ; i++) {
            Content content = parent.getContent(i);
            if(content instanceof Element) {
                return (Element)content;
            }
        }
        return null;
    }
}
