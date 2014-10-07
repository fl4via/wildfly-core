/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
/**
 * Add a connector to a remoting container.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 * @author Kabir Khan
 */
public class SaslAdd extends AbstractAddStepHandler {

    static final SaslAdd INSTANCE = new SaslAdd();

    private SaslAdd() {
        super(SaslResource.INCLUDE_MECHANISMS_ATTRIBUTE,
                SaslResource.QOP_ATTRIBUTE,
                SaslResource.STRENGTH_ATTRIBUTE,
                SaslResource.REUSE_SESSION_ATTRIBUTE,
                SaslResource.SERVER_AUTH_ATTRIBUTE);
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return false;
    }
}
