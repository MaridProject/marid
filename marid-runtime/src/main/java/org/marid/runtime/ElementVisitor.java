/*
 * MARID, the visual component programming environment.
 * Copyright (C) 2020 Dzmitry Auchynnikau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.marid.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

@FunctionalInterface
public interface ElementVisitor<E> {

  boolean visit(E element);

  static <E, C> C collect(Supplier<C> supplier, BiPredicate<C, E> accumulator, Consumer<ElementVisitor<E>> consumer) {
    var c = supplier.get();
    consumer.accept(e -> accumulator.test(c, e));
    return c;
  }

  static <E, C extends Collection<E>> C collect(Supplier<C> supplier, Consumer<ElementVisitor<E>> consumer) {
    return collect(supplier, (c, e) -> { c.add(e); return true; }, consumer);
  }

  static <E> List<E> toList(Consumer<ElementVisitor<E>> consumer) {
    var list = new ArrayList<E>();
    consumer.accept(list::add);
    return list;
  }

  static <E> Queue<E> toQueue(Consumer<ElementVisitor<E>> consumer) {
    var queue = new ConcurrentLinkedQueue<E>();
    consumer.accept(queue::offer);
    return queue;
  }
}
