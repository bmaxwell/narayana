/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 *
 * (C) 2005-2006,
 * @author JBoss Inc.
 */
/*
 * Copyright (C) 2001, 2002,
 *
 * Hewlett-Packard Arjuna Labs,
 * Newcastle upon Tyne,
 * Tyne and Wear,
 * UK.
 *
 * $Id: SynchronizationImple.java 2342 2006-03-30 13:06:17Z  $
 */

package com.arjuna.ats.internal.jta.resources.jts.orbspecific;

import org.omg.CORBA.UNKNOWN;

import com.arjuna.ats.internal.jta.utils.jtaxLogger;
import com.arjuna.ats.internal.jta.utils.jts.StatusConverter;
import com.arjuna.ats.internal.jts.ORBManager;

/**
 * Whenever a synchronization is registered, an instance of this class
 * is used to wrap it.
 *
 * @author Mark Little (mark_little@hp.com)
 * @version $Id: SynchronizationImple.java 2342 2006-03-30 13:06:17Z  $
 * @since JTS 1.2.4.
 */

public class SynchronizationImple implements org.omg.CosTransactions.SynchronizationOperations
{

    public SynchronizationImple (javax.transaction.Synchronization ptr)
    {
	_theSynch = ptr;
	_theReference = null;
	_theClassLoader = Thread.currentThread().getContextClassLoader();
    }

    public final org.omg.CosTransactions.Synchronization getSynchronization ()
    {
        if (_theReference == null)
        {
            _thePOATie = getPOATie();

            ORBManager.getPOA().objectIsReady(_thePOATie);

            _theReference = org.omg.CosTransactions.SynchronizationHelper.narrow(ORBManager.getPOA().corbaReference(_thePOATie));
        }

        return _theReference;
    }

    public void before_completion () throws org.omg.CORBA.SystemException
    {
	if (jtaxLogger.logger.isTraceEnabled()) {
        jtaxLogger.logger.trace("SynchronizationImple.before_completion - Class: " + _theSynch.getClass() + " HashCode: " + _theSynch.hashCode() + " toString: " + _theSynch);
    }

	if (_theSynch != null)
	{
	    ClassLoader origClassLoader = Thread.currentThread().getContextClassLoader();

	    try
	    {
		Thread.currentThread().setContextClassLoader(_theClassLoader);
		_theSynch.beforeCompletion();
	    }
	    catch (Exception e)
	    {
		throw new UNKNOWN();
	    }
	    finally
	    {
		Thread.currentThread().setContextClassLoader(origClassLoader);
	    }
	}
	else
	    throw new UNKNOWN();
    }

    public void after_completion (org.omg.CosTransactions.Status status) throws org.omg.CORBA.SystemException
    {
	if (jtaxLogger.logger.isTraceEnabled()) {
        jtaxLogger.logger.trace("SynchronizationImple.after_completion - Class: " + _theSynch.getClass() + " HashCode: " + _theSynch.hashCode() + " toString: " + _theSynch);
    }

	if (_theSynch != null)
	{
	    int s = StatusConverter.convert(status);
	    ClassLoader origClassLoader = Thread.currentThread().getContextClassLoader();

	    try
	    {
		Thread.currentThread().setContextClassLoader(_theClassLoader);
		_theSynch.afterCompletion(s);

		if (_theReference != null)
		    ORBManager.getPOA().shutdownObject(_thePOATie);
	    }
	    catch (Exception e)
	    {
		e.printStackTrace();

		if (_theReference != null)
		    ORBManager.getPOA().shutdownObject(_thePOATie);

		throw new UNKNOWN(); // should not cause any affect!
	    }
	    finally
	    {
		Thread.currentThread().setContextClassLoader(origClassLoader);
	    }
	}
	else
	    throw new UNKNOWN(); // should not cause any affect!
    }

    // this is used to allow subclasses to override the Tie type provided.
    // the Tie classes do not inherit from one another even if the business interfaces
    // they correspond to do in the idl. Hence Servant is the only common parent.
    protected org.omg.PortableServer.Servant getPOATie() {
        return new org.omg.CosTransactions.SynchronizationPOATie(this);
    }

    private javax.transaction.Synchronization       _theSynch;
    private org.omg.CosTransactions.Synchronization _theReference;
    private org.omg.PortableServer.Servant _thePOATie;
    private ClassLoader _theClassLoader;
}
