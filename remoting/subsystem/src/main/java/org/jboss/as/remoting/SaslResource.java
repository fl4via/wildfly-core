/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.as.remoting;

import static org.jboss.as.remoting.CommonAttributes.INCLUDE_MECHANISMS;
import static org.jboss.as.remoting.CommonAttributes.QOP;
import static org.jboss.as.remoting.CommonAttributes.REUSE_SESSION;
import static org.jboss.as.remoting.CommonAttributes.SASL;
import static org.jboss.as.remoting.CommonAttributes.SECURITY;
import static org.jboss.as.remoting.CommonAttributes.SERVER_AUTH;
import static org.jboss.as.remoting.CommonAttributes.STRENGTH;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.xnio.sasl.SaslQop;
import org.xnio.sasl.SaslStrength;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SaslResource extends SimpleResourceDefinition {
    static final PathElement SASL_CONFIG_PATH = PathElement.pathElement(SECURITY, SASL);

    static final AttributeDefinition INCLUDE_MECHANISMS_ATTRIBUTE = new StringListAttributeDefinition.Builder(INCLUDE_MECHANISMS)
            .setAllowNull(true)
            .setAttributeMarshaller(new SaslAttributeMarshaller(Element.INCLUDE_MECHANISMS))
            .build();

    static final AttributeDefinition QOP_ATTRIBUTE = new StringListAttributeDefinition.Builder(QOP)
            .setAllowNull(true)
            .setAttributeMarshaller(new SaslAttributeMarshaller(Element.QOP))
            .setElementValidator(QopParameterValidation.INSTANCE)
            .build();
    static final AttributeDefinition STRENGTH_ATTRIBUTE = new StringListAttributeDefinition.Builder(STRENGTH)
            .setAllowNull(true)
            .setAttributeMarshaller(new SaslAttributeMarshaller(Element.STRENGTH))
            .setElementValidator(StrengthParameterValidation.INSTANCE)
            .build();
    static final SimpleAttributeDefinition SERVER_AUTH_ATTRIBUTE = SimpleAttributeDefinitionBuilder.create(SERVER_AUTH, ModelType.BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setAttributeMarshaller(new WrappedAttributeMarshaller(Attribute.VALUE))
            .build();
    static final SimpleAttributeDefinition REUSE_SESSION_ATTRIBUTE = SimpleAttributeDefinitionBuilder.create(REUSE_SESSION, ModelType.BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setAttributeMarshaller(new WrappedAttributeMarshaller(Attribute.VALUE))
            .build();

    static final AttributeDefinition[] ATTRIBUTES = {INCLUDE_MECHANISMS_ATTRIBUTE, QOP_ATTRIBUTE, STRENGTH_ATTRIBUTE, SERVER_AUTH_ATTRIBUTE, REUSE_SESSION_ATTRIBUTE};

    static final SaslResource INSTANCE = new SaslResource();

    private final List<AccessConstraintDefinition> accessConstraints;

    private SaslResource() {
        super(SASL_CONFIG_PATH,
                RemotingExtension.getResourceDescriptionResolver(SASL),
                SaslAdd.INSTANCE,
                SaslRemove.INSTANCE);
        this.accessConstraints = RemotingExtension.REMOTING_SECURITY_DEF.wrapAsList();
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        final ReloadRequiredWriteAttributeHandler writeHandler =
                new ReloadRequiredWriteAttributeHandler(INCLUDE_MECHANISMS_ATTRIBUTE, QOP_ATTRIBUTE, STRENGTH_ATTRIBUTE,
                        REUSE_SESSION_ATTRIBUTE, SERVER_AUTH_ATTRIBUTE);
        resourceRegistration.registerReadWriteAttribute(INCLUDE_MECHANISMS_ATTRIBUTE, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(QOP_ATTRIBUTE, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(STRENGTH_ATTRIBUTE, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(REUSE_SESSION_ATTRIBUTE, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(SERVER_AUTH_ATTRIBUTE, null, writeHandler);
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }

    private static class SaslAttributeMarshaller extends AttributeMarshaller {
        private final Element element;

        SaslAttributeMarshaller(Element element) {
            this.element = element;
        }

        @Override
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            if (resourceModel.hasDefined(attribute.getName())) {
                List<ModelNode> list = resourceModel.get(attribute.getName()).asList();
                if (list.size() > 0) {
                    writer.writeEmptyElement(element.getLocalName());
                    StringBuilder sb = new StringBuilder();
                    for (ModelNode child : list) {
                        if (sb.length() > 0) {
                            sb.append(" ");
                        }
                        sb.append(child.asString());
                    }
                    writer.writeAttribute(Attribute.VALUE.getLocalName(), sb.toString());
                }
            }
        }

    }

    private abstract static class SaslEnumValidator extends StringLengthValidator implements AllowedValuesValidator {
        final List<ModelNode> allowedValues = new ArrayList<ModelNode>();

        SaslEnumValidator(Enum<?>[] src, boolean toLowerCase) {
            super(1);
            for (Enum<?> e : src) {
                allowedValues.add(new ModelNode().set(toLowerCase ? e.name().toLowerCase(Locale.ENGLISH) : e.name()));
            }
        }

        @Override
        public List<ModelNode> getAllowedValues() {
            return allowedValues;
        }

    }

    private static class QopParameterValidation extends SaslEnumValidator implements AllowedValuesValidator {
        static final QopParameterValidation INSTANCE = new QopParameterValidation();

        public QopParameterValidation() {
            super(SaslQop.values(), false);
        }
    }

    private static class StrengthParameterValidation extends SaslEnumValidator implements AllowedValuesValidator {
        static final StrengthParameterValidation INSTANCE = new StrengthParameterValidation();

        public StrengthParameterValidation() {
            super(SaslStrength.values(), true);
        }
    }
}
