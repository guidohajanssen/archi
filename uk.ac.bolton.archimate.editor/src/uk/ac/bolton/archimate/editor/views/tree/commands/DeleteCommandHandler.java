/*******************************************************************************
 * Copyright (c) 2010 Bolton University, UK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 *******************************************************************************/
package uk.ac.bolton.archimate.editor.views.tree.commands;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import uk.ac.bolton.archimate.editor.diagram.commands.DiagramCommandFactory;
import uk.ac.bolton.archimate.editor.model.DiagramModelUtils;
import uk.ac.bolton.archimate.editor.model.commands.DeleteDiagramModelCommand;
import uk.ac.bolton.archimate.editor.model.commands.DeleteElementCommand;
import uk.ac.bolton.archimate.editor.model.commands.DeleteFolderCommand;
import uk.ac.bolton.archimate.model.FolderType;
import uk.ac.bolton.archimate.model.IArchimateElement;
import uk.ac.bolton.archimate.model.IArchimateModel;
import uk.ac.bolton.archimate.model.IArchimateModelElement;
import uk.ac.bolton.archimate.model.IDiagramModel;
import uk.ac.bolton.archimate.model.IDiagramModelComponent;
import uk.ac.bolton.archimate.model.IDiagramModelConnection;
import uk.ac.bolton.archimate.model.IDiagramModelObject;
import uk.ac.bolton.archimate.model.IDiagramModelReference;
import uk.ac.bolton.archimate.model.IFolder;
import uk.ac.bolton.archimate.model.IFolderContainer;
import uk.ac.bolton.archimate.model.IRelationship;
import uk.ac.bolton.archimate.model.util.ArchimateModelUtils;


/**
 * Handles Delete Commands for the Tree Model View
 * 
 * @author Phillip Beauvoir
 */
public class DeleteCommandHandler {
    
    /*
     * If deleting elements from more than one model in the tree we need to use the
     * Command Stack allocated to each model. And then allocate one CompoundCommand per Command Stack.
     */
    private List<CommandStack> fCommandStacks = new ArrayList<CommandStack>();
    private Hashtable<CommandStack, CompoundCommand> fCommandMap = new Hashtable<CommandStack, CompoundCommand>();
    
    private ISelectionProvider fSelectionProvider;
    
    // Selected objects in Tree
    private Object[] fSelectedObjects;
    
    // Top level objects to delete
    private List<Object> fElementsToDelete;
    
    // Elements to check including children of top elements to delete
    private List<Object> fElementsToCheck;
    
    
    /**
     * @param element
     * @return True if we can delete this object
     */
    public static boolean canDelete(Object element) {
        // Elements and Diagrams
        if(element instanceof IArchimateElement || element instanceof IDiagramModel) {
            return true;
        }
        
        // Certain Folders
        if(element instanceof IFolder) {
            IFolder folder = (IFolder)element;
            if(folder.getType().equals(FolderType.DERIVED) || folder.getType().equals(FolderType.USER)) {
                return true;
            }
        }
        
        return false;
    }

    public DeleteCommandHandler(ISelectionProvider selectionProvider, Object[] objects) {
        fSelectionProvider = selectionProvider;
        fSelectedObjects = objects;
    }
    
    /**
     * @return True if any of the objects to be deleted are referenced in a diagram
     */
    public boolean hasDiagramReferences() {
        for(Object object : fSelectedObjects) {
            boolean result = hasDiagramReferences(object);
            if(result) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * @return True if object has references in a diagram model
     */
    private boolean hasDiagramReferences(Object object) {
        if(object instanceof IFolder) {
            for(EObject element : ((IFolder)object).getElements()) {
                boolean result = hasDiagramReferences(element);
                if(result) {
                    return true;
                }
            }
            for(IFolder f : ((IFolder)object).getFolders()) {
                boolean result = hasDiagramReferences(f);
                if(result) {
                    return true;
                }
            }
        }
        
        else if(object instanceof IArchimateElement) {
            return DiagramModelUtils.isElementReferencedInDiagrams((IArchimateElement)object);
        }
        
        return false;
    }
    
    /**
     * Delete the objects.
     * Once this occurs this DeleteCommandHandler is disposed.
     */
    public void delete() {
        // Gather the elements to delete
        getElementsToDelete();
        
        // Create the Commands
        createCommands();
        
        // Execute the Commands on the CommandStack(s) - there could be more than one if more than one model open in the Tree
        for(CommandStack stack : fCommandStacks) {
            CompoundCommand compoundCommand = fCommandMap.get(stack);
            stack.execute(compoundCommand);
        }
        
        dispose();
    }
    
    /**
     * Create the Delete Commands
     */
    private void createCommands() {
        // We need to ensure that the Delete Diagram Model Commands are called first in order to close
        // any open diagram editors gracefully before removing their models from parent folders.
        for(Object object : fElementsToDelete) {
            CompoundCommand compoundCommand = getCompoundCommand(object);
            if(compoundCommand == null) { // sanity check
                continue;
            }
            
            if(object instanceof IDiagramModel) {
                Command cmd = new DeleteDiagramModelCommand((IDiagramModel)object);
                compoundCommand.add(cmd);
            }
        }
        
        for(Object object : fElementsToDelete) {
            CompoundCommand compoundCommand = getCompoundCommand(object);
            if(compoundCommand == null) { // sanity check
                continue;
            }

            if(object instanceof IFolder) {
                Command cmd = new DeleteFolderCommand((IFolder)object);
                compoundCommand.add(cmd);
            }
            else if(object instanceof IArchimateElement) {
                Command cmd = new DeleteElementCommand((IArchimateElement)object);
                compoundCommand.add(cmd);
            }
            else if(object instanceof IDiagramModelObject) {
                Command cmd = DiagramCommandFactory.createDeleteDiagramObjectCommand((IDiagramModelObject)object);
                compoundCommand.add(cmd);
            }
            else if(object instanceof IDiagramModelConnection) {
                Command cmd = DiagramCommandFactory.createDeleteDiagramConnectionCommand((IDiagramModelConnection)object);
                compoundCommand.add(cmd);
            }
        }
    }
    
    /**
     * Create the list of objects to delete and check
     * @return
     */
    private void getElementsToDelete() {
        // Actual elements to delete
        fElementsToDelete = new ArrayList<Object>();
        
        // Elements to check against for diagram references and other uses
        fElementsToCheck = new ArrayList<Object>();
        
        // First, gather up the list of Archimate objects to be deleted...
        for(Object object : fSelectedObjects) {
            if(canDelete(object)) {
                addToList(object, fElementsToDelete);
                addFolderChildElements(object);
                addElementRelationships(object);
            }
        }
        
        // Gather referenced diagram objects to be deleted checking that the parent diagram model is not also selected to be deleted
        for(Object object : fElementsToCheck) {
            // Archimate Elements
            if(object instanceof IArchimateElement) {
                IArchimateElement element = (IArchimateElement)object;
                for(IDiagramModel diagramModel : element.getArchimateModel().getDiagramModels()) {
                    // Check diagram model is not selected to be deleted - no point in deleting any of its children
                    if(!fElementsToDelete.contains(diagramModel)) { 
                        IDiagramModelComponent diagramModelComponent = DiagramModelUtils.findDiagramModelComponentForElement(diagramModel, element); // is there one?
                        if(diagramModelComponent != null) {
                            addToList(diagramModelComponent, fElementsToDelete);
                        }
                    }
                }
            }
            
            // Diagram Models and their references
            if(object instanceof IDiagramModel) {
                IDiagramModel diagramModelDeleted = (IDiagramModel)object;
                for(IDiagramModel diagramModel : diagramModelDeleted.getArchimateModel().getDiagramModels()) {
                    List<IDiagramModelReference> list = DiagramModelUtils.findDiagramModelReferences(diagramModel, diagramModelDeleted); // is there one?
                    fElementsToDelete.addAll(list);
                }
            }
        }
    }
    
    /**
     * Gather elements in folders that need checking for referenced diagram objects and other checks
     */
    private void addFolderChildElements(Object object) {
        // Folder
        if(object instanceof IFolder) {
            for(EObject element : ((IFolder)object).getElements()) {
                addFolderChildElements(element);
            }

            // Child folders
            for(IFolder f : ((IFolderContainer)object).getFolders()) {
                addFolderChildElements(f);
            }
        }
        else {
            // Add to check list
            addToList(object, fElementsToCheck);
            // Diagram models need to be deleted explicitly with their own command in case they need closing in the editor
            if(object instanceof IDiagramModel) {
                addToList(object, fElementsToDelete);
            }
        }
    }
    
    /**
     * Add any connected relationships for an Element
     */
    private void addElementRelationships(Object object) {
        // Folder
        if(object instanceof IFolder) {
            for(EObject element : ((IFolder)object).getElements()) {
                addElementRelationships(element);
            }

            // Child folders
            for(IFolder f : ((IFolderContainer)object).getFolders()) {
                addElementRelationships(f);
            }
        }
        // Element
        else if(object instanceof IArchimateElement && !(object instanceof IRelationship)) {
            for(IRelationship relationship : ArchimateModelUtils.getRelationships((IArchimateElement)object)) {
                addToList(relationship, fElementsToDelete);
                addToList(relationship, fElementsToCheck);
            }
        }
    }
    
    /**
     * Add object to list if not already in list
     */
    private void addToList(Object object, List<Object> list) {
        if(object != null && !list.contains(object)) {
            list.add(object);
        }
    }
    
    /**
     * Get, and if need be create, a CompoundCommand to which to add the object to be deleted command
     * @param object
     * @return
     */
    private CompoundCommand getCompoundCommand(Object object) {
        IArchimateModel model = null;
        
        // Get the model it belongs to...
        if(object instanceof IArchimateModelElement) {
            model = ((IArchimateModelElement)object).getArchimateModel();
        }
        else if(object instanceof IDiagramModelComponent) {
            model = ((IDiagramModelComponent)object).getDiagramModel().getArchimateModel();
        }
        else {
            System.err.println("model was null in " + getClass());
            return null;
        }
        
        // ...so that we can get the Command Stack registered to it
        CommandStack stack = (CommandStack)model.getAdapter(CommandStack.class);
        if(stack == null) {
            System.err.println("CommandStack was null in " + getClass());
            return null;
        }
        
        if(!fCommandStacks.contains(stack)) {
            fCommandStacks.add(stack);
        }
        
        CompoundCommand compoundCommand = fCommandMap.get(stack);
        if(compoundCommand == null) {
            List<?> selected = ((IStructuredSelection)fSelectionProvider.getSelection()).toList();
            compoundCommand = new DeleteElementsCompoundCommand(selected);
            fCommandMap.put(stack, compoundCommand);
        }
        
        return compoundCommand;
    }
    
    private void dispose() {
        fSelectedObjects = null;
        fElementsToDelete = null;
        fSelectionProvider = null;
        fCommandStacks = null;
        fCommandMap = null;
    }

}