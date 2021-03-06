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

import de.matthiasmann.twl.model.AbstractTreeTableNode;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CopyNodeOperation;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateChildOperation;
import de.matthiasmann.twlthemeeditor.datamodel.operations.DeleteNodeOperation;
import de.matthiasmann.twlthemeeditor.datamodel.operations.MoveNodeOperations;
import de.matthiasmann.twlthemeeditor.datamodel.operations.PasteNodeOperation;
import de.matthiasmann.twlthemeeditor.dom.Content;
import de.matthiasmann.twlthemeeditor.dom.Element;
import de.matthiasmann.twlthemeeditor.dom.Parent;
import de.matthiasmann.twlthemeeditor.dom.Parent.ContentListener;
import de.matthiasmann.twlthemeeditor.properties.NodeReferenceProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matthias Mann
 */
public abstract class ThemeTreeNode extends AbstractTreeTableNode {

    protected final ThemeFile themeFile;
    protected final Element element;
    protected final ArrayList<Property<?>> properties;
    protected final ContentListener contentListener;

    protected boolean error;

    protected ThemeTreeNode(ThemeFile themeFile, TreeTableNode parent, Element element) {
        super(parent);
        this.themeFile = themeFile;
        this.element = element;
        this.properties = new ArrayList<Property<?>>();
        setLeaf(true);
        
        contentListener = new ContentListener() {
            public void contentAdded(Parent parent, Content child, int index) {
                addChildren();
            }
            public void contentRemoved(Parent parent, Content child, int index) {
                addChildren();
            }
            public void contentMoved(Parent parent, Content child, int oldIndex, int newIndex) {
                addChildren();
            }
            void addChildren() {
                try {
                    ThemeTreeNode.this.addChildren();
                } catch(IOException ex) {
                    Logger.getLogger(ThemeTreeNode.class.getName()).log(Level.SEVERE,
                            "Could not update children", ex);
                }
            }
        };
        element.addContentListener(contentListener);
    }

    public final ThemeTreeModel getThemeTreeModel() {
        return (ThemeTreeModel)getTreeTableModel();
    }

    public final ThemeFile getThemeFile() {
        return themeFile;
    }

    public final ThemeFile getRootThemeFile() {
        return themeFile.getRootThemeFile();
    }

    public final Element getDOMElement() {
        return element;
    }

    public void setError(boolean hasError) {
        this.error = hasError;
        if(getParent() instanceof ThemeTreeNode) {
            ((ThemeTreeNode)getParent()).setError(hasError);
        }
    }

    public <E extends TreeTableNode> List<E> getChildren(Class<E> clazz) {
        ArrayList<E> result = new ArrayList<E>();
        for(int i=0,n=getNumChildren() ; i<n ; i++) {
            TreeTableNode child = getChild(i);
            if(clazz.isInstance(child)) {
                result.add(clazz.cast(child));
            }
        }
        return result;
    }
    
    public boolean canPasteElement(Element element) {
        return false;
    }

    public boolean childrenNeedName() {
        return false;
    }

    public abstract Kind getKind();
    
    public abstract void addToXPP(DomXPPParser xpp);
    
    public abstract void addChildren() throws IOException;

    protected void addChildren(ThemeFile themeFile, Element node, DomWrapper wrapper) throws IOException {
        IdentityHashMap<Element, TreeTableNode> existingNodes = new IdentityHashMap<Element, TreeTableNode>();
        for(int i=0,n=getNumChildren() ; i<n ; i++) {
            TreeTableNode ttn = getChild(i);
            if(ttn instanceof ThemeTreeNode) {
                Element e = ((ThemeTreeNode)ttn).getDOMElement();
                if(e != null) {
                    existingNodes.put(e, ttn);
                }
            }
        }

        int pos = 0;
        for(Content child : node) {
            if(child instanceof Element) {
                Element e = (Element)child;
                TreeTableNode ttn = existingNodes.remove(e);
                if(ttn != null) {
                    if(getChild(pos) != ttn) {
                        removeChild(ttn);
                        insertChild(ttn, pos);
                    }
                } else {
                    ttn = wrapper.wrap(themeFile, this, e);
                    if(ttn == null) {
                        ttn = new Unknown(this, e, themeFile);
                    }
                    if(ttn instanceof ThemeTreeNode) {
                        ((ThemeTreeNode)ttn).addChildren();
                    }
                    insertChild(ttn, pos);
                }
                pos++;
            }
        }

        for(TreeTableNode ttn : existingNodes.values()) {
            removeChild(ttn);
        }

        setLeaf(getNumChildren() == 0);
    }

    private void removeChild(TreeTableNode ttn) {
        int childIndex = super.getChildIndex(ttn);
        if(childIndex >= 0) {
            super.removeChild(childIndex);
        }
    }

    public List<ThemeTreeOperation> getOperations() {
        List<ThemeTreeOperation> result = new ArrayList<ThemeTreeOperation>();
        result.add(new DeleteNodeOperation(element, this));
        result.add(new MoveNodeOperations("opMoveNodeUp", element, this, -1));
        result.add(new MoveNodeOperations("opMoveNodeDown", element, this, +1));
        result.add(new CopyNodeOperation(element, this));
        return result;
    }

    public List<CreateChildOperation> getCreateChildOperations() {
        List<CreateChildOperation> result = new ArrayList<CreateChildOperation>();
        result.add(new PasteNodeOperation(this, element));
        return result;
    }

    public final MoveNodeOperations getMoveOperation(int dir) {
        for(ThemeTreeOperation operation : getOperations()) {
            if(operation instanceof MoveNodeOperations) {
                MoveNodeOperations moveNodeOperations = (MoveNodeOperations)operation;
                if(moveNodeOperations.getDirection() == dir) {
                    return moveNodeOperations;
                }
            }
        }
        return null;
    }

    public Object getData(int column) {
        switch (column) {
            case 0: {
                String displayName = getDisplayName();
                int flags = 0;
                if(error) {
                    flags |= DecoratedText.ERROR;
                }
                if(isModified()) {
                    flags |= DecoratedText.MODIFIED;
                }
                String icon = getIcon();
                if(icon != null) {
                    return new DecoratedTextWithIcon(displayName, flags, icon);
                }
                return DecoratedText.apply(displayName, flags);
            }
            case 1:
                return getType();
            default:
                return "";
        }
    }

    public String getDisplayName() {
        String name = getName();
        if(name == null && (getParent() instanceof NameGenerator)) {
            name = ((NameGenerator)getParent()).generateName(this);
        }
        if(name == null) {
            name = "Unnamed #" + (1+getParent().getChildIndex(this));
        }
        return name;
    }

    public abstract String getName();

    protected String getType() {
        return element.getName();
    }

    protected String getIcon() {
        return null;
    }
    
    protected boolean isModified() {
        return false;
    }

    public final void handleNodeRenamed(String from, String to, Kind kind) {
        for(Property<?> property : properties) {
            if(property instanceof NodeReferenceProperty) {
                ((NodeReferenceProperty)property).handleNodeRenamed(from, to, kind);
            }
        }
        for(int i=0,n=getNumChildren() ; i<n ; i++) {
            TreeTableNode child = getChild(i);
            if(child instanceof ThemeTreeNode) {
                ((ThemeTreeNode)child).handleNodeRenamed(from, to, kind);
            }
        }
    }

    public final ThemeTreeNode findNode(String name, Kind kind) {
        if(getKind() == kind && name.equals(getName())) {
            return this;
        }
        for(int i=0,n=getNumChildren() ; i<n ; i++) {
            TreeTableNode child = getChild(i);
            if(child instanceof ThemeTreeNode) {
                ThemeTreeNode result = ((ThemeTreeNode)child).findNode(name, kind);
                if(result != null) {
                    return result;
                }
            }
        }
        return null;
    }
    
    public final ThemeTreeNode findNode(long id) {
        if(element.getID() == id) {
            return this;
        }
        for(int i=0,n=getNumChildren() ; i<n ; i++) {
            TreeTableNode child = getChild(i);
            if(child instanceof ThemeTreeNode) {
                ThemeTreeNode result = ((ThemeTreeNode)child).findNode(id);
                if(result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    public final void collectNodes(String baseName, Kind kind, Collection<ThemeTreeNode> nodes) {
        final String ownName = getName();
        if(getKind() == kind && ownName != null && ownName.startsWith(baseName)) {
            nodes.add(this);
        }
        for(int i=0,n=getNumChildren() ; i<n ; i++) {
            TreeTableNode child = getChild(i);
            if(child instanceof ThemeTreeNode) {
                ((ThemeTreeNode)child).collectNodes(baseName, kind, nodes);
            }
        }
    }

    public Property<?>[] getProperties() {
        return properties.toArray(new Property<?>[properties.size()]);
    }

    protected final void addProperty(Property<?> property) {
        properties.add(property);
    }

    @Override
    public String toString() {
        String name = getName();
        if(name != null) {
            return getKind() + " " + name;
        } else if(getParent() != null) {
            return getParent().toString() + "/" + element.getName();
        } else {
            return "<"+element.getName()+">";
        }
    }

}
