/**
 *
 */
package fr.cedrik.inotes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author C&eacute;drik LIME
 */
public class FoldersList extends ArrayList<Folder> {

	/**
	 * {@inheritDoc}
	 */
	public FoldersList() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	public FoldersList(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * {@inheritDoc}
	 */
	public FoldersList(Collection<? extends Folder> c) {
		super(c);
	}

	public Folder getInbox() {
		for (Folder folder : this) {
			if (folder.isInbox()) {
				return folder;
			}
		}
		assert false : "No " + Folder.INBOX + " folder found!";
		return null;
	}

	public Folder getFolderById(String id) {
		for (Folder folder : this) {
			if (folder.id.equals(id)) {
				return folder;
			}
		}
		return null;
	}

	protected Folder getFolderByLevelTree(String levelTree) {
		for (Folder folder : this) {
			if (folder.levelTree.equals(levelTree)) {
				return folder;
			}
		}
		return null;
	}

	protected Folder getParent(Folder folder) {
		int idx = folder.levelTree.lastIndexOf('.');
		if (idx == -1) {
			return null;
		}
		String requestedFolderTree = folder.levelTree.substring(0, idx);
		Folder parent = getFolderByLevelTree(requestedFolderTree);
		return parent;
	}

	public List<Folder> getFoldersChain(Folder folder) {
		List<Folder> result = new ArrayList<Folder>();
		result.add(folder);
		Folder parent;
		while ((parent = getParent(folder)) != null) {
			result.add(parent);
		}
		Collections.reverse(result);
		assert result != null && ! result.isEmpty() : folder;
		return result;
	}
}
