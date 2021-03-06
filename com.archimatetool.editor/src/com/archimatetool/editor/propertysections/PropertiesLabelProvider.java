/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package com.archimatetool.editor.propertysections;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Image;

import com.archimatetool.editor.ui.ArchimateLabelProvider;
import com.archimatetool.editor.utils.StringUtils;
import com.archimatetool.model.IArchimateComponent;



/**
 * Properties Label Provider
 * 
 * @author Phillip Beauvoir
 */
public class PropertiesLabelProvider implements ILabelProvider {

    public Image getImage(Object object) {
        if(!(object instanceof IStructuredSelection)) {
            return null;
        }
        
        object = ((IStructuredSelection)object).getFirstElement();
        
        if(object instanceof IAdaptable) {
            object = ((IAdaptable)object).getAdapter(object.getClass());
        }
        
        return ArchimateLabelProvider.INSTANCE.getImage(object);
    }

    public String getText(Object object) {
        if(!(object instanceof IStructuredSelection)) {
            return " "; //$NON-NLS-1$
        }
        
        object = ((IStructuredSelection)object).getFirstElement();
        
        if(object instanceof IAdaptable) {
            object = ((IAdaptable)object).getAdapter(object.getClass());
        }

        object = ArchimateLabelProvider.INSTANCE.getWrappedElement(object);
        
        // An Archimate Component is a special text
        if(object instanceof IArchimateComponent) {
            return getArchimateComponentText((IArchimateComponent)object);
        }

        // Check the main label provider
        String text = ArchimateLabelProvider.INSTANCE.getLabel(object);
        if(StringUtils.isSet(text)) {
            return StringUtils.escapeAmpersandsInText(text);
        }
        
        return " "; // Ensure the title bar is displayed //$NON-NLS-1$
    }
    
    String getArchimateComponentText(IArchimateComponent archimateComponent) {
        String name = StringUtils.escapeAmpersandsInText(archimateComponent.getName());
        
        String typeName = ArchimateLabelProvider.INSTANCE.getDefaultName(archimateComponent.eClass());
        
        if(StringUtils.isSet(name)) {
            return name + " (" + typeName + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        
        return typeName;
    }
    
    public void addListener(ILabelProviderListener listener) {
    }

    public void dispose() {
    }

    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    public void removeListener(ILabelProviderListener listener) {
    }

}
