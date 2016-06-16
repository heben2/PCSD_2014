package com.acertainsupplychain;

import java.util.List;
import java.util.Set;

/**
 * The ItemSupplier interface abstracts the functionality of underlying
 * suppliers in the supply chain. Each supplier maintains a set of items and the
 * total quantities ordered for these items. For simplicity, it is fine to
 * assume that the set of items managed by an item supplier is fixed.
 */
public interface ItemSupplier {

	/**
	 * Executes an order step with the item supplier, adding the quantity
	 * ordered to the given items.
	 * 
	 * @param step
	 *            - the order step to be executed by this item supplier.
	 * @throws OrderProcessingException
	 *             - if the step is malformed or another exception occurs (you
	 *             may specialize exceptions deriving from
	 *             OrderProcessingException if you want).
	 */
	public void executeStep(OrderStep step) throws OrderProcessingException;

	/**
	 * Returns the total quantity ordered per item at this item supplier.
	 * 
	 * @param itemIds
	 *            - the IDs of the items queried.
	 * @return the position of the items.
	 * @throws InvalidItemException
	 *             - if any of the item IDs is unknown to this item supplier.
	 */
	public List<ItemQuantity> getOrdersPerItem(Set<Integer> itemIds)
			throws InvalidItemException;

}
