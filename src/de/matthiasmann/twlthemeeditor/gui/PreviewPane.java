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

import de.matthiasmann.twl.AnimationState;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.FileSelector;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.Menu;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.SplitPane;
import de.matthiasmann.twl.TableRowSelectionManager;
import de.matthiasmann.twl.TextArea;
import de.matthiasmann.twl.TreeTable;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.AbstractProperty;
import de.matthiasmann.twl.model.IntegerModel;
import de.matthiasmann.twl.model.JavaFileSystemModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.SimpleTextAreaModel;
import de.matthiasmann.twl.model.TableSingleSelectionModel;
import de.matthiasmann.twl.model.TextAreaModel;
import de.matthiasmann.twl.utils.ClassUtils;
import de.matthiasmann.twlthemeeditor.datamodel.Kind;
import de.matthiasmann.twlthemeeditor.gui.MainUI.ExtFilter;
import de.matthiasmann.twlthemeeditor.properties.BoundProperty;
import de.matthiasmann.twlthemeeditor.properties.NodeReferenceProperty;
import de.matthiasmann.twlthemeeditor.properties.RectProperty;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 *
 * @author Matthias Mann
 */
public class PreviewPane extends DialogLayout {

    private final PreviewWidget previewWidget;
    private final Label labelErrorDisplay;
    private final Button btnClearStackTrace;
    private final Button btnShowStackTrace;
    private final SplitPane splitPane;
    private final ScrollPane widgetPropertiesScrollPane;
    private final Menu testWidgetMenu;
    private final TestWidgetManager testWidgetManager;

    private Context ctx;
    
    public PreviewPane() {
        this.previewWidget = new PreviewWidget();
        this.labelErrorDisplay = new Label() {
            @Override
            public int getMinWidth() {
                return 0;
            }
        };
        this.btnClearStackTrace = new Button("Clear");
        this.btnShowStackTrace = new Button("Stack Trace");

        testWidgetMenu = new Menu("Widgets");
        testWidgetManager = new TestWidgetManager();

        labelErrorDisplay.setTheme("errorDisplay");
        labelErrorDisplay.setClip(true);
        btnClearStackTrace.setEnabled(false);
        btnShowStackTrace.setEnabled(false);

        widgetPropertiesScrollPane = new ScrollPane();
        widgetPropertiesScrollPane.setTheme("propertyEditor");
        widgetPropertiesScrollPane.setFixed(ScrollPane.Fixed.HORIZONTAL);

        splitPane = new SplitPane();
        splitPane.setDirection(SplitPane.Direction.HORIZONTAL);
        splitPane.setReverseSplitPosition(true);
        splitPane.setSplitPosition(200);

        splitPane.add(previewWidget);
        splitPane.add(widgetPropertiesScrollPane);

        setHorizontalGroup(createParallelGroup()
                .addWidget(splitPane)
                .addGroup(createSequentialGroup(labelErrorDisplay, btnClearStackTrace, btnShowStackTrace)));
        setVerticalGroup(createSequentialGroup()
                .addWidget(splitPane)
                .addGroup(createParallelGroup(labelErrorDisplay, btnClearStackTrace, btnShowStackTrace)));

        previewWidget.getExceptionHolder().addCallback(new Runnable() {
            public void run() {
                updateException();
            }
        });
        btnClearStackTrace.addCallback(new Runnable() {
            public void run() {
                clearException();
            }
        });
        btnShowStackTrace.addCallback(new Runnable() {
            public void run() {
                showStackTrace();
            }
        });
        previewWidget.setTestWidgetChangedCB(new Runnable() {
            public void run() {
                updateTestWidgetProperties();
            }
        });
        testWidgetManager.setCallback(new Runnable() {
            public void run() {
                changeTestWidget();
            }
        });

        updateTestWidgetMenu();
        changeTestWidget();
    }

    public void setURL(URL url) {
        previewWidget.setURL(url);
    }

    public void reloadTheme() {
        previewWidget.reloadTheme();
    }

    public Image getImage(String name) {
        return previewWidget.getImage(name);
    }

    public Object getThemeLoadErrorLocation() {
        return previewWidget.getThemeLoadErrorLocation();
    }

    public void removeCallback(Runnable cb) {
        previewWidget.getExceptionHolder().removeCallback(cb);
    }

    public void addCallback(Runnable cb) {
        previewWidget.getExceptionHolder().addCallback(cb);
    }

    public void setContext(Context ctx) {
        this.ctx = ctx;
    }

    public Menu getTestWidgetMenu() {
        return testWidgetMenu;
    }

    void updateTestWidgetMenu() {
        testWidgetMenu.clear();
        testWidgetManager.updateMenu(testWidgetMenu);
        testWidgetMenu.addSpacer();
        testWidgetMenu.add("Recreate Widgets", new Runnable() {
            public void run() {
                recreateTestWidgets();
            }
        });
        //btnRecreateTestWidgets.setTooltipContent("Clears widget cache and recreates current widget");

        testWidgetMenu.add("Load Widget", new Runnable() {
            public void run() {
                loadUserWidget();
            }
        });
        //btnLoadUserWidget.setTooltipContent("Load a Widget from a user supplied JAR file");
    }
    
    void updateException() {
        int nr = selectException();
        if(nr >= 0) {
            ExceptionHolder exceptionHolder = previewWidget.getExceptionHolder();
            Throwable ex = exceptionHolder.getException(nr);
            labelErrorDisplay.setText(exceptionHolder.getExceptionName(nr) + " exception: " + ex.getMessage());
            btnClearStackTrace.setEnabled(true);
            btnShowStackTrace.setEnabled(true);
        } else {
            labelErrorDisplay.setText("");
            btnClearStackTrace.setEnabled(false);
            btnShowStackTrace.setEnabled(false);
        }
    }

    void clearException() {
        clearException(selectException());
    }

    void clearException(int nr) {
        if(nr >= 0) {
            previewWidget.clearException(nr);
        }
    }

    void showStackTrace() {
        final int nr = selectException();
        if(nr < 0) {
            return;
        }
        
        final Throwable ex = previewWidget.getExceptionHolder().getException(nr);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        pw.flush();

        TextAreaModel model = new SimpleTextAreaModel(sw.toString());

        TextArea textArea = new TextArea(model);
        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFixed(ScrollPane.Fixed.HORIZONTAL);

        Button btnClear = new Button("Clear Exception and Close");
        Button btnClose = new Button("Close");

        DialogLayout layout = new DialogLayout();
        layout.setHorizontalGroup(layout.createParallelGroup()
                .addWidget(scrollPane)
                .addGroup(layout.createSequentialGroup().addGap().addWidgets(btnClear, btnClose)));
        layout.setVerticalGroup(layout.createSequentialGroup()
                .addWidget(scrollPane)
                .addGroup(layout.createParallelGroup().addWidgets(btnClear, btnClose)));

        final PopupWindow popupWindow = new PopupWindow(this);
        popupWindow.setTheme("previewExceptionPopup");
        popupWindow.setCloseOnClickedOutside(false);
        popupWindow.setCloseOnEscape(true);
        popupWindow.add(layout);

        btnClear.addCallback(new Runnable() {
            public void run() {
                clearException(nr);
                popupWindow.closePopup();
            }
        });
        btnClose.addCallback(new Runnable() {
            public void run() {
                popupWindow.closePopup();
            }
        });

        GUI gui = getGUI();
        popupWindow.setSize(gui.getWidth()*4/5, gui.getHeight()*4/5);
        popupWindow.setPosition(
                (gui.getWidth() - popupWindow.getWidth())/2,
                (gui.getHeight() - popupWindow.getHeight())/2);
        popupWindow.openPopup();
    }

    private int selectException() {
        return previewWidget.getExceptionHolder().getHighestPriority();
    }

    void changeTestWidget() {
        previewWidget.setWidgetFactory(testWidgetManager.getCurrentTestWidgetFactory());
    }

    void recreateTestWidgets() {
        testWidgetManager.clearCache();
        changeTestWidget();
    }

    void loadUserWidget() {
        final PopupWindow popupWindow = new PopupWindow(this);
        JavaFileSystemModel fsm = new JavaFileSystemModel();
        FileSelector.NamedFileFilter filter = new FileSelector.NamedFileFilter(
                "JAR files", new ExtFilter(".jar"));
        FileSelector fileSelector = new FileSelector(
                Preferences.userNodeForPackage(PreviewPane.class),
                "userWidgetJARs");
        fileSelector.setFileSystemModel(fsm);
        fileSelector.addFileFilter(FileSelector.AllFilesFilter);
        fileSelector.addFileFilter(filter);
        fileSelector.setFileFilter(filter);
        fileSelector.setAllowMultiSelection(true);
        fileSelector.addCallback(new FileSelector.Callback() {
            public void filesSelected(Object[] files) {
                if(testWidgetManager.loadUserWidgets(files)) {
                    updateTestWidgetMenu();
                }
                popupWindow.closePopup();
            }
            public void canceled() {
                popupWindow.closePopup();
            }
        });
        popupWindow.setTheme("fileselector-popup");
        popupWindow.add(fileSelector);
        popupWindow.setSize(getWidth()*4/5, getHeight()*4/5);
        popupWindow.setPosition(
                getWidth()/2 - popupWindow.getWidth()/2,
                getHeight()/2 - popupWindow.getHeight()/2);
        popupWindow.openPopup();
    }

    void updateTestWidgetProperties() {
        final Widget testWidget = previewWidget.getTestWidget();
        if(testWidget == null || ctx == null) {
            widgetPropertiesScrollPane.setContent(null);
            return;
        }
        ArrayList<Property<?>> properties = new ArrayList<Property<?>>();
        properties.add(new WidgetRectProperty(testWidget));
        properties.add(new NodeReferenceProperty(new AbstractProperty<String>() {
            public boolean canBeNull() {
                return false;
            }
            public String getName() {
                return "Theme name";
            }
            public String getPropertyValue() {
                return testWidget.getTheme();
            }
            public Class<String> getType() {
                return String.class;
            }
            public boolean isReadOnly() {
                return false;
            }
            public void setPropertyValue(String value) throws IllegalArgumentException {
                testWidget.setTheme(value);
                testWidget.reapplyTheme();
            }
        }, null, Kind.THEME));
        properties.add(new AbstractProperty<AnimationState>() {
            public boolean canBeNull() {
                return false;
            }
            public String getName() {
                return "Animation state";
            }
            public AnimationState getPropertyValue() {
                return testWidget.getAnimationState();
            }
            public Class<AnimationState> getType() {
                return AnimationState.class;
            }
            public boolean isReadOnly() {
                return true;
            }
            public void setPropertyValue(AnimationState value) throws IllegalArgumentException {
                throw new UnsupportedOperationException("Not supported");
            }
        });
        properties.add(new BoundProperty<Boolean>(testWidget, "locallyEnabled",
                BoundProperty.getReadMethod(testWidget, "locallyEnabled", Boolean.class),
                BoundProperty.getWriteMethod(testWidget, "enabled", boolean.class),
                Boolean.class) {

            @Override
            public String getName() {
                return "Enabled";
            }
        });
        addBeanProperties(testWidget, properties);
        
        PropertyPanel panel = new PropertyPanel(ctx, properties.toArray(new Property<?>[properties.size()]));

        WidgetTreeModel wtm = new WidgetTreeModel();
        wtm.createTreeFromWidget(testWidget);
        TreeTable tt = new TreeTable(wtm);
        tt.setSelectionManager(new TableRowSelectionManager(new TableSingleSelectionModel()));
        tt.setTheme("themetree");
        CollapsiblePanel cp = new CollapsiblePanel(CollapsiblePanel.Direction.VERTICAL, "Theme tree", tt, null);
        panel.getHorizontalGroup().addWidget(cp);
        panel.getVerticalGroup().addWidget(cp);

        widgetPropertiesScrollPane.setContent(panel);
    }

    @SuppressWarnings("unchecked")
    private void addBeanProperties(final Widget testWidget, ArrayList<Property<?>> properties) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(testWidget.getClass());
            for(PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                if(pd.getReadMethod() != null && pd.getWriteMethod() != null) {
                    if(pd.getReadMethod().getDeclaringClass() != Widget.class) {
                        properties.add(new BoundProperty(testWidget, pd,
                                ClassUtils.mapPrimitiveToWrapper(pd.getPropertyType())));
                    }
                }
            }
        } catch(Throwable ex) {
            Logger.getLogger(PreviewPane.class.getName()).log(Level.SEVERE, "can't collect bean properties", ex);
        }
    }

    static abstract class BoundIntegerProperty extends BoundProperty<Integer> implements IntegerModel {
        public BoundIntegerProperty(Object bean, String name) {
            super(bean, name, Integer.class);
        }
        public void addCallback(Runnable callback) {
            addValueChangedCallback(callback);
        }
        public void removeCallback(Runnable callback) {
            removeValueChangedCallback(callback);
        }
        public int getMinValue() {
            return 0;
        }
        public int getValue() {
            return getPropertyValue();
        }
        @Override
        public boolean isReadOnly() {
            return false;
        }
    }

    static class WidgetRectProperty extends RectProperty {
        final Widget widget;
        public WidgetRectProperty(final Widget widget) {
            super(new BoundIntegerProperty(widget, "x") {
                public int getMaxValue() {
                    return widget.getParent().getInnerWidth()-1;
                }
                public void setValue(int value) {
                    widget.setPosition(value, widget.getY());
                }
            }, new BoundIntegerProperty(widget, "y") {
                public int getMaxValue() {
                    return widget.getParent().getInnerHeight()-1;
                }
                public void setValue(int value) {
                    widget.setPosition(widget.getX(), value);
                }
            }, new BoundIntegerProperty(widget, "width") {
                public int getMaxValue() {
                    return widget.getParent().getInnerWidth();
                }
                public void setValue(int value) {
                    widget.setSize(value, widget.getHeight());
                }
            }, new BoundIntegerProperty(widget, "height") {
                public int getMaxValue() {
                    return widget.getParent().getInnerHeight();
                }
                public void setValue(int value) {
                    widget.setSize(widget.getWidth(), value);
                }
            }, "Widget position & size");
            this.widget = widget;
        }

        @Override
        public Dimension getLimit() {
            Widget parent = widget.getParent();
            return new Dimension(parent.getInnerWidth(), parent.getInnerHeight());
        }
    }
    
}
