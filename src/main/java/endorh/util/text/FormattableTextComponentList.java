package endorh.util.text;

import net.minecraft.util.text.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static endorh.util.text.TextUtil.stc;

/**
 * Formattable list of {@link IFormattableTextComponent}.
 */
public class FormattableTextComponentList implements List<IFormattableTextComponent> {
	protected List<IFormattableTextComponent> backend;
	
	public FormattableTextComponentList() {
		this.backend = new ArrayList<>();
	}
	
	public FormattableTextComponentList(IFormattableTextComponent... items) {
		this.backend = Arrays.stream(items).collect(Collectors.toList());
	}
	
	public FormattableTextComponentList(List<IFormattableTextComponent> items) {
		this.backend = new ArrayList<>(items);
	}
	
	protected FormattableTextComponentList(
	  FormattableTextComponentList parent, int fromIndex, int toIndex
	) {
		this.backend = parent.backend.subList(fromIndex, toIndex);
	}
	
	public FormattableTextComponentList setStyle(Style style) {
		backend.forEach(s -> s.setStyle(style));
		return this;
	}
	
	public FormattableTextComponentList modifyStyle(UnaryOperator<Style> modifyFunc) {
		backend.forEach(s -> modifyFunc.apply(s.getStyle()));
		return this;
	}
	
	public FormattableTextComponentList withStyle(Style style) {
		backend.forEach(s -> s.setStyle(style.mergeStyle(s.getStyle())));
		return this;
	}
	
	public FormattableTextComponentList withStyle(TextFormatting... formats) {
		backend.forEach(s -> s.setStyle(s.getStyle().createStyleFromFormattings(formats)));
		return this;
	}
	
	public FormattableTextComponentList withStyle(TextFormatting format) {
		backend.forEach(s -> s.setStyle(s.getStyle().applyFormatting(format)));
		return this;
	}
	
	public IFormattableTextComponent join(String separator) {
		return join(stc(separator));
	}
	
	public IFormattableTextComponent join(ITextComponent separator) {
		return backend.stream().reduce(stc(""), (a, b) -> a.append(separator).append(b));
	}
	
	// Deferred methods
	
	@Override public int size() {
		return backend.size();
	}
	
	@Override public boolean isEmpty() {
		return backend.isEmpty();
	}
	
	@Override public boolean contains(Object o) {
		return backend.contains(o);
	}
	
	@NotNull @Override public Iterator<IFormattableTextComponent> iterator() {
		return backend.iterator();
	}
	
	@NotNull @Override public Object @NotNull [] toArray() {
		return backend.toArray();
	}
	
	@SuppressWarnings("SuspiciousToArrayCall")
	@NotNull @Override public <T> T @NotNull [] toArray(@NotNull T @NotNull [] a) {
		return backend.toArray(a);
	}
	
	@Override public boolean add(IFormattableTextComponent elem) {
		return backend.add(elem);
	}
	
	@Override public boolean remove(Object o) {
		return backend.remove(o);
	}
	
	@SuppressWarnings("SlowListContainsAll")
	@Override public boolean containsAll(@NotNull Collection<?> c) {
		return backend.containsAll(c);
	}
	
	@Override public boolean addAll(@NotNull Collection<? extends IFormattableTextComponent> c) {
		return backend.addAll(c);
	}
	
	@Override public boolean addAll(int index, @NotNull Collection<? extends IFormattableTextComponent> c) {
		return backend.addAll(index, c);
	}
	
	@Override public boolean removeAll(@NotNull Collection<?> c) {
		return backend.removeAll(c);
	}
	
	@Override public boolean retainAll(@NotNull Collection<?> c) {
		return backend.retainAll(c);
	}
	
	@Override public void clear() {
		backend.clear();
	}
	
	@Override public IFormattableTextComponent get(int index) {
		return backend.get(index);
	}
	
	@Override public IFormattableTextComponent set(int index, IFormattableTextComponent element) {
		return backend.set(index, element);
	}
	
	@Override public void add(int index, IFormattableTextComponent element) {
		backend.add(index, element);
	}
	
	@Override public IFormattableTextComponent remove(int index) {
		return backend.remove(index);
	}
	
	@Override public int indexOf(Object o) {
		return backend.indexOf(o);
	}
	
	@Override public int lastIndexOf(Object o) {
		return backend.lastIndexOf(o);
	}
	
	@NotNull @Override public ListIterator<IFormattableTextComponent> listIterator() {
		return backend.listIterator();
	}
	
	@NotNull @Override public ListIterator<IFormattableTextComponent> listIterator(int index) {
		return backend.listIterator(index);
	}
	
	@NotNull @Override public FormattableTextComponentList subList(int fromIndex, int toIndex) {
		return new FormattableTextComponentList(this, fromIndex, toIndex);
	}
}
