/**
 *  Copyright (C) 2002-2013   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.panel;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.util.List;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Map;


/**
 * A dialog to allow resizing of the map.
 */
public class ScaleMapSizeDialog extends FreeColDialog<Dimension> {

    private static final int COLUMNS = 5;

    final Map oldMap;

    final JTextField inputWidth;

    final JTextField inputHeight;


    /**
     * Create a ScaleMapSizeDialog.
     *
     * @param freeColDialog The <code>FreeColDialog</code> for the game.
     */
    public ScaleMapSizeDialog(final FreeColClient freeColClient) {
        super(freeColClient);

        /*
         * TODO: Extend this dialog. It should be possible to specify the sizes
         * using percentages.
         *
         * Add a panel containing information about the scaling (old size, new
         * size etc).
         */

        MigPanel panel = new MigPanel(new MigLayout("wrap 1, center"));
        JPanel widthPanel = new JPanel(new FlowLayout());
        JPanel heightPanel = new JPanel(new FlowLayout());
        String str;

        oldMap = freeColClient.getGame().getMap();
        str = Integer.toString(oldMap.getWidth());
        inputWidth = new JTextField(str, COLUMNS);
        str = Integer.toString(oldMap.getHeight());
        inputHeight = new JTextField(str, COLUMNS);

        JLabel widthLabel = new JLabel(Messages.message("width"));
        widthLabel.setLabelFor(inputWidth);
        JLabel heightLabel = new JLabel(Messages.message("height"));
        heightLabel.setLabelFor(inputHeight);

        widthPanel.setOpaque(false);
        widthPanel.add(widthLabel);
        widthPanel.add(inputWidth);
        heightPanel.setOpaque(false);
        heightPanel.add(heightLabel);
        heightPanel.add(inputHeight);

        panel.add(widthPanel);
        panel.add(heightPanel);
        panel.setSize(panel.getPreferredSize());

        final ActionListener al = new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    ScaleMapSizeDialog.this.checkFields();
                }
            };

        inputWidth.addActionListener(al);
        inputHeight.addActionListener(al);

        final Dimension fake = null;
        List<ChoiceItem<Dimension>> c = choices();
        c.add(new ChoiceItem<Dimension>(Messages.message("ok"),
                fake).okOption());
        c.add(new ChoiceItem<Dimension>(Messages.message("cancel"),
                fake).cancelOption().defaultOption());
        initialize(DialogType.QUESTION, true, panel, null, c);
    }

    /**
     * Force the text fields to contain non-negative integers.
     */
    private void checkFields() {
        try {
            int w = Integer.parseInt(inputWidth.getText());
            if (w <= 0) throw new NumberFormatException();
        } catch (NumberFormatException nfe) {
            inputWidth.setText(Integer.toString(oldMap.getWidth()));
        } 
        try {
            int h = Integer.parseInt(inputHeight.getText());
            if (h <= 0) throw new NumberFormatException();
        } catch (NumberFormatException nfe) {
            inputHeight.setText(Integer.toString(oldMap.getHeight()));
        } 
    }

    /**
     * {@inheritDoc}
     */
    public Dimension getResponse() {
        Object value = getValue();
        if (options.get(0).equals(value)) {
            checkFields();
            return new Dimension(Integer.parseInt(inputHeight.getText()),
                                 Integer.parseInt(inputWidth.getText()));
        }
        return null;
    }


    // Override Component

    @Override
    public void requestFocus() {
        this.inputWidth.requestFocus();
    }
}
