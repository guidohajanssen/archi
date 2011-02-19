/*******************************************************************************
 * Copyright (c) 2010 Bolton University, UK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 *******************************************************************************/
package uk.ac.bolton.archimate.editor.diagram.actions;

import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.ui.IWorkbenchPart;

import uk.ac.bolton.archimate.editor.diagram.commands.FontColorCommand;
import uk.ac.bolton.archimate.editor.diagram.editparts.ITextEditPart;
import uk.ac.bolton.archimate.editor.diagram.editparts.connections.IDiagramConnectionEditPart;
import uk.ac.bolton.archimate.editor.ui.ColorFactory;
import uk.ac.bolton.archimate.model.IFontAttribute;


/**
 * Font Color Action
 * 
 * @author Phillip Beauvoir
 */
public class FontColorAction extends SelectionAction {
    
    public static final String ID = "FontColorAction";
    public static final String TEXT = "Font colour...";
    
    public FontColorAction(IWorkbenchPart part) {
        super(part);
        setText(TEXT);
        setId(ID);
    }

    @Override
    protected boolean calculateEnabled() {
        return getFirstSelectedFontEditPart(getSelectedObjects()) != null;
    }

    private EditPart getFirstSelectedFontEditPart(List<?> selection) {
        for(Object object : getSelectedObjects()) {
            if(isValidEditPart(object)) {
                return (EditPart)object;
            }
        }
        
        return null;
    }
    
    private boolean isValidEditPart(Object object) {
        return object instanceof ITextEditPart || object instanceof IDiagramConnectionEditPart;
    }
    
    @Override
    public void run() {
        List<?> selection = getSelectedObjects();
        
        ColorDialog colorDialog = new ColorDialog(getWorkbenchPart().getSite().getShell());
        
        // Set default RGB on first selected object
        RGB defaultRGB = null;
        EditPart firstPart = getFirstSelectedFontEditPart(selection);
        if(firstPart != null) {
            Object model = firstPart.getModel();
            if(model instanceof IFontAttribute) {
                String s = ((IFontAttribute)model).getFontColor();
                if(s != null) {
                    defaultRGB = ColorFactory.convertStringToRGB(s);
                }
            }
        }
        
        if(defaultRGB != null) {
            colorDialog.setRGB(defaultRGB);
        }
        else {
            colorDialog.setRGB(new RGB(0, 0, 0));
        }
        
        RGB newColor = colorDialog.open();
        if(newColor != null) {
            execute(createCommand(selection, newColor));
        }
    }
    
    private Command createCommand(List<?> selection, RGB newColor) {
        CompoundCommand result = new CompoundCommand("Change font colour");
        
        for(Object object : selection) {
            if(object instanceof EditPart) {
                EditPart editPart = (EditPart)object;
                Object model = editPart.getModel();
                if(model instanceof IFontAttribute) {
                    IFontAttribute fontObject = (IFontAttribute)model;
                    Command cmd = new FontColorCommand(fontObject, ColorFactory.convertRGBToString(newColor));
                    if(cmd.canExecute()) {
                        result.add(cmd);
                    }
                }
            }
        }

        return result.unwrap();
    }
    
}