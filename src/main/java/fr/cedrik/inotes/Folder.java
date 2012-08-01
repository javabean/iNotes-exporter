/**
 *
 */
package fr.cedrik.inotes;

/**
 * @author C&eacute;drik LIME
 */
public class Folder {
	public String levelTree;
	public int    levelNumber;
	public String name;
	public String id;

	public Folder() {
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " (level " + levelNumber + ' ' + levelTree + "): " + name + ' ' + id;
	}

	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof Folder)) {
			return false;
		}
		return ((Folder)obj).id.equals(id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
