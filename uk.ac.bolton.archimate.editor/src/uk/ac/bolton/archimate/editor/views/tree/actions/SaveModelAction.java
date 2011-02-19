/*******************************************************************************
 * Copyright (c) 2010 Bolton University, UK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 *******************************************************************************/
package uk.ac.bolton.archimate.editor.views.tree.actions;

import java.io.IOException;

import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchCommandConstants;

import uk.ac.bolton.archimate.editor.model.IEditorModelManager;
import uk.ac.bolton.archimate.editor.views.tree.ITreeModelView;
import uk.ac.bolton.archimate.model.IArchimateModel;


/**
 * Save Model Action
 * 
 * @author Phillip Beauvoir
 */
public class SaveModelAction extends ViewerAction {
    
    private ITreeModelView fView;
    
    public SaveModelAction(ITreeModelView view) {
        super(view.getSelectionProvider());
        setText("Save");
        
        fView = view;
        
        // Ensures key binding is displayed
        setActionDefinitionId(IWorkbenchCommandConstants.FILE_SAVE);
    }
    
    @Override
    public void run() {
        // Get selected Model and save it and any Diagrams via EditorModel Manager
        IArchimateModel model = (IArchimateModel)fView.getAdapter(IArchimateModel.class);
        if(model != null) {
            try {
                IEditorModelManager.INSTANCE.saveModel(model);
            }
            catch(IOException ex) {
                MessageDialog.openError(fView.getSite().getShell(), "Error saving file", ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void update(IStructuredSelection selection) {
        CommandStack stack = (CommandStack)fView.getAdapter(CommandStack.class);
        setEnabled(stack.isDirty());
    }

}