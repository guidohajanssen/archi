/*******************************************************************************
 * Copyright (c) 2010 Bolton University, UK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 *******************************************************************************/
package uk.ac.bolton.archimate.editor.diagram.actions;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.gef.Disposable;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPart;

import uk.ac.bolton.archimate.editor.diagram.commands.ConnectionRouterTypeCommand;
import uk.ac.bolton.archimate.model.IArchimatePackage;
import uk.ac.bolton.archimate.model.IDiagramModel;


/**
 * Connection Router Action
 * 
 * @author Phillip Beauvoir
 */
public abstract class ConnectionRouterAction extends Action implements Disposable {
    
    public static String CONNECTION_ROUTER_BENDPONT = "Manual";
    public static String CONNECTION_ROUTER_SHORTEST_PATH = "Shortest Path";
    public static String CONNECTION_ROUTER_MANHATTAN = "Manhattan";
    
    private IWorkbenchPart part;
    private IDiagramModel diagramModel;
    
    /*
     * Adapter to listen to change made from Properties View
     */
    private Adapter eAdapter = new AdapterImpl() {
        @Override
        public void notifyChanged(Notification msg) {
            Object feature = msg.getFeature();
            if(feature == IArchimatePackage.Literals.DIAGRAM_MODEL__CONNECTION_ROUTER_TYPE) {
                update();
            }
        }
    };
    
    public ConnectionRouterAction(IWorkbenchPart part) {
        super(null, AS_RADIO_BUTTON);
        this.part = part;
        diagramModel = (IDiagramModel)part.getAdapter(IDiagramModel.class);
        diagramModel.eAdapters().add(eAdapter);
        update();
    }

    @Override
    public void run() {
        if(isChecked()) {
            CommandStack stack = (CommandStack)part.getAdapter(CommandStack.class);
            stack.execute(new ConnectionRouterTypeCommand(diagramModel, getType()));
        }
    }
    
    protected void update() {
        setChecked(diagramModel.getConnectionRouterType() == getType());
    }
    
    protected abstract int getType();
    
    public void dispose() {
        diagramModel.eAdapters().remove(eAdapter);
    }
    
    
    /*
     * Bend Point
     */
    public static class BendPointConnectionRouterAction extends ConnectionRouterAction  {
        public static String ID = "BendPointConnectionAction";
        
        public BendPointConnectionRouterAction(IWorkbenchPart part) {
            super(part);
            setId(ID);
            setText(CONNECTION_ROUTER_BENDPONT);
        }

        @Override
        public int getType() {
            return IDiagramModel.CONNECTION_ROUTER_BENDPOINT;
        }
    };
    
    /*
     * Shortest Path
     */
    public static class ShortestPathConnectionRouterAction extends ConnectionRouterAction  {
        public static String ID = "ShortestPathConnectionRouterAction";
        
        public ShortestPathConnectionRouterAction(IWorkbenchPart part) {
            super(part);
            setId(ID);
            setText(CONNECTION_ROUTER_SHORTEST_PATH);
        }

        @Override
        public int getType() {
            return IDiagramModel.CONNECTION_ROUTER_SHORTEST_PATH;
        }
    };
    
    /*
     * Manhattan
     */
    public static class ManhattanConnectionRouterAction extends ConnectionRouterAction  {
        public static String ID = "ManhattanConnectionAction";
        
        public ManhattanConnectionRouterAction(IWorkbenchPart part) {
            super(part);
            setId(ID);
            setText(CONNECTION_ROUTER_MANHATTAN);
        }

        @Override
        public int getType() {
            return IDiagramModel.CONNECTION_ROUTER_MANHATTAN;
        }
    };
}