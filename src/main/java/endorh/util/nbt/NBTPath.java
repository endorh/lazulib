package endorh.util.nbt;

import net.minecraft.nbt.CollectionNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Util;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static endorh.util.text.TextUtil.stc;
import static java.lang.Math.min;

/**
 * NBT path object.<br>
 * May contain any number of name and list index nodes mixed in any order.<br>
 * Name nodes are separated by dots. Many list index nodes follow a name node between
 * square brackets.<br>
 * <br>
 * <i>Examples</i>:
 * <ul>
 *    <li>{@code Only.Name.Nodes}</li>
 *    <li>{@code Name.And.List[0].Nodes[1][2]}</li>
 *    <li>{@code Name.Nodes.Can."Contain spaces and other unusual characters if".Quoted}</li>
 * </ul>
 * <i>List index nodes have certain limitations</i>, such as not being able to create paths
 * in conditions where it would be impossible<br>
 */
public class NBTPath implements Comparable<NBTPath>, Iterable<NBTPath.Node> {
	protected final List<Node> list;
	
	/**
	 * Default style used to display paths
	 */
	public static Style defaultStyle = new Style();
	
	/**
	 * Style class containing formats for each styleable token<br>
	 * Used by {@link NBTPath#getDisplay(Style)}
	 */
	public static class Style {
		public TextFormatting nameStyle = TextFormatting.DARK_PURPLE;
		public TextFormatting quoteStyle = TextFormatting.GOLD;
		public TextFormatting dotStyle = TextFormatting.GOLD;
		public TextFormatting indexStyle = TextFormatting.DARK_AQUA;
		public TextFormatting bracketStyle = TextFormatting.GOLD;
	}
	
	/**
	 * Deep traversal over an NBT compound in depth-first order.<br>
	 * Skips compounds, that is, it traverses individual values only.<br>
	 * Intended use:
	 * <pre>{@code
	 *    CompoundNBT nbt = new CompoundNBT();
	 *    for (Map.Entry<NBTPath, INBT> entry : traverse(nbt)) {
	 *        ...
	 *    }
	 * }</pre>
	 */
	public static Iterable<Map.Entry<NBTPath, INBT>> traverse(CompoundNBT nbt) {
		return () -> new CompoundNBTIterator(nbt, new NBTPath(new ArrayList<>()));
	}
	
	protected static class CompoundNBTIterator implements Iterator<Entry<NBTPath, INBT>> {
		protected final CompoundNBT root;
		protected final NBTPath path;
		protected final Iterator<String> key;
		protected CompoundNBTIterator nest = null;
		protected Entry<NBTPath, INBT> last = null;
		
		protected CompoundNBTIterator(CompoundNBT root, NBTPath path) {
			this.root = root;
			this.key = root.keySet().iterator();
			this.path = path;
		}
		
		@Override public boolean hasNext() {
			return last != null || next(false) != null;
		}
		
		@Override public Entry<NBTPath, INBT> next() {
			return next(true);
		}
		
		public Entry<NBTPath, INBT> next(boolean use) {
			if (last != null) {
				final Entry<NBTPath, INBT> r = last;
				last = null;
				return r;
			}
			while (key.hasNext() || nest != null) {
				if (nest == null) {
					String k = key.next();
					final INBT elem = root.get(k);
					if (elem instanceof CompoundNBT) {
						nest = new CompoundNBTIterator((CompoundNBT) elem, path.resolve(k));
					} else {
						final Entry<NBTPath, INBT> r =
						  org.apache.commons.lang3.tuple.Pair.of(path.resolve(k), elem);
						if (!use) last = r;
						return r;
					}
				}
				Entry<NBTPath, INBT> nestNext = nest.next(use);
				if (nestNext != null) {
					return nestNext;
				} else nest = null;
			}
			return null;
		}
	}
	
	protected static final Pattern nodePattern = Pattern.compile(
	  "(?<name>\\w++|\"(?:[^\"\\\\]++|\\\\.)*+\")\\s*+(?<arr>(?:\\[\\s*+[+-]?\\d++\\s*+]\\s*+)*+)");
	protected static final Pattern lenientNodePattern = Pattern.compile(
	  "(?<name>\"(?:[^\"\\\\]++|\\\\.)*+\"|[^\".][^.]*+)\\s*+" +
	  "(?<arr>(?:\\[\\s*+[+-]?\\d++\\s*+]\\s*+)*+)");
	protected static final Pattern pattern = Pattern.compile(
	  "(?:" + nodePattern.pattern() + "(?:\\.|$))*+(?<!\\.)");
	protected static final Pattern lenientPattern = Pattern.compile(
	  "(?:" + lenientNodePattern.pattern() + "(?:\\.|$))*+(?<!\\.)");
	
	public NBTPath(String path) {
		path = path.trim();
		if (!lenientPattern.matcher(path).matches())
			throw new IllegalArgumentException("Invalid NBT path: \"" + path + "\"");
		final Matcher m = lenientNodePattern.matcher(path);
		List<Node> list = new ArrayList<>();
		while (m.find()) {
			String name = m.group("name");
			if (name.startsWith("\""))
				name = name.substring(1, name.length() - 1);
			list.add(new TagNode(name));
			String arr = m.group("arr").trim();
			if (!arr.isEmpty()) {
				final String[] split = arr.substring(1, arr.length() - 1).trim()
				  .split("\\s*+]\\s*+\\[\\s*+");
				for (String s : split)
					list.add(new ListNode(Integer.parseInt(s)));
			}
		}
		this.list = list;
	}
	
	/**
	 * Create the necessary compounds and/or lists (if possible) so
	 * this path's parent exists.<br>
	 * You may also pass a second INBT value to be placed on this path
	 * @param root NBT where to create this path. Must be a compound
	 *             or list, depending on the first node of this path
	 * @return True if the path was successfully created and a future call
	 *         to {@code this.parent().}{@link NBTPath#exists(INBT)} with
	 *         the same root should succeed
	 */
	public boolean makePath(INBT root) { return makePath(root, null); }
	
	/**
	 * Create the necessary compounds and/or lists (if possible) so
	 * this path exists.<br>
	 * You may omit the value, in which case, only the path up to this
	 * parent will be made.
	 * @param root NBT where to create this path. Must be a compound
	 *             or list, depending on the first node of this path
	 * @param value INBT value to place at the made path
	 * @return True if the path was successfully created, and a future
	 *         call to {@link NBTPath#exists(INBT)} with the same root
	 *         should succeed.
	 */
	public boolean makePath(INBT root, @Nullable INBT value) {
		INBT copy = root.copy();
		INBT child = copy;
		Node node, nextNode = null;
		for (int i = 0, s = list.size() - 1; i < s; i++) {
			node = list.get(i);
			nextNode = list.get(i + 1);
			if (child instanceof CompoundNBT && node instanceof TagNode) {
				final String key = ((TagNode) node).name;
				if (!((CompoundNBT) child).contains(key))
					((CompoundNBT) child).put(key, nextNode.buildParent());
			} else if (child instanceof ListNBT && node instanceof ListNode) {
				final int index = ((ListNode) node).index;
				final int len = ((ListNBT) child).size();
				if (index > len || index < -len)
					return false;
				if (index == len) {
					try {
						((ListNBT) child).add(nextNode.buildParent());
					} catch (UnsupportedOperationException ignored) {
						return false;
					}
				}
			} else return false;
			child = node.apply(child);
			if (child == null)
				return false;
		}
		if (nextNode instanceof TagNode && !(child instanceof CompoundNBT)
		    || nextNode instanceof ListNode
		       && (!(child instanceof ListNBT)
		           || ((ListNBT) child).size() > Math.abs(((ListNode) nextNode).index)))
			return false;
		if (nextNode == null)
			nextNode = list.get(list.size() - 1);
		if (value != null) {
			if (nextNode.exists(child)) {
				//noinspection ConstantConditions
				if (!nextNode.apply(child).equals(value))
					return false;
			} else {
				if (nextNode instanceof TagNode) {
					((CompoundNBT) child).put(((TagNode) nextNode).name, value);
				} else if (nextNode instanceof ListNode) {
					int index = ((ListNode) nextNode).index;
					final int len = ((ListNBT) child).size();
					if (index == len) {
						try {
							((ListNBT) child).add(value);
						} catch (UnsupportedOperationException ignored) {
							return false;
						}
					}
					else return false;
				}
			}
		}
		Node first = list.get(0);
		if (root instanceof CompoundNBT && first instanceof TagNode) {
			final String key = ((TagNode) first).name;
			//noinspection ConstantConditions
			((CompoundNBT) root).put(key, ((CompoundNBT) copy).get(key));
		} else if (root instanceof ListNBT && first instanceof ListNode) {
			int index = ((ListNode) first).index;
			final int len = ((ListNBT) root).size();
			if (index < 0)
				index = len + index;
			if (index == len)
				((ListNBT) root).add(((ListNBT) copy).get(index));
			else ((ListNBT) root).set(index, ((ListNBT) copy).get(index));
		}
		return true;
	}
	
	public boolean exists(INBT root) {
		INBT child = root;
		for (Node node : list) {
			if (!node.exists(child))
				return false;
			child = node.apply(child);
			if (child == null)
				return false;
		}
		return true;
	}
	
	/**
	 * Create an NBT path from a list of nodes
	 */
	public NBTPath(List<Node> list) {
		this.list = new ArrayList<>(list);
	}
	
	/**
	 * Create a copy NBT path from another
	 */
	public NBTPath(NBTPath other) {
		this(other.list);
	}
	
	/**
	 * Test if this path contains another, that is, if the other's first nodes
	 * equal this' nodes
	 */
	public boolean contains(String other) {
		return contains(new NBTPath(other));
	}
	
	/**
	 * Test if this path contains another, that is, if the other's first nodes equal this' nodes
	 */
	public boolean contains(NBTPath other) {
		final int s = list.size();
		if (other.list.size() < s)
			return false;
		for (int i = 0; i < s; i++) {
			if (!list.get(i).equals(other.list.get(i)))
				return false;
		}
		return true;
	}
	
	/**
	 * Attempt to relativize another path
	 * @throws IllegalArgumentException if the path is not a child path,
	 * that is, if a call to {@link NBTPath#contains} would fail for the passed path
	 */
	public NBTPath relativize(String other) {
		return relativize(new NBTPath(other));
	}
	
	/**
	 * Attempt to relativize another path
	 * @throws IllegalArgumentException if the path is not a child path,
	 * that is, if a call to {@link NBTPath#contains} would fail for the passed path
	 */
	public NBTPath relativize(NBTPath other) {
		final int s = list.size();
		if (other.list.size() < s)
			throw new IllegalArgumentException("Cannot relativize a shorter path");
		if (!contains(other))
			throw new IllegalArgumentException("Relativized path is not a child path");
		return new NBTPath(other.list.subList(this.list.size(), other.list.size()));
	}
	
	public NBTPath resolve(String other) {
		return resolve(new NBTPath(other));
	}
	
	/**
	 * Resolve another path
	 */
	public NBTPath resolve(NBTPath other) {
		final List<Node> result = new ArrayList<>(list);
		result.addAll(other.list);
		return new NBTPath(result);
	}
	
	/**
	 * Unmodifiable view of the nodes of this path
	 */
	public List<Node> getNodes() {
		return Collections.unmodifiableList(list);
	}
	
	/**
	 * Test if this path is the empty or root path
	 */
	public boolean isRoot() {
		return list.isEmpty();
	}
	
	/**
	 * @return The parent path or null if this is already the root<br>
	 * @see NBTPath#isRoot()
	 */
	public @Nullable NBTPath parent() {
		return list.isEmpty() ? null : new NBTPath(
		  Util.make(new ArrayList<>(list), l -> l.remove(l.size() - 1)));
	}
	
	/**
	 * Apply the path to an NBT element<br>
	 * @return The NBT element pointed by this path, or {@code null} if not present
	 */
	public @Nullable INBT apply(INBT nbt) {
		if (nbt == null)
			return null;
		INBT child = nbt;
		for (Node node : list) {
			child = node.apply(child);
			if (child == null)
				return null;
		}
		return child;
	}
	
	/**
	 * Remove the nbt element at this path location from the passed nbt
	 * @return True if the element was found, and was successfully removed.
	 *         False if the element did not exist to begin with.
	 * @throws IllegalArgumentException if this is a root path
	 */
	public boolean delete(INBT nbt) {
		if (nbt == null)
			return false;
		if (list.isEmpty())
			throw new IllegalArgumentException("Cannot delete a root path");
		INBT child = nbt;
		final int last = list.size() - 1;
		for (Node node : list.subList(0, last)) {
			child = node.apply(child);
			if (child == null)
				return false;
		}
		return list.get(last).delete(child);
	}
	
	/**
	 * Abstract NBT path node<br>
	 * @see TagNode
	 * @see ListNode
	 */
	public static abstract class Node implements Comparable<Node> {
		/**
		 * @return The child pointed by this node from {@code nbt}
		 *         or null if not applicable.
		 */
		public abstract @Nullable INBT apply(INBT nbt);
		/**
		 * @return True if the child pointed by this node from {@code nbt} exists
		 */
		public abstract boolean exists(INBT nbt);
		/**
		 * Build the parent for this node, that is, an NBT
		 * that can contain this node.
		 */
		public abstract INBT buildParent();
		/**
		 * Delete the child pointed by this node at {@code nbt}
		 */
		public abstract boolean delete(INBT nbt);
		
		/**
		 * Pretty formatted text with {@link Style}
		 */
		public abstract IFormattableTextComponent getDisplay(Style style);
		/**
		 * Pretty formatted text with {@link NBTPath#defaultStyle}
		 */
		public IFormattableTextComponent getDisplay() {
			return getDisplay(defaultStyle);
		}
	}
	
	// Kinda useless, since the class should be immutable
	/**
	 * Copy this path object
	 */
	@Deprecated public NBTPath copy() {
		return new NBTPath(this);
	}
	
	/**
	 * Named node for NBT compounds.
	 */
	public static class TagNode extends Node {
		public final String name;
		public TagNode(String name) {
			this.name = Objects.requireNonNull(name);
		}
		
		@Override public @Nullable INBT apply(INBT nbt) {
			return (nbt instanceof CompoundNBT)? ((CompoundNBT) nbt).get(name) : null;
		}
		
		@Override public boolean exists(INBT nbt) {
			return nbt instanceof CompoundNBT && ((CompoundNBT) nbt).contains(name);
		}
		
		@Override public INBT buildParent() {
			return new CompoundNBT();
		}
		
		@Override public boolean delete(INBT nbt) {
			if (nbt instanceof CompoundNBT && ((CompoundNBT) nbt).contains(name)) {
				((CompoundNBT) nbt).remove(name);
				return true;
			}
			return false;
		}
		
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			TagNode tagNode = (TagNode) o;
			return name.equals(tagNode.name);
		}
		
		@Override public int hashCode() {
			return Objects.hash(name);
		}
		
		protected final Pattern simple = Pattern.compile("^\\w*+$");
		@Override public String toString() {
			return simple.matcher(name).matches()
			       ? name : "\"" + StringEscapeUtils.escapeJava(name) + "\"";
		}
		
		@Override public IFormattableTextComponent getDisplay(Style style) {
			return simple.matcher(name).matches()
			       ? stc(name).mergeStyle(style.nameStyle) :
			       stc("\"").append(
			         stc(StringEscapeUtils.escapeJava(name)).mergeStyle(style.nameStyle)
			       ).appendString("\"").mergeStyle(style.quoteStyle);
		}
		
		@Override public int compareTo(@NotNull NBTPath.Node o) {
			if (o instanceof ListNode) return 1;
			else if (o instanceof TagNode) return name.compareTo(((TagNode) o).name);
			else return -1;
		}
	}
	
	/**
	 * Indexed node for NBT lists
	 */
	public static class ListNode extends Node {
		public final int index;
		public ListNode(int index) {
			this.index = index;
		}
		
		@Override public @Nullable INBT apply(INBT nbt) {
			return (nbt instanceof CollectionNBT) ? apply((CollectionNBT<?>) nbt) : null;
		}
		
		@Override public INBT buildParent() {
			return new ListNBT();
		}
		
		@Override public boolean exists(INBT nbt) {
			if (!(nbt instanceof CollectionNBT))
				return false;
			final int s = ((CollectionNBT<?>) nbt).size();
			return index < s && index >= -s;
		}
		
		protected @Nullable INBT apply(CollectionNBT<?> list) {
			if (index < 0) {
				final int i = list.size() + index;
				if (i < 0)
					return null;
				return list.get(i);
			} else if (index >= list.size())
				return null;
			return list.get(index);
		}
		
		@Override public boolean delete(INBT nbt) {
			if (nbt instanceof CollectionNBT) {
				final int s = ((CollectionNBT<?>) nbt).size();
				int i = index < 0? s + index : index;
				if (i < s) {
					((CollectionNBT<?>) nbt).remove(i);
					return true;
				}
			}
			return false;
		}
		
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ListNode listNode = (ListNode) o;
			return index == listNode.index;
		}
		
		@Override public int hashCode() {
			return Objects.hash(index);
		}
		
		@Override public String toString() {
			return "[" + index + "]";
		}
		
		@Override public IFormattableTextComponent getDisplay(Style style) {
			return stc("[").append(stc(String.format("%d", index)).mergeStyle(style.indexStyle))
			  .appendString("]").mergeStyle(style.bracketStyle);
		}
		
		@Override public int compareTo(@NotNull NBTPath.Node o) {
			if (o instanceof TagNode) return -1;
			else if (o instanceof ListNode) return Integer.compare(index, ((ListNode) o).index);
			else return -1;
		}
	}
	
	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		NBTPath nbtPath = (NBTPath) o;
		return list.equals(nbtPath.list);
	}
	
	@Override public int hashCode() {
		return Objects.hash(list);
	}
	
	@Override public String toString() {
		Node last = null;
		StringBuilder str = new StringBuilder();
		for (Node node : list) {
			if (node instanceof TagNode && last != null)
				str.append(".");
			str.append(node.toString());
			last = node;
		}
		return str.toString();
	}
	
	/**
	 * Pretty formatted text with {@link Style}
	 */
	public IFormattableTextComponent getDisplay(Style style) {
		Node last = null;
		IFormattableTextComponent tc = stc("");
		for (Node node : list) {
			if (node instanceof TagNode && last != null)
				tc = tc.append(stc(".").mergeStyle(style.dotStyle));
			tc = tc.append(node.getDisplay(style));
			last = node;
		}
		return tc;
	}
	
	/**
	 * Pretty formatted text with {@link NBTPath#defaultStyle}
	 */
	public IFormattableTextComponent getDisplay() {
		return getDisplay(defaultStyle);
	}
	
	/**
	 * The number of nodes in this list.
	 */
	public int size() {
		return list.size();
	}
	
	@Override public int compareTo(@NotNull NBTPath o) {
		int size = min(size(), o.size());
		for (int i = 0; i < size; i++) {
			int c = list.get(i).compareTo(o.list.get(i));
			if (c != 0) return c;
		}
		return Integer.compare(size(), o.size());
	}
	
	@NotNull @Override public Iterator<Node> iterator() {
		return new NBTNodeIterator(this);
	}
	
	@Internal public static class NBTNodeIterator implements Iterator<Node> {
		protected Node[] nodes;
		protected int cursor = 0;
		
		public NBTNodeIterator(NBTPath path) {
			nodes = path.list.toArray(new Node[0]);
		}
		
		@Override public boolean hasNext() {
			return cursor < nodes.length;
		}
		
		@Override public Node next() {
			return nodes[cursor++];
		}
	}
}
