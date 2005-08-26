package org.alfresco.service.transaction;

import javax.transaction.UserTransaction;

/**
 * Contract for retrieving access to a transaction
 * 
 * @author David Caruana
 */
public interface TransactionService
{
    /**
     * Gets a user transaction that supports transaction gation.
     * This is like the EJB <b>REQUIRED</b> transaction attribute.
     * 
     * @return the user transaction
     */
    UserTransaction getUserTransaction();
    
    /**
     * Gets a user transaction that ensures a new transaction is created.
     * Any enclosing transaction is not propagated.
     * This is like the EJB <b>REQUIRES_NEW</b> transaction attribute -
     * when the transaction is started, the current transaction will be
     * suspended and a new one started.
     * 
     * @return Returns a non-gating user transaction
     */
    UserTransaction getNonPropagatingUserTransaction();
}
