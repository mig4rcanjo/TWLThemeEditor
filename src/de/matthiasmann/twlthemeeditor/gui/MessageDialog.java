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

import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.SplitPane;
import de.matthiasmann.twl.Table;
import de.matthiasmann.twl.TableRowSelectionManager;
import de.matthiasmann.twl.TextArea;
import de.matthiasmann.twl.model.AbstractTableModel;
import de.matthiasmann.twl.model.HasCallback;
import de.matthiasmann.twl.model.TableSelectionModel;
import de.matthiasmann.twl.model.TableSingleSelectionModel;
import de.matthiasmann.twl.model.TextAreaModel;
import de.matthiasmann.twl.model.TextAreaModel.Clear;
import de.matthiasmann.twl.model.TextAreaModel.Element;
import de.matthiasmann.twl.model.TextAreaModel.HAlignment;
import de.matthiasmann.twl.model.TextAreaModel.VAlignment;
import de.matthiasmann.twlthemeeditor.datamodel.DecoratedText;
import de.matthiasmann.twlthemeeditor.gui.MessageLog.Entry;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author Matthias Mann
 */
public class MessageDialog extends DialogLayout {

    static final DateFormat TIME_FORMAT = DateFormat.getTimeInstance();

    private final MessageLog messageLog;
    private final TableModel tableModel;
    private final TableSelectionModel selectionModel;
    private final Table table;
    private final MessageTextAreaModel textModel;
    private final Button btnDiscard;
    private final Button btnClose;

    private MessageLog.Entry[] entries;

    public MessageDialog(MessageLog messageLog, MessageLog.Entry selectedEntry) {
        this.messageLog = messageLog;
        this.entries = messageLog.getEntries();
        this.tableModel = new TableModel();
        this.selectionModel = new TableSingleSelectionModel();
        this.table = new Table(tableModel);
        this.textModel = new MessageTextAreaModel();

        ScrollPane spTable = new ScrollPane(table);
        spTable.setFixed(ScrollPane.Fixed.HORIZONTAL);
        spTable.setExpandContentSize(true);

        TextArea textArea = new TextArea(textModel);
        ScrollPane spTextArea = new ScrollPane(textArea);
        spTextArea.setFixed(ScrollPane.Fixed.HORIZONTAL);
        spTextArea.setExpandContentSize(true);

        SplitPane splitPane = new SplitPane();
        splitPane.setDirection(SplitPane.Direction.VERTICAL);
        splitPane.add(spTable);
        splitPane.add(spTextArea);

        btnDiscard = new Button("Discard message");
        btnClose = new Button("Close");

        btnDiscard.addCallback(new Runnable() {
            public void run() {
                removeSelectedMsg();
            }
        });
        selectionModel.addSelectionChangeListener(new Runnable() {
            public void run() {
                updateText();
            }
        });

        DecoratedTextRenderer.install(table);
        table.setSelectionManager(new TableRowSelectionManager(selectionModel));

        setHorizontalGroup(createParallelGroup()
                .addWidget(splitPane)
                .addGroup(createSequentialGroup().addGap().addWidgets(btnDiscard, btnClose)));
        setVerticalGroup(createSequentialGroup()
                .addWidget(splitPane)
                .addGroup(createParallelGroup().addWidgets(btnDiscard, btnClose)));

        updateText();
        setSelected(selectedEntry);
    }

    public void addCloseCallback(Runnable callback) {
        btnClose.addCallback(callback);
    }

    void updateText() {
        Entry entry = getSelectedEntry();
        if(entry != null) {
            textModel.set(entry.getDetailText(), entry.getDetailException());
            btnDiscard.setEnabled(true);
        } else {
            textModel.clear();
            btnDiscard.setEnabled(false);
        }
    }

    void removeSelectedMsg() {
        Entry entry = getSelectedEntry();
        if(entry != null) {
            messageLog.remove(entry);
            entries = messageLog.getEntries();
            if(entries.length == 0) {
                btnClose.getModel().fireActionCallback();
            }
            tableModel.updateTable();
            setSelected(null);
        }
    }

    private Entry getSelectedEntry() {
        int nr = selectionModel.getFirstSelected();
        return (nr < 0) ? null : entries[nr];
    }

    private void setSelected(MessageLog.Entry entry) {
        int selectedIdx = entries.length - 1;
        for(int i=0,n=entries.length ; i<n ; ++i) {
            if(entries[i] == entry) {
                selectedIdx = i;
                break;
            }
        }
        selectionModel.setSelection(selectedIdx, selectedIdx);
        table.scrollToRow(selectedIdx);
    }

    static final String TABLE_COLUMN_NAMES[] = {"Time", "Category", "Message"};

    class TableModel extends AbstractTableModel {
        public int getNumColumns() {
            return TABLE_COLUMN_NAMES.length;
        }
        public String getColumnHeaderText(int column) {
            return TABLE_COLUMN_NAMES[column];
        }
        public int getNumRows() {
            return entries.length;
        }
        public Object getCell(int row, int column) {
            MessageLog.Entry entry = entries[row];
            String text;
            switch (column) {
                case 0:
                    text = TIME_FORMAT.format(entry.getTime());
                    break;
                case 1:
                    text = entry.getCategory().toString();
                    break;
                case 2:
                    text = entry.getMessage();
                    break;
                default:
                    text = "";
            }
            return DecoratedText.apply(text, entry.getCategory().getFlags());
        }
        void updateTable() {
            fireAllChanged();
        }
    }

    static class MessageTextAreaModel extends HasCallback implements TextAreaModel {
        private final ArrayList<Element> elements = new ArrayList<Element>(2);

        public void set(String detailText, Throwable ex) {
            elements.clear();
            if(detailText != null && detailText.length() > 0) {
                elements.add(new DefaultTextElement(detailText, false));
            }
            if(ex != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                pw.flush();
                elements.add(new DefaultTextElement(sw.toString(), true));
            }
            doCallback();
        }

        public void clear() {
            elements.clear();
            doCallback();
        }
        
        public Iterator<Element> iterator() {
            return elements.iterator();
        }
    }

    static class DefaultTextElement implements TextAreaModel.TextElement {
        private final String text;
        private final boolean pre;

        public DefaultTextElement(String text, boolean pre) {
            this.text = text;
            this.pre = pre;
        }
        
        public HAlignment getHorizontalAlignment() {
            return HAlignment.LEFT;
        }
        public TextAreaModel.ValueUnit getMarginLeft() {
            return TextAreaModel.ZERO_PX;
        }
        public TextAreaModel.ValueUnit getMarginRight() {
            return TextAreaModel.ZERO_PX;
        }
        public VAlignment getVerticalAlignment() {
            return VAlignment.BOTTOM;
        }
        public String getFontName() {
            return "default";
        }
        public String getText() {
            return text;
        }
        public TextAreaModel.ValueUnit getTextIndent() {
            return TextAreaModel.ZERO_PX;
        }
        public boolean isParagraphEnd() {
            return true;
        }
        public boolean isParagraphStart() {
            return false;
        }
        public boolean isPreformatted() {
            return pre;
        }
        public Clear getClear() {
            return Clear.NONE;
        }
    }
}