package org.aion.avm.userlib;

import java.util.*;


/**
 * A simple List implementation.
 *
 * <p>This implementation is backed by a single Object[], starts at 5 elements and doubles when full.
 *
 * @param <E> The type of elements within the set.
 */
public class AionList<E> implements List<E> {
    private static final int DEFAULT_CAPACITY = 5;

    private Object[] storage;

    private int size;

    private int modCount;

    public AionList() {
        this.storage = new Object[DEFAULT_CAPACITY];
        this.size = 0;
        this.modCount = 0;
    }

    public void trimToSize() {
        modCount++;
        if (size < storage.length) {
            Object[] tmp = this.storage;
            this.storage = new Object[size];
            System.arraycopy(tmp, 0, this.storage, 0, size);
        }
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean contains(Object toFind) {
        return indexOf(toFind) >= 0;
    }

    @Override
    public boolean isEmpty() {
        return 0 == size;
    }

    @Override
    public Object[] toArray() {
        Object[] ret = new Object[size];
        System.arraycopy(this.storage, 0, ret, 0, size);
        return ret;
    }

    //
    @Override
    public <T> T[] toArray(T[] a) {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public E get(int index) {
        checkIndex(index);
        return (E) this.storage[index];
    }

    @SuppressWarnings("unchecked")
    @Override
    public E set(int index, E element) {
        checkIndex(index);
        E oldData = (E) this.storage[index];
        this.storage[index] = element;
        return oldData;
    }

    @Override
    public boolean add(E newElement) {
        if (this.size == this.storage.length) {
            this.storage = grow();
        }
        this.storage[size] = newElement;
        size = size + 1;
        modCount++;
        return true;
    }

    @Override
    public void add(int index, E element) {
        rangeCheckForAdd(index);
        if (this.size == this.storage.length) {
            this.storage = grow();
        }
        System.arraycopy(this.storage, index, this.storage, index + 1, size - index);
        this.storage[index] = element;
        size = size + 1;
        modCount++;
    }

    private void rangeCheckForAdd(int index) {
        if (index > size || index < 0)
            throw new IndexOutOfBoundsException();
    }

    private void checkIndex(int index) {
        if (index >= size || index < 0)
            throw new IndexOutOfBoundsException();
    }

    private Object[] grow() {
        Object[] newStorage = new Object[2 * this.storage.length];
        System.arraycopy(this.storage, 0, newStorage, 0, this.storage.length);
        return newStorage;
    }


    @SuppressWarnings("unchecked")
    @Override
    public E remove(int index) {
        checkIndex(index);

        E oldValue = (E) this.storage[index];
        if ((size - 1) > index)
            System.arraycopy(this.storage, index + 1, this.storage, index, size - 1 - index);
        this.storage[size - 1] = null;
        // todo move up (20)
        size = size - 1;
        modCount++;

        return oldValue;
    }

    @Override
    public boolean remove(Object toRemove) {
        boolean ret = false;
        int index = indexOf(toRemove);
        if (index >= 0) {
            this.remove(index);
            ret = true;
        }
        return ret;
    }

    @Override
    public void clear() {
        this.storage = new Object[DEFAULT_CAPACITY];
        this.size = 0;
        modCount++;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean ret = false;
        for (E obj : c) {
            this.add(obj);
            ret = true;
        }
        return ret;
    }

    // todo either for loop, array copy or once and copy
    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        rangeCheckForAdd(index);
        boolean ret = false;
        for (E obj : c) {
            this.add(index++, obj);
            ret = true;
        }
        return ret;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object obj : c) {
            if (!this.contains(obj)) return false;
        }
        return true;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean ret = false;
        Iterator<E> it = this.iterator();
        while (it.hasNext()) {
            if (!c.contains(it.next())) {
                it.remove();
                ret = true;
            }
        }
        return ret;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean ret = false;
        Iterator<E> it = this.iterator();
        while (it.hasNext()) {
            if (c.contains(it.next())) {
                it.remove();
                ret = true;
            }
        }
        return ret;
    }

    @Override
    public int indexOf(Object o) {
        if (o == null) {
            for (int i = 0; i < size; i++)
                if (storage[i] == null)
                    return i;
        } else {
            for (int i = 0; i < size; i++)
                if (o.equals(storage[i]))
                    return i;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        if (o == null) {
            for (int i = size - 1; i >= 0; i--)
                if (storage[i] == null)
                    return i;
        } else {
            for (int i = size - 1; i >= 0; i--)
                if (o.equals(storage[i]))
                    return i;
        }
        return -1;
    }

    @Override
    public ListIterator<E> listIterator() {
        return new AionListIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        rangeCheckForAdd(index);
        return new AionListIterator(index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        subListRangeCheck(fromIndex, toIndex, size);

        return new AionSubList<>(this, fromIndex, toIndex);
    }

    // todo 60 if inline
    private static void subListRangeCheck(int fromIndex, int toIndex, int size) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        }
        if (toIndex > size) {
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        }
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new AionListIterator(0);
    }


    /**
     * Note that this is a very simple implementation so it skips all optional operations and doesn't detect concurrent modifications.
     * (public only to reference it from tests when building the DApp jar)
     */
    public class AionListIterator implements ListIterator<E> {
        private int lastReturnedIndex = -1;
        private int nextIndex;
        public AionListIterator(int nextIndex) {
            this.nextIndex = nextIndex;
        }
        @Override
        public boolean hasNext() {
            return this.nextIndex < AionList.this.size;
        }
        @SuppressWarnings("unchecked")
        @Override
        public E next() {
            E elt = null;
            if (hasNext()) {
                elt = (E) AionList.this.storage[this.nextIndex];
                lastReturnedIndex = this.nextIndex;
                this.nextIndex += 1;
            } else {
                throw new NoSuchElementException();
            }
            return elt;
        }
        @Override
        public boolean hasPrevious() {
            return this.nextIndex > 0;
        }
        @SuppressWarnings("unchecked")
        @Override
        public E previous() {
            E elt = null;
            if (hasPrevious()) {
                this.nextIndex -= 1;
                elt = (E) AionList.this.storage[this.nextIndex];
                lastReturnedIndex = this.nextIndex;
            } else {
                throw new NoSuchElementException();
            }
            return elt;
        }
        @Override
        public int nextIndex() {
            return this.nextIndex;
        }
        @Override
        public int previousIndex() {
            return this.nextIndex - 1;
        }
        @Override
        public void remove() {
            if (this.lastReturnedIndex < 0) {
                throw new IllegalStateException();
            } else {
                AionList.this.remove(this.lastReturnedIndex);
                this.nextIndex = this.lastReturnedIndex;
                this.lastReturnedIndex = -1;
            }
        }
        @Override
        public void set(E e) {
            if (this.lastReturnedIndex < 0) {
                throw new IllegalStateException();
            }
            AionList.this.set(this.lastReturnedIndex, e);
        }

        @Override
        public void add(E e) {
            AionList.this.add(this.nextIndex, e);
            this.lastReturnedIndex = -1;
            this.nextIndex += 1;
        }
    }

    private static class AionSubList<E> implements List<E> {

        private final AionList<E> root;
        private final AionSubList<E> parent;
        private final int offset;
        private int size;
        private int modCount;

        /**
         * Constructs a sublist from given root AionList
         */
        public AionSubList(AionList<E> root, int fromIndex, int toIndex) {
            this.root = root;
            this.parent = null;
            this.offset = fromIndex;
            this.size = toIndex - fromIndex;
            this.modCount = root.modCount;
        }

        /**
         * Constructs a sublist of another SubList.
         */
        private AionSubList(AionSubList<E> parent, int fromIndex, int toIndex) {
            this.root = parent.root;
            this.parent = parent;
            this.offset = parent.offset + fromIndex;
            this.size = toIndex - fromIndex;
            this.modCount = root.modCount;
        }

        @Override
        public int size() {
            return this.size;
        }

        @Override
        public boolean isEmpty() {
            return 0 == this.size;
        }

        @Override
        public boolean contains(Object toFind) {
            return indexOf(toFind) >= 0;
        }

        @Override
        public Iterator<E> iterator() {
            return listIterator(0);
        }

        @Override
        public Object[] toArray() {
            checkForComodification();
            Object[] ret = new Object[this.size];
            System.arraycopy(root.storage, offset, ret, 0, this.size);
            return ret;
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return null;
        }

        @Override
        public boolean add(E newElement) {
            checkForComodification();
            root.add(offset + this.size, newElement);
            updateSizeAndModCount(1);
            return true;
        }

        @Override
        public boolean remove(Object element) {
            checkForComodification();
            boolean ret = false;
            int indexToRemove = indexOf(element);
            if (indexToRemove > 0) {
                root.remove(indexToRemove + offset);
                updateSizeAndModCount(-1);
                ret = true;
            }
            return ret;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            for (Object obj : c) {
                if (!this.contains(obj)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            return addAll(this.size, c);
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> c) {
            rangeCheckForAdd(index);
            checkForComodification();
            this.root.addAll(index + offset, c);
            updateSizeAndModCount(c.size());
            return true;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            boolean ret = false;
            for(Object obj : c){
                boolean removed = remove(obj);
                if(removed){
                    ret = true;
                }
            }
            return ret;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            checkForComodification();
            Iterator<E> it = this.listIterator(0);
            boolean ret = false;

            while (it.hasNext()) {
                if (!c.contains(it.next())) {
                    it.remove();
                    ret = true;
                }
            }
            return ret;
        }

        @Override
        public void clear() {
            checkForComodification();
            for (int i = offset + size - 1; i >= offset; i--) {
                root.remove(i);
            }
            updateSizeAndModCount(-size);
        }

        @Override
        public E get(int index) {
            checkForComodification();
            return root.get(index + offset);
        }

        @Override
        public E set(int index, E element) {
            checkForComodification();
            return root.set(offset + index, element);
        }

        @Override
        public void add(int index, E element) {
            rangeCheckForAdd(index);
            checkForComodification();
            root.add(index + offset, element);
            updateSizeAndModCount(1);
        }

        @Override
        public E remove(int index) {
            rangeCheckForRemove(index);
            checkForComodification();
            E result = root.remove(index + offset);
            updateSizeAndModCount(-1);
            return result;
        }

        @Override
        public int indexOf(Object o) {
            int end = this.size + this.offset;
            if (o == null) {
                for (int i = this.offset; i < end; i++) {
                    if (root.storage[i] == null) {
                        return i - this.offset;
                    }
                }
            } else {
                for (int i = this.offset; i < end; i++) {
                    if (o.equals(root.storage[i])) {
                        return i - this.offset;
                    }
                }
            }
            return -1;
        }

        @Override
        public int lastIndexOf(Object o) {
            int start = this.size + this.offset - 1;
            if (o == null) {
                for (int i = start; i >= this.offset; i--) {
                    if (root.storage[i] == null) {
                        return i - this.offset;
                    }
                }
            } else {
                for (int i = start; i >= this.offset; i--) {
                    if (o.equals(root.storage[i])) {
                        return i - this.offset;
                    }
                }
            }
            return -1;
        }

        @Override
        public ListIterator<E> listIterator() {
            return listIterator(0);
        }

        @Override
        public ListIterator<E> listIterator(int index) {
            return new ListIterator<E>() {
                private ListIterator<E> itr = root.listIterator(offset + index);

                public boolean hasNext() {
                    return nextIndex() < AionSubList.this.size;
                }

                public E next() {
                    if (hasNext()) {
                        return itr.next();
                    } else {
                        throw new NoSuchElementException();
                    }
                }

                public boolean hasPrevious() {
                    return previousIndex() >= 0;
                }

                public E previous() {
                    if (hasPrevious()) {
                        return itr.previous();
                    } else {
                        throw new NoSuchElementException();
                    }
                }

                public int nextIndex() {
                    return itr.nextIndex() - offset;
                }

                public int previousIndex() {
                    return itr.previousIndex() - offset;
                }

                public void remove() {
                    itr.remove();
                    updateSizeAndModCount(-1);
                }

                public void set(E e) {
                    itr.set(e);
                }

                public void add(E e) {
                    itr.add(e);
                    updateSizeAndModCount(1);
                }
            };
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            return new AionSubList<>(this, fromIndex, toIndex);
        }

        /**
         * Helpers
         */

        private void checkForComodification() {
            if (root.modCount != modCount) {
                throw new RuntimeException();
            }
        }

        private void updateSizeAndModCount(int sizeChange) {
            AionSubList<E> slist = this;
            do {
                slist.size += sizeChange;
                slist.modCount = root.modCount;
                slist = slist.parent;
            } while (slist != null);
        }

        private void rangeCheckForAdd(int index) {
            if (index > size || index < 0) {
                throw new IndexOutOfBoundsException();
            }
        }

        private void rangeCheckForRemove(int index) {
            if (index >= size || index < 0) {
                throw new IndexOutOfBoundsException();
            }
        }


//        public class AionSubListIterator implements ListIterator<E> {
//            private int lastReturnedIndex = -1;
//            private int nextIndex;
//            public AionSubListIterator(int nextIndex) {
//                this.nextIndex = nextIndex;
//            }
//            @Override
//            public boolean hasNext() {
//                return this.nextIndex < AionSubList.this.size;
//            }
//            @SuppressWarnings("unchecked")
//            @Override
//            public E next() {
//                E elt = null;
//                if (this.nextIndex < AionSubList.this.size) {
//                    elt = (E) AionSubList.this.root.storage[this.nextIndex];
//                    this.nextIndex += 1;
//                    lastReturnedIndex = this.nextIndex - 1;
//                } else {
//                    throw new NoSuchElementException();
//                }
//                return elt;
//            }
//            @Override
//            public boolean hasPrevious() {
//                return this.nextIndex > 0;
//            }
//            @SuppressWarnings("unchecked")
//            @Override
//            public E previous() {
//                E elt = null;
//                if (this.nextIndex > 0) {
//                    this.nextIndex -= 1;
//                    elt = (E) AionSubList.this.root.storage[this.nextIndex];
//                    lastReturnedIndex = this.nextIndex;
//                } else {
//                    throw new NoSuchElementException();
//                }
//                return elt;
//            }
//            @Override
//            public int nextIndex() {
//                return this.nextIndex;
//            }
//            @Override
//            public int previousIndex() {
//                return this.nextIndex - 1;
//            }
//            @Override
//            public void remove() {
//                if (this.lastReturnedIndex < 0) {
//                    throw new IllegalStateException();
//                } else {
//                    AionSubList.this.remove(this.lastReturnedIndex);
//                    this.nextIndex = this.lastReturnedIndex;
//                    this.lastReturnedIndex = -1;
//                }
//            }
//            @Override
//            public void set(E e) {
//                throw new UnsupportedOperationException();
//            }
//            @Override
//            public void add(E e) {
//                throw new UnsupportedOperationException();
//            }
//        }
    }
}
