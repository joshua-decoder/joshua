/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
package joshua.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 *
 * @author Lane Schwartz
 */
public class Lists {

//	public static void main(String[] args) {
//		
//		int[] list = {100, 200, 300, 400, 500};
//		
//		for (IndexedInt i : eachWithIndex(list)) {
//			
//			System.out.println(i.getIndex() + " " + i.getValue());
//			
//		}
//		
//		Integer[] list2 = new Integer[]{10, 20, 30, 40};
//		for (Indexed<Integer> i : eachWithIndex(list2)) {
//			
//			System.out.println(i.getIndex() + " " + i.getValue());
//			
//		}
//		
//		java.util.List<Integer> list3 = new java.util.ArrayList<Integer>();
//		for (int i : list2) { list3.add(i); }
//		
//		for (Indexed<Integer> i : eachWithIndex(list3)) {
//			
//			System.out.println(i.getIndex() + " " + i.getValue());
//			
//		}
//	}
	

	public static Iterable<Integer> upto(final int exclusiveUpperBound) {
		return new Iterable<Integer>() {
			public Iterator<Integer> iterator() {
				return new Iterator<Integer>() {
					int next = 0;
					
					public boolean hasNext() {
						return next < exclusiveUpperBound;
					}

					public Integer next() {
						int result = next;
						next += 1;
						return result;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
					
				};
			}
			
		};
	}
	
	public static Iterable<IndexedByte> eachWithIndex(final byte[] list) {
		
		return new Iterable<IndexedByte>() {
			
			public Iterator<IndexedByte> iterator() {
				return new Iterator<IndexedByte>() {

					int nextIndex = -1;
					IndexedByte indexedValue;
					
					public boolean hasNext() {
						return (nextIndex < list.length);
					}

					public IndexedByte next() {
						if (nextIndex >= list.length) {
							throw new NoSuchElementException();
						} else if (nextIndex < 0) {
							nextIndex = 0;
							indexedValue = new IndexedByte(list[nextIndex],nextIndex);
						} else {
							indexedValue.value = list[nextIndex];
							indexedValue.index = nextIndex;
						}
						
						nextIndex += 1;
						return indexedValue;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
					
				};
			}
			
		};
	}
	
	public static Iterable<IndexedShort> eachWithIndex(final short[] list) {
		
		return new Iterable<IndexedShort>() {
			
			public Iterator<IndexedShort> iterator() {
				return new Iterator<IndexedShort>() {

					int nextIndex = -1;
					IndexedShort indexedValue;
					
					public boolean hasNext() {
						return (nextIndex < list.length);
					}

					public IndexedShort next() {
						if (nextIndex >= list.length) {
							throw new NoSuchElementException();
						} else if (nextIndex < 0) {
							nextIndex = 0;
							indexedValue = new IndexedShort(list[nextIndex],nextIndex);
						} else {
							indexedValue.value = list[nextIndex];
							indexedValue.index = nextIndex;
						}
						
						nextIndex += 1;
						return indexedValue;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
					
				};
			}
			
		};
	}
	
	public static Iterable<IndexedInt> eachWithIndex(final int[] list) {
		
		return new Iterable<IndexedInt>() {
			
			public Iterator<IndexedInt> iterator() {
				return new Iterator<IndexedInt>() {

					int nextIndex = -1;
					IndexedInt indexedValue;
					
					public boolean hasNext() {
						return (nextIndex < list.length);
					}

					public IndexedInt next() {
						if (nextIndex >= list.length) {
							throw new NoSuchElementException();
						} else if (nextIndex < 0) {
							nextIndex = 0;
							indexedValue = new IndexedInt(list[nextIndex],nextIndex);
						} else {
							indexedValue.value = list[nextIndex];
							indexedValue.index = nextIndex;
						}
						
						nextIndex += 1;
						return indexedValue;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
					
				};
			}
			
		};
	}
	
	public static Iterable<IndexedLong> eachWithIndex(final long[] list) {
		
		return new Iterable<IndexedLong>() {
			
			public Iterator<IndexedLong> iterator() {
				return new Iterator<IndexedLong>() {

					int nextIndex = -1;
					IndexedLong indexedValue;
					
					public boolean hasNext() {
						return (nextIndex < list.length);
					}

					public IndexedLong next() {
						if (nextIndex >= list.length) {
							throw new NoSuchElementException();
						} else if (nextIndex < 0) {
							nextIndex = 0;
							indexedValue = new IndexedLong(list[nextIndex],nextIndex);
						} else {
							indexedValue.value = list[nextIndex];
							indexedValue.index = nextIndex;
						}
						
						nextIndex += 1;
						return indexedValue;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
					
				};
			}
			
		};
	}

	public static Iterable<IndexedFloat> eachWithIndex(final float[] list) {
		
		return new Iterable<IndexedFloat>() {
			
			public Iterator<IndexedFloat> iterator() {
				return new Iterator<IndexedFloat>() {

					int nextIndex = -1;
					IndexedFloat indexedValue;
					
					public boolean hasNext() {
						return (nextIndex < list.length);
					}

					public IndexedFloat next() {
						if (nextIndex >= list.length) {
							throw new NoSuchElementException();
						} else if (nextIndex < 0) {
							nextIndex = 0;
							indexedValue = new IndexedFloat(list[nextIndex],nextIndex);
						} else {
							indexedValue.value = list[nextIndex];
							indexedValue.index = nextIndex;
						}
						
						nextIndex += 1;
						return indexedValue;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
					
				};
			}
			
		};
	}
	
	public static Iterable<IndexedDouble> eachWithIndex(final double[] list) {
		
		return new Iterable<IndexedDouble>() {
			
			public Iterator<IndexedDouble> iterator() {
				return new Iterator<IndexedDouble>() {

					int nextIndex = -1;
					IndexedDouble indexedValue;
					
					public boolean hasNext() {
						return (nextIndex < list.length);
					}

					public IndexedDouble next() {
						if (nextIndex >= list.length) {
							throw new NoSuchElementException();
						} else if (nextIndex < 0) {
							nextIndex = 0;
							indexedValue = new IndexedDouble(list[nextIndex],nextIndex);
						} else {
							indexedValue.value = list[nextIndex];
							indexedValue.index = nextIndex;
						}
						
						nextIndex += 1;
						return indexedValue;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
					
				};
			}
			
		};
	}
	
	public static <V> Iterable<Indexed<V>> eachWithIndex(final V[] list) {
		return new Iterable<Indexed<V>>() {
			
			public Iterator<Indexed<V>> iterator() {
				return new Iterator<Indexed<V>>() {

					int nextIndex = -1;
					Indexed<V> indexedValue;
					
					public boolean hasNext() {
						return (nextIndex < list.length);
					}

					public Indexed<V> next() {
						if (nextIndex >= list.length) {
							throw new NoSuchElementException();
						} else if (nextIndex < 0) {
							nextIndex = 0;
							indexedValue = new Indexed<V>(list[nextIndex],nextIndex);
						} else {
							indexedValue.value = list[nextIndex];
							indexedValue.index = nextIndex;
						}
						
						nextIndex += 1;
						return indexedValue;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
					
				};
			}
			
		};
	}
	
	public static <V> Iterable<Indexed<V>> eachWithIndex(final Iterator<V> valueIterator) {
		return new Iterable<Indexed<V>>() {
			
			public Iterator<Indexed<V>> iterator() {
				return new Iterator<Indexed<V>>() {

					int nextIndex = -1;
					Indexed<V> indexedValue;
					
					public boolean hasNext() {
						return valueIterator.hasNext();
					}

					public Indexed<V> next() {
						if (! valueIterator.hasNext()) {
							throw new NoSuchElementException();
						} else if (nextIndex < 0) {
							nextIndex = 0;
							indexedValue = new Indexed<V>(valueIterator.next(),nextIndex);
						} else {
							indexedValue.value = valueIterator.next();
							indexedValue.index = nextIndex;
						}
						
						nextIndex += 1;
						return indexedValue;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
					
				};
			}
			
		};
	}
	
	public static <V> Iterable<Indexed<V>> eachWithIndex(final Iterable<V> iterable) {
		return eachWithIndex(iterable.iterator());
	}
	

	public static class Index {
		
		int index;
		
		Index(int index) {
			this.index = index;
		}
		
		public int getIndex() {
			return this.index;
		}
		
		void setIndex(int index) {
			this.index = index;
		}
	}

	public static class IndexedBoolean extends Index {
		
		boolean value;
		
		IndexedBoolean(boolean value, int index) {
			super(index);
			this.value = value;
		}
		
		public boolean getValue() {
			return this.value;
		}
		
		void setValue(boolean value) {
			this.value = value;
			this.index += 1;
		}
	}

	public static class IndexedByte extends Index {
		
		byte value;
		
		IndexedByte(byte value, int index) {
			super(index);
			this.value = value;
		}
		
		public byte getValue() {
			return this.value;
		}
		
		void setValue(byte value) {
			this.value = value;
			this.index += 1;
		}
	}

	public static class IndexedShort extends Index {
		
		short value;
		
		IndexedShort(short value, int index) {
			super(index);
			this.value = value;
		}
		
		public short getValue() {
			return this.value;
		}
		
		void setValue(short value) {
			this.value = value;
			this.index += 1;
		}
	}

	public static class IndexedInt extends Index {
		
		int value;
		
		IndexedInt(int value, int index) {
			super(index);
			this.value = value;
		}
		
		public int getValue() {
			return this.value;
		}
		
		void setValue(int value) {
			this.value = value;
			this.index += 1;
		}
	}

	public static class IndexedLong extends Index {
		
		long value;
		
		IndexedLong(long value, int index) {
			super(index);
			this.value = value;
		}
		
		public long getValue() {
			return this.value;
		}
		
		void setValue(long value) {
			this.value = value;
			this.index += 1;
		}
	}

	public static class IndexedFloat extends Index {
		
		float value;
		
		IndexedFloat(float value, int index) {
			super(index);
			this.value = value;
		}
		
		public float getValue() {
			return this.value;
		}
		
		void setValue(float value) {
			this.value = value;
			this.index += 1;
		}
	}

	public static class IndexedDouble extends Index {
		
		double value;
		
		IndexedDouble(double value, int index) {
			super(index);
			this.value = value;
		}
		
		public double getValue() {
			return this.value;
		}
		
		void setValue(double value) {
			this.value = value;
			this.index += 1;
		}
	}


	public static class Indexed<V> extends Index {
		
		V value;
		
		Indexed(V value, int index) {
			super(index);
			this.value = value;
		}
		
		public V getValue() {
			return this.value;
		}
	}
}
